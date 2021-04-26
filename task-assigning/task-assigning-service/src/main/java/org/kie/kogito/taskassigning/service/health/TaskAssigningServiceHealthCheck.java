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

package org.kie.kogito.taskassigning.service.health;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.kie.kogito.taskassigning.service.Message;
import org.kie.kogito.taskassigning.service.TaskAssigningService;
import org.kie.kogito.taskassigning.service.TaskAssigningServiceContext;

@ApplicationScoped
public class TaskAssigningServiceHealthCheck {

    public static final String SERVER_STATUS = "server-status";
    public static final String SERVER_MESSAGE = "server-message";
    public static final String LIVENESS_NAME = "Task Assigning Service - liveness check";
    public static final String READINESS_NAME = "Task Assigning Service - readiness check";

    @Inject
    TaskAssigningService service;

    @Produces
    @Liveness
    HealthCheck livenessCheck() {
        return () -> {
            TaskAssigningServiceContext.StatusInfo statusInfo = service.getContext().getStatusInfo();
            HealthCheckResponseBuilder builder = newBuilder(LIVENESS_NAME, statusInfo);
            if (statusInfo.getStatus() == TaskAssigningService.Status.ERROR ||
                    statusInfo.getStatus() == TaskAssigningService.Status.SHUTDOWN) {
                return builder.down().build();
            } else {
                return builder.up().build();
            }
        };
    }

    @Produces
    @Readiness
    HealthCheck readinessCheck() {
        return () -> {
            TaskAssigningServiceContext.StatusInfo statusInfo = service.getContext().getStatusInfo();
            HealthCheckResponseBuilder builder = newBuilder(READINESS_NAME, statusInfo);
            if (statusInfo.getStatus() == TaskAssigningService.Status.READY) {
                return builder.up().build();
            } else {
                return builder.down().build();
            }
        };
    }

    private static HealthCheckResponseBuilder newBuilder(String name, TaskAssigningServiceContext.StatusInfo info) {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder()
                .name(name)
                .withData(SERVER_STATUS, info.getStatus().name());
        if (info.getStatusMessage() != null) {
            builder.withData(SERVER_MESSAGE, buildServerMessage(info.getStatusMessage()));
        }
        return builder;
    }

    private static String buildServerMessage(Message message) {
        return String.format("[%s]:[%s]:[%s]", message.getSeverity().name(), message.getTimestamp(), message.getValue());
    }
}
