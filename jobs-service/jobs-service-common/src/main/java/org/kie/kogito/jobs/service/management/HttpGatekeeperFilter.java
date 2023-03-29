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
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

import static org.kie.kogito.jobs.service.management.LeaderStatusChangeEvent.LEADER_STATUS_CHANGE_INTERCEPTOR_HIGH_PRIORITY;

@ApplicationScoped
public class HttpGatekeeperFilter {

    public static final String ERROR_MESSAGE = "Job Service instance is not master and can not serve requests.";
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    @ConfigProperty(name = "quarkus.smallrye-health.root-path", defaultValue = "/q/health")
    String healthCheckPath;

    protected void onLeaderStatusChange(@Observes @Priority(LEADER_STATUS_CHANGE_INTERCEPTOR_HIGH_PRIORITY) LeaderStatusChangeEvent event) {
        this.enabled.set(event.isLeader());
    }

    @RouteFilter(100)
    void masterFilter(RoutingContext rc) {
        if (!enabled.get() && !rc.request().path().contains(healthCheckPath)) {
            //block
            rc.response().setStatusCode(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
            rc.response().setStatusMessage(ERROR_MESSAGE);
            rc.end();
            return;
        }
        //continue
        rc.next();
    }
}
