/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.kogito.jobs.service.scheduler;

import java.time.OffsetDateTime;
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

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.kie.kogito.jobs.JobsServiceException;
import org.kie.kogito.jobs.service.management.LeaderStatusChangeEvent;
import org.kie.kogito.jobs.service.model.JobDetails;
import org.kie.kogito.jobs.service.model.JobStatus;
import org.kie.kogito.jobs.service.repository.ReactiveJobRepository;
import org.kie.kogito.jobs.service.scheduler.impl.TimerDelegateJobScheduler;
import org.kie.kogito.jobs.service.utils.DateUtil;
import org.kie.kogito.jobs.service.utils.ErrorHandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;

@ApplicationScoped
public class JobSchedulerManager {

    private static final long NO_JOB = -1;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerManager.class);

    /**
     * The current chunk size in minutes the scheduler handles, it is used to keep a limit number of jobs scheduled
     * in the in-memory scheduler.
     */
    @ConfigProperty(name = "kogito.jobs-service.schedulerChunkInMinutes")
    long schedulerChunkInMinutes;

    /**
     * The interval the job loading method runs to fetch the persisted jobs from the repository.
     */
    @ConfigProperty(name = "kogito.jobs-service.loadJobIntervalInMinutes")
    long loadJobIntervalInMinutes;

    /**
     * The interval based on the current time the job loading method uses to fetch jobs "FROM (now -
     * {@link #loadJobFromCurrentTimeIntervalInMinutes}) TO {@link #schedulerChunkInMinutes}"
     */
    @ConfigProperty(name = "kogito.jobs-service.loadJobFromCurrentTimeIntervalInMinutes")
    long loadJobFromCurrentTimeIntervalInMinutes;

    @ConfigProperty(name = "test.InitialMaxFireTime")
    Optional<String> testInitialMaxFireTime;

    @ConfigProperty(name = "test.pageSize")
    Optional<Integer> testPageSize;

    @Inject
    TimerDelegateJobScheduler scheduler;

    @Inject
    ReactiveJobRepository repository;

    @Inject
    Vertx vertx;

    @Inject
    ManagedExecutor managedExecutor;

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    private final AtomicLong jobDetailsLoader = new AtomicLong(NO_JOB);

    void onStartup(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) StartupEvent startupEvent) {
        System.out.println("XXXXXXXXXX JobSchedulerManager.onStartup starting: " + OffsetDateTime.now());
        if (loadJobIntervalInMinutes > schedulerChunkInMinutes) {
            LOGGER.warn("The loadJobIntervalInMinutes ({}) cannot be greater than schedulerChunkInMinutes ({}), " +
                    "setting value {} for both",
                    loadJobIntervalInMinutes,
                    schedulerChunkInMinutes,
                    schedulerChunkInMinutes);
            loadJobIntervalInMinutes = schedulerChunkInMinutes;
        }
        System.out.println("XXXXXXXXXX JobSchedulerManager.onStartup finished!: " + OffsetDateTime.now());
    }

    protected void onLeaderStatusChange(@Observes @Priority(LeaderStatusChangeEvent.LEADER_STATUS_CHANGE_INTERCEPTOR_LOW_PRIORITY) LeaderStatusChangeEvent event) {
        System.out.println("XXXXXXXXX JobSchedulerManager.onLeaderStatusChange: started " + event.isLeader());
        enabled.set(event.isLeader());
        if (enabled.get()) {
            setLeaderStatusOn();
        } else {
            setLeaderStatusOff();
        }
        System.out.println("XXXXXXXXX JobSchedulerManager.onLeaderStatusChange: finished! " + event.isLeader());
    }

    private synchronized void setLeaderStatusOn() {
        System.out.println("XXXXXXXXXX JobSchedulerManager.setLeaderStatusOn started: " + OffsetDateTime.now());
        managedExecutor.runAsync(this::initialLoadJobDetails).exceptionally(throwable -> {
            throw new JobsServiceException(throwable.getMessage(), throwable.getCause());
        });
        //vertx.runOnContext(this::loadJobDetailsNew);

        //periodic execution
        jobDetailsLoader.set(vertx.setPeriodic(TimeUnit.MINUTES.toMillis(loadJobIntervalInMinutes), id -> loadJobDetails()));
        System.out.println("XXXXXXXXXX JobSchedulerManager.setLeaderStatusOn finished: " + OffsetDateTime.now());
    }

    private synchronized void setLeaderStatusOff() {
        System.out.println("XXXXXXXXXX JobSchedulerManager.setLeaderStatusOff started: " + OffsetDateTime.now());
        if (jobDetailsLoader.getAndSet(NO_JOB) != NO_JOB) {
            vertx.cancelTimer(jobDetailsLoader.get());
        }
        scheduler.removeScheduledJobHandles();
        System.out.println("XXXXXXXXXX JobSchedulerManager.setLeaderStatusOff finished: " + OffsetDateTime.now());
    }

    //Runs periodically loading the jobs from the repository in chunks
    void loadJobDetails() {
        if (!enabled.get()) {
            LOGGER.info("Skip loading scheduled jobs");
            return;
        }
        scheduleJobDetails(loadJobsInCurrentChunk());
    }

    void initialLoadJobDetails() {
        if (!enabled.get()) {
            LOGGER.info("Skipping current initialLoadJobDetails invocation, current service instance is not enabled.");
            return;
        }

        boolean hasFinished = false;
        final AtomicReference<ZonedDateTime> nextFromFireTime = new AtomicReference<>(ZonedDateTime.parse("1970-01-01T00:00:00+00:00"));
        final AtomicReference<ZonedDateTime> currentFireTime = new AtomicReference<>();
        ZonedDateTime maxFireTime = DateUtil.now().plusMinutes(schedulerChunkInMinutes);
        final AtomicInteger resultSize = new AtomicInteger();
        final AtomicInteger offset = new AtomicInteger(0);
        int pageSize = 2;
        final List<JobDetails> jobsToSchedule = new ArrayList<>();
        int readIteration = 1;

        //TODO remove this.
        if (testInitialMaxFireTime.isPresent()) {
            maxFireTime = ZonedDateTime.parse(testInitialMaxFireTime.get());
        }
        if (testPageSize.isPresent()) {
            pageSize = testPageSize.get();
        }

        while (!hasFinished && enabled.get()) {
            LOGGER.debug("Staring read iteration: #{}", readIteration);
            resultSize.set(0);
            try {
                loadJobsFromFireTime(nextFromFireTime.get(), maxFireTime, offset.get(), pageSize)
                        .forEach(jobDetails -> {
                            currentFireTime.set(DateUtil.instantToZonedDateTime(jobDetails.getTrigger().hasNextFireTime().toInstant()));
                            LOGGER.debug("Loading job: {}, with nextFireTime: {}", jobDetails.getId(), currentFireTime);
                            //TODO, could have null date?
                            if (currentFireTime.get().equals(nextFromFireTime.get())) {
                                offset.incrementAndGet();
                            } else {
                                nextFromFireTime.set(currentFireTime.get());
                                offset.set(1);
                            }
                            resultSize.incrementAndGet();
                            jobsToSchedule.add(jobDetails);
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
            LOGGER.debug("Read iteration results, for read iteration: #{} are, resultSize: {}, pageSize: {}", readIteration, resultSize.get(), pageSize);
            readIteration++;
            if (resultSize.get() < pageSize) {
                LOGGER.debug("No more pages needs to be loaded.");
                hasFinished = true;
            }
        }
        LOGGER.debug("Total jobs to schedule for initialization: {}", jobsToSchedule.size());
        if (enabled.get()) {
            scheduleJobDetails(ReactiveStreams.fromIterable(jobsToSchedule));
        } else {
            LOGGER.info("Skipping current initialLoadJobDetails jobs scheduling, current service instance is not enabled.");
        }
    }

    private void scheduleJobDetails(PublisherBuilder<JobDetails> details) {
        details.filter(j -> scheduler.scheduled(j.getId()).isEmpty())//not consider already scheduled jobs
                .flatMapRsPublisher(t -> ErrorHandling.skipErrorPublisher(scheduler::schedule, t))
                .forEach(a -> LOGGER.debug("Loaded and scheduled job {}", a))
                .run()
                .whenComplete((v, t) -> Optional.ofNullable(t)
                        .map(ex -> {
                            LOGGER.error("Error Loading scheduled jobs!", ex);
                            return null;
                        })
                        .orElseGet(() -> {
                            LOGGER.info("Loading scheduled jobs completed !");
                            return null;
                        }));
    }

    private PublisherBuilder<JobDetails> loadJobsInCurrentChunk() {
        return repository.findByStatusBetweenDatesOrderByPriority(DateUtil.now().minusMinutes(loadJobFromCurrentTimeIntervalInMinutes),
                DateUtil.now().plusMinutes(schedulerChunkInMinutes),
                JobStatus.SCHEDULED, JobStatus.RETRY);
    }

    private PublisherBuilder<JobDetails> loadJobsFromFireTime(ZonedDateTime fromFireTime, ZonedDateTime maxFireTime, int offset, int limit) {
        return repository.findByStatusBetweenDatesPaged(fromFireTime, maxFireTime,
                new JobStatus[] { JobStatus.SCHEDULED, JobStatus.RETRY }, "fire_time", true, offset, limit);
    }
}
