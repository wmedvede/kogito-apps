/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.jobs.service.scheduler;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.kie.kogito.jobs.JobsServiceException;
import org.kie.kogito.jobs.service.management.MessagingChangeEvent;
import org.kie.kogito.jobs.service.model.JobDetails;
import org.kie.kogito.jobs.service.model.JobStatus;
import org.kie.kogito.jobs.service.repository.ReactiveJobRepository;
import org.kie.kogito.jobs.service.scheduler.impl.TimerDelegateJobScheduler;
import org.kie.kogito.jobs.service.utils.DateUtil;
import org.kie.kogito.jobs.service.utils.ErrorHandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.mutiny.core.Vertx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import static org.kie.kogito.jobs.service.repository.ReactiveJobRepository.SortTermField.CREATED;
import static org.kie.kogito.jobs.service.repository.ReactiveJobRepository.SortTermField.FIRE_TIME;
import static org.kie.kogito.jobs.service.repository.ReactiveJobRepository.SortTermField.ID;

@ApplicationScoped
public class JobSchedulerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerManager.class);

    private static final Integer SCHEDULER_PAGE_SIZE = 2;

    /**
     * The current chunk size in minutes the scheduler handles, it is used to keep a limit number of jobs scheduled
     * in the in-memory scheduler.
     */
    @ConfigProperty(name = "kogito.jobs-service.schedulerChunkInMinutes", defaultValue = "10")
    long schedulerChunkInMinutes;

    @ConfigProperty(name = "kogito.jobs-service.pageSize", defaultValue = "2")
    int schedulerPageSize;

    /**
     * The interval the job loading method runs to fetch the persisted jobs from the repository.
     */
    @ConfigProperty(name = "kogito.jobs-service.loadJobIntervalInMinutes", defaultValue = "2")
    long loadJobIntervalInMinutes;

    /**
     * The interval based on the current time the job loading method uses to fetch jobs "FROM (now -
     * {@link #loadJobFromCurrentTimeIntervalInMinutes}) TO {@link #schedulerChunkInMinutes}"
     */
    @ConfigProperty(name = "kogito.jobs-service.loadJobFromCurrentTimeIntervalInMinutes", defaultValue = "0")
    long loadJobFromCurrentTimeIntervalInMinutes;

    @Inject
    TimerDelegateJobScheduler scheduler;

    @Inject
    ReactiveJobRepository repository;

    @Inject
    Vertx vertx;
    final AtomicBoolean enabled = new AtomicBoolean(false);

    final AtomicLong periodicTimerIdForLoadJobs = new AtomicLong(-1L);

    final AtomicBoolean initialLoading = new AtomicBoolean(true);

    static final ZonedDateTime INITIAL_DATE = ZonedDateTime.parse("2000-01-01T00:00:00.0+00");

    @ConfigProperty(name = "test.InitialMaxFireTime")
    Optional<String> testInitialMaxFireTime;

    private void startJobsLoadingFromRepositoryTask() {
        //guarantee it starts the task just in case it is not already active
        if (periodicTimerIdForLoadJobs.get() < 0) {
            if (loadJobIntervalInMinutes > schedulerChunkInMinutes) {
                LOGGER.warn("The loadJobIntervalInMinutes ({}) cannot be greater than schedulerChunkInMinutes ({}), " +
                        "setting value {} for both",
                        loadJobIntervalInMinutes,
                        schedulerChunkInMinutes,
                        schedulerChunkInMinutes);
                loadJobIntervalInMinutes = schedulerChunkInMinutes;
            }
            //first execution
            vertx.runOnContext(this::loadJobDetails);
            //next executions to run periodically
            periodicTimerIdForLoadJobs.set(vertx.setPeriodic(TimeUnit.MINUTES.toMillis(loadJobIntervalInMinutes), id -> loadJobDetails()));
        }
    }

    private void cancelJobsLoadingFromRepositoryTask() {
        if (periodicTimerIdForLoadJobs.get() > 0) {
            vertx.cancelTimer(periodicTimerIdForLoadJobs.get());
            //negative id indicates this is not active anymore
            periodicTimerIdForLoadJobs.set(-1);
        }
    }

    protected synchronized void onMessagingStatusChange(@Observes MessagingChangeEvent event) {
        boolean wasEnabled = enabled.getAndSet(event.isEnabled());
        if (enabled.get() && !wasEnabled) {
            // good, avoid starting twice if we receive two consecutive enabled = true
            startJobsLoadingFromRepositoryTask();
        } else if (!enabled.get()) {
            // but only cancel if we receive enabled = false, otherwise with two consecutive enable we are also cancelling.
            cancelJobsLoadingFromRepositoryTask();
        }
    }

    //Runs periodically loading the jobs from the repository in chunks
    void loadJobDetails() {
        if (!enabled.get()) {
            LOGGER.info("Skip loading scheduled jobs");
            return;
        }
        ZonedDateTime from = DateUtil.now().minusMinutes(loadJobFromCurrentTimeIntervalInMinutes);
        ZonedDateTime to = DateUtil.now().plusMinutes(schedulerChunkInMinutes);
        if (initialLoading.get()) {
            from = INITIAL_DATE;
        }
        doLoadJobDetailsByCreated(from, to, INITIAL_DATE, 0, schedulerPageSize);
    }

    public void doLoadJobDetails(ZonedDateTime fromFireTime, ZonedDateTime toFireTime, int offset, int pageSize) {
        final AtomicReference<ZonedDateTime> nextFromFireTime = new AtomicReference<>(fromFireTime);
        final AtomicReference<ZonedDateTime> currentFireTime = new AtomicReference<>();
        final AtomicInteger queryResultSize = new AtomicInteger();
        final AtomicInteger atomicOffset = new AtomicInteger(offset);
        final AtomicInteger retries = new AtomicInteger(4);

        LOGGER.info("doLoadJobDetails, from: {}, to: {}, offset: {}, pageSize: {}", fromFireTime.toOffsetDateTime(), toFireTime.toOffsetDateTime(), offset, pageSize);
        loadJobsBetweenDates(nextFromFireTime.get(), toFireTime, atomicOffset.get(), pageSize)
                .map(jobDetails -> {
                    LOGGER.info("doLoadJobDetails, job found, id: {}, nextFireTime: {}, created: {} ", jobDetails.getId(),
                            DateUtil.instantToZonedDateTime(jobDetails.getTrigger().hasNextFireTime().toInstant()).toOffsetDateTime(),
                            jobDetails.getCreated());
                    //TODO, currentFireTime can be null?
                    currentFireTime.set(DateUtil.instantToZonedDateTime(jobDetails.getTrigger().hasNextFireTime().toInstant()));
                    if (currentFireTime.get().toInstant().equals(nextFromFireTime.get().toInstant())) {
                        atomicOffset.incrementAndGet();
                    } else {
                        nextFromFireTime.set(currentFireTime.get());
                        atomicOffset.set(1);
                    }
                    queryResultSize.incrementAndGet();
                    return jobDetails;
                })
                .filter(jobDetails -> scheduler.scheduled(jobDetails.getId()).isEmpty()) //not consider already scheduled jobs
                .flatMapRsPublisher(jobDetails -> ErrorHandling.skipErrorPublisher(scheduler::schedule, jobDetails))
                .forEach(jobDetails -> LOGGER.debug("Loaded and scheduled job {}", jobDetails))
                .run()
                .whenComplete((unused, throwable) -> {
                    LOGGER.info("doLoadJobDetails, queryResultSize: {}, pageSize: {}", queryResultSize.get(), pageSize);
                    if (throwable != null) {
                        LOGGER.error("Error Loading scheduled jobs!", throwable);
                        // Review this and emulate an exception
                        if (retries.decrementAndGet() >= 0) {
                            // doLoadJobDetails(fromFireTime, toFireTime, offset, pageSize);
                        } else {
                            // TODO, stop reading and disable current server.
                        }
                    } else if (queryResultSize.get() == 0 || queryResultSize.get() < pageSize) {

                        LOGGER.info("Loading scheduled jobs completed !");
                    } else {
                        doLoadJobDetails(nextFromFireTime.get(), toFireTime, atomicOffset.get(), pageSize);
                    }
                });
    }

    public void doLoadJobDetailsByCreated(ZonedDateTime fromFireTime, ZonedDateTime toFireTime, ZonedDateTime fromCreated, int offset, int pageSize) {
        final AtomicReference<ZonedDateTime> nextFromCreated = new AtomicReference<>(fromCreated);
        final AtomicReference<ZonedDateTime> currentCreated = new AtomicReference<>();
        final AtomicInteger nextOffset = new AtomicInteger(offset);
        final AtomicInteger queryResultSize = new AtomicInteger();
        final AtomicInteger retries = new AtomicInteger(4);

        LOGGER.info("doLoadJobDetails, from: {}, to: {}, created: {}, offset: {}, pageSize: {}", fromFireTime.toOffsetDateTime(), toFireTime.toOffsetDateTime(), fromCreated.toOffsetDateTime(), offset,
                pageSize);
        loadJobsBetweenDatesByCreated(fromFireTime, toFireTime, fromCreated, offset, pageSize)
                .map(jobDetails -> {
                    LOGGER.info("doLoadJobDetails, job found, id: {}, nextFireTime: {}, created: {} ", jobDetails.getId(),
                            DateUtil.instantToZonedDateTime(jobDetails.getTrigger().hasNextFireTime().toInstant()).toOffsetDateTime(),
                            jobDetails.getCreated());
                    //TODO, currentCreated can be null in infinispan or mongodb?
                    currentCreated.set(DateUtil.instantToZonedDateTime(jobDetails.getCreated().toInstant()));
                    if (nextFromCreated.get().toInstant().equals(currentCreated.get().toInstant())) {
                        nextOffset.incrementAndGet();
                    } else {
                        nextFromCreated.set(currentCreated.get());
                        nextOffset.set(1);
                    }
                    queryResultSize.incrementAndGet();
                    return jobDetails;
                })
                .filter(jobDetails -> false && scheduler.scheduled(jobDetails.getId()).isEmpty()) //not consider already scheduled jobs
                .flatMapRsPublisher(jobDetails -> ErrorHandling.skipErrorPublisher(scheduler::schedule, jobDetails))
                .forEach(jobDetails -> LOGGER.debug("Loaded and scheduled job {}", jobDetails))
                .run()
                .whenComplete((unused, throwable) -> {
                    LOGGER.info("doLoadJobDetails, queryResultSize: {}, pageSize: {}", queryResultSize.get(), pageSize);
                    if (throwable != null) {
                        LOGGER.error("Error Loading scheduled jobs!", throwable);
                        // Review this and emulate an exception
                        if (retries.decrementAndGet() >= 0) {
                            // doLoadJobDetails(fromFireTime, toFireTime, offset, pageSize);
                        } else {
                            // TODO, stop reading and disable current server.
                        }
                    } else if (queryResultSize.get() == 0 || queryResultSize.get() < pageSize) {

                        LOGGER.info("Loading scheduled jobs completed !");
                    } else {
                        doLoadJobDetailsByCreated(fromFireTime, toFireTime, nextFromCreated.get(), nextOffset.get(), pageSize);
                    }
                });
    }

    private PublisherBuilder<JobDetails> loadJobsBetweenDates(ZonedDateTime fromFireTime, ZonedDateTime maxFireTime, int offset, int limit) {
        return repository.findByStatusBetweenDates(fromFireTime, maxFireTime,
                null,
                new JobStatus[] { JobStatus.SCHEDULED, JobStatus.RETRY },
                new ReactiveJobRepository.SortTerm[] {
                        ReactiveJobRepository.SortTerm.of(FIRE_TIME, true),
                        ReactiveJobRepository.SortTerm.of(CREATED, true) },
                offset, limit);
    }

    private PublisherBuilder<JobDetails> loadJobsBetweenDatesByCreated(ZonedDateTime fromFireTime, ZonedDateTime maxFireTime, ZonedDateTime createdFrom, int offset, int limit) {
        return repository.findByStatusBetweenDates(fromFireTime, maxFireTime,
                createdFrom,
                new JobStatus[] { JobStatus.SCHEDULED, JobStatus.RETRY },
                new ReactiveJobRepository.SortTerm[] {
                        ReactiveJobRepository.SortTerm.of(CREATED, true),
                        ReactiveJobRepository.SortTerm.of(FIRE_TIME, true),
                        ReactiveJobRepository.SortTerm.of(ID, true) },
                offset, limit);
    }

    //TODO, Remove
    public List<JobDetails> doLoadJobDetailsBlocking(ZonedDateTime fromFireTime, ZonedDateTime toFireTime, int pageSize) {
        boolean hasFinished = false;
        final AtomicReference<ZonedDateTime> nextFromFireTime = new AtomicReference<>(fromFireTime);
        final AtomicReference<ZonedDateTime> currentFireTime = new AtomicReference<>();
        final AtomicInteger queryResultSize = new AtomicInteger();
        final AtomicInteger offset = new AtomicInteger(0);
        final List<JobDetails> result = new ArrayList<>();
        int readIteration = 1;

        // JobStatus.SCHEDULED, JobStatus.RETRY);

        while (!hasFinished) {
            LOGGER.debug("Staring read iteration: #{}", readIteration);
            queryResultSize.set(0);
            try {
                loadJobsBetweenDates(nextFromFireTime.get(), toFireTime, offset.get(), pageSize)
                        .map(jobDetails -> {
                            currentFireTime.set(DateUtil.instantToZonedDateTime(jobDetails.getTrigger().hasNextFireTime().toInstant()));
                            if (currentFireTime.get().toInstant().equals(nextFromFireTime.get().toInstant())) {
                                offset.incrementAndGet();
                            } else {
                                nextFromFireTime.set(currentFireTime.get());
                                offset.set(1);
                            }
                            queryResultSize.incrementAndGet();
                            result.add(jobDetails);
                            return jobDetails;
                        })
                        .filter(jobDetails -> scheduler.scheduled(jobDetails.getId()).isEmpty()) //not consider already scheduled jobs
                        .flatMapRsPublisher(jobDetails -> ErrorHandling.skipErrorPublisher(scheduler::schedule, jobDetails))
                        .forEach(jobDetails -> LOGGER.debug("Loaded and scheduled job {}", jobDetails))
                        .run()
                        .toCompletableFuture()
                        .get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JobsServiceException("An error was produced during Jobs Service initialization procedure.", e);
            } catch (ExecutionException e) {
                throw new JobsServiceException("An error was produced during Jobs Service initialization procedure.", e);
            }
            LOGGER.debug("Read iteration results, for read iteration: #{} are, resultSize: {}, pageSize: {}", readIteration, queryResultSize.get(), pageSize);
            readIteration++;
            if (queryResultSize.get() < pageSize) {
                LOGGER.debug("No more pages needs to be loaded");
                hasFinished = true;
            }
        }
        LOGGER.info("Loading scheduled jobs completed !");
        LOGGER.debug("Total jobs read {}", result.size());
        initialLoading.set(false);
        return result;
    }

    //TODO, Remove
    public List<JobDetails> pagedLoadJobsFromFireTime(ZonedDateTime fromFireTime, ZonedDateTime toFireTime, int pageSize) {

        boolean hasFinished = false;
        final AtomicReference<ZonedDateTime> nextFromFireTime = new AtomicReference<>(fromFireTime);
        final AtomicReference<ZonedDateTime> currentFireTime = new AtomicReference<>();
        final AtomicInteger queryResultSize = new AtomicInteger();
        final AtomicInteger offset = new AtomicInteger(0);
        final List<JobDetails> result = new ArrayList<>();
        int readIteration = 1;

        while (!hasFinished) {
            LOGGER.info("Staring read iteration: #{}", readIteration);
            queryResultSize.set(0);
            try {
                loadJobsBetweenDates(nextFromFireTime.get(), toFireTime, offset.get(), pageSize)
                        .forEach(jobDetails -> {
                            currentFireTime.set(DateUtil.instantToZonedDateTime(jobDetails.getTrigger().hasNextFireTime().toInstant()));
                            LOGGER.info("Loading job: {}, with nextFireTime: {}, created: {}", jobDetails.getId(), currentFireTime, jobDetails.getCreated());
                            //TODO, could have null date?
                            //cuidado, si uso el currentFireTime.get().equals(nextFromFireTime.get())
                            //me puede venir la misma fecha/hora, pero si una tiene timezone Z y la otra UTC, que es basicametne lo mismo
                            //me dice que son distitnas
                            if (currentFireTime.get().toInstant().equals(nextFromFireTime.get().toInstant())) {
                                offset.incrementAndGet();
                            } else {
                                nextFromFireTime.set(currentFireTime.get());
                                offset.set(1);
                            }
                            queryResultSize.incrementAndGet();
                            result.add(jobDetails);
                        })
                        .run()
                        .toCompletableFuture()
                        .get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JobsServiceException("An error was produced during Jobs Service initialization procedure.", e);
            } catch (ExecutionException e) {
                throw new JobsServiceException("An error was produced during Jobs Service initialization procedure.", e);
            }
            LOGGER.info("Read iteration results, for read iteration: #{} are, resultSize: {}, pageSize: {}", readIteration, queryResultSize.get(), pageSize);
            readIteration++;
            if (queryResultSize.get() == 0 || queryResultSize.get() < pageSize) {
                LOGGER.debug("No more pages needs to be loaded");
                hasFinished = true;
            }
        }
        LOGGER.debug("Total jobs read {}", result.size());
        return result;
    }
}
