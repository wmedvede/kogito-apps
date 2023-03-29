/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
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

package org.kie.kogito.jobs.service.management;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import static org.kie.kogito.jobs.service.management.LeaderStatusChangeEvent.LEADER_STATUS_CHANGE_INTERCEPTOR_HIGH_PRIORITY;

@Readiness
@ApplicationScoped
public class JobServiceLeaderHealthCheck implements HealthCheck {

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    protected void onLeaderStatusChange(@Observes @Priority(LEADER_STATUS_CHANGE_INTERCEPTOR_HIGH_PRIORITY) LeaderStatusChangeEvent event) {
        this.enabled.set(event.isLeader());
    }

    @Override
    public HealthCheckResponse call() {
        final HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("Leader Instance");
        if (enabled.get()) {
            return responseBuilder.up().build();
        }
        return responseBuilder.down().build();
    }
}