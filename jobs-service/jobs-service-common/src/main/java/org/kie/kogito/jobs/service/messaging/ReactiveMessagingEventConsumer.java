/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.jobs.service.messaging;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.kie.kogito.jobs.api.event.CancelJobRequestEvent;
import org.kie.kogito.jobs.api.event.CreateProcessInstanceJobRequestEvent;
import org.kie.kogito.jobs.api.event.JobCloudEvent;
import org.kie.kogito.jobs.service.exception.JobServiceException;
import org.kie.kogito.jobs.service.model.JobStatus;
import org.kie.kogito.jobs.service.model.ScheduledJob;
import org.kie.kogito.jobs.service.model.job.JobDetails;
import org.kie.kogito.jobs.service.model.job.ScheduledJobAdapter;
import org.kie.kogito.jobs.service.repository.ReactiveJobRepository;
import org.kie.kogito.jobs.service.scheduler.impl.TimerDelegateJobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.kogito.jobs.api.event.CancelJobRequestEvent.CANCEL_JOB_REQUEST;
import static org.kie.kogito.jobs.api.event.CreateProcessInstanceJobRequestEvent.CREATE_PROCESS_INSTANCE_JOB_REQUEST;

@ApplicationScoped
public class ReactiveMessagingEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveMessagingEventConsumer.class);

    private static final String KOGITO_JOB_SERVICE_JOB_REQUEST_EVENTS = "kogito-job-service-job-request-events";

    private static final String KOGITO_JOB_SERVICE_TRIGGER_REQUEST_EVENTS = "kogito-job-service-trigger-request-events";

    private static final String KOGITO_JOB_SERVICE_TRIGGER_RESPONSE_EVENTS = "kogito-job-service-trigger-response-events";

    //kogito-job-service-job-status-events

    private static final String JOB_SERVICE_REQUEST_EVENTS = "job-service-request-events";

    @Inject
    TimerDelegateJobScheduler scheduler;

    @Inject
    ReactiveJobRepository jobRepository;

    @Incoming(KOGITO_JOB_SERVICE_JOB_REQUEST_EVENTS)
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    @Retry(delay = 500, maxRetries = 4)
    public Uni<Void> onKogitoServiceRequest(Message<JobCloudEvent<?>> message) {
        JobCloudEvent<?> jobCloudEvent = message.getPayload();
        LOGGER.info("VAMO CARAJO: " + ZonedDateTime.now());
        switch (jobCloudEvent.getType()) {
            case CREATE_PROCESS_INSTANCE_JOB_REQUEST:
                return handleEvent(message, (CreateProcessInstanceJobRequestEvent) jobCloudEvent);
            case CANCEL_JOB_REQUEST:
                return handleEvent(message, (CancelJobRequestEvent) jobCloudEvent);
            default:
                LOGGER.error("Invalid kogito service request type: {}, for the cloud event: {}", jobCloudEvent.getType(), jobCloudEvent);
                return Uni.createFrom().completionStage(message.nack(new JobServiceException("Invalid kogito service request type: " + jobCloudEvent.getType())));
        }
    }

    private Uni<Void> handleEvent(Message<JobCloudEvent<?>> message, CreateProcessInstanceJobRequestEvent event) {
        return Uni.createFrom().completionStage(jobRepository.get(event.getData().getId()))
                .flatMap(existingJob -> {
                    if (existingJob == null || existingJob.getStatus() == JobStatus.SCHEDULED) {
                        return Uni.createFrom().publisher(scheduler.schedule(ScheduledJobAdapter.to(ScheduledJob.builder().job(event.getData()).build())));
                    } else {
                        LOGGER.info("A Job in status: {} already exists for the job id: {}, no processing will be done fot the event: {}.",
                                    existingJob.getStatus(),
                                    existingJob.getId(),
                                    event);
                        return Uni.createFrom().item(existingJob);
                    }
                })
                .onItem().transformToUni(createdJob -> {
                    if (createdJob == null) {
                        // The scheduler halted the stream processing by emitting no values, an error was produced.
                        return Uni.createFrom().failure(new JobServiceException("An internal scheduler error was produced during Job scheduling"));
                    } else {
                        return Uni.createFrom().completionStage(message.ack());
                    }
                }).onFailure().recoverWithUni(throwable -> {
                    String msg = String.format("An error was produced during Job scheduling for the event: %s", event);
                    LOGGER.error(msg, throwable);
                    return Uni.createFrom().completionStage(message.nack(new JobServiceException("An error was produced during Job scheduling: " + throwable.getMessage(), throwable)));
                });
    }

    private Uni<Void> handleEvent(Message<JobCloudEvent<?>> message, CancelJobRequestEvent event) {
        return Uni.createFrom().completionStage(scheduler.cancel(event.getData().getId()))
                .onItemOrFailure().transformToUni((cancelledJob, throwable) -> {
                    if (throwable != null) {
                        String msg = String.format("An error was produced during Job cancelling for the event: %s", event);
                        LOGGER.error(msg, throwable);
                        return Uni.createFrom().completionStage(message.nack(new JobServiceException("An error was produced during Job cancelling: " + throwable.getMessage(), throwable)));
                    } else {
                        if (cancelledJob == null) {
                            LOGGER.info("No Job exists for the job id: {} or it was already cancelled", event.getData().getId());
                        }
                        return Uni.createFrom().completionStage(message.ack());
                    }
                });
    }

    //@Incoming(KOGITO_SERVICE_REQUEST_EVENTS)
    //@Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public CompletionStage<Void> onKogitoServiceRequestOLD(Message<JobCloudEvent<?>> message) {
        JobCloudEvent<?> jobCloudEvent = message.getPayload();
        switch (jobCloudEvent.getType()) {
            case CREATE_PROCESS_INSTANCE_JOB_REQUEST:
                return handleEventOLD(message, (CreateProcessInstanceJobRequestEvent) jobCloudEvent);
            case CANCEL_JOB_REQUEST:
                return handleEventOLD(message, (CancelJobRequestEvent) jobCloudEvent);
            default:
                return message.nack(new JobServiceException("Invalid kogito service request type: " + jobCloudEvent.getType() + " for cloud the event: " + jobCloudEvent));
        }
    }

    private CompletionStage<Void> handleEventOLD(Message<JobCloudEvent<?>> message, CreateProcessInstanceJobRequestEvent event) {
        CompletionStage<Response<JobDetails>> findJobQuery = jobRepository.get(event.getData().getId())
                .thenCompose(existingJob -> CompletableFuture.completedFuture(Response.of(existingJob)));
        return ReactiveStreams.fromCompletionStage(findJobQuery)
                .flatMap(findJobQueryResponse -> {
                    if (findJobQueryResponse.getDetail() == null || findJobQueryResponse.getDetail().getStatus() == JobStatus.SCHEDULED) {
                        return ReactiveStreams.fromPublisher(scheduler.schedule(ScheduledJobAdapter.to(ScheduledJob.builder().job(event.getData()).build())));
                    } else {
                        LOGGER.warn("A Job in status: {} already exists for the job id: {}, no processing will be done for the event: {}.",
                                    findJobQueryResponse.getDetail().getStatus(),
                                    findJobQueryResponse.getDetail().getId(),
                                    event);
                        return ReactiveStreams.of(findJobQueryResponse.getDetail());
                    }
                })
                .flatMap(scheduledJob -> ReactiveStreams.of(Response.of(scheduledJob)))
                .onErrorResumeWith(throwable -> ReactiveStreams.of(Response.of(throwable)))
                .findFirst()
                .run()
                .thenCompose(scheduleJobResponse -> {
                    if (scheduleJobResponse.isEmpty()) {
                        LOGGER.error("An error was produced during Job scheduling for the event: {}", event);
                        return message.nack(new JobServiceException("An internal scheduler error was produced during Job scheduling"));
                    } else if (scheduleJobResponse.get().hasError()) {
                        LOGGER.error("An error was produced during Job scheduling for the event: {}", event);
                        return message.nack(new JobServiceException("An error was produced during Job scheduling: " + scheduleJobResponse.get().getError().getMessage(),
                                                                    scheduleJobResponse.get().getError()));
                    } else {
                        return message.ack();
                    }
                });
    }

    private CompletionStage<Void> handleEventOLD(Message<JobCloudEvent<?>> message, CancelJobRequestEvent event) {
        return null;
    }

    public static class Response<T> {

        private final T detail;
        private final Throwable error;

        private Response(T detail, Throwable error) {
            this.detail = detail;
            this.error = error;
        }

        public static <T> Response<T> of(T jobDetails) {
            return new Response<>(jobDetails, null);
        }

        public static <T> Response<T> of(Throwable error) {
            return new Response<>(null, error);
        }

        public T getDetail() {
            return detail;
        }

        public Throwable getError() {
            return error;
        }

        public boolean hasError() {
            return error != null;
        }
    }
}
