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

package org.kie.kogito.taskassigning.service.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.kie.kogito.taskassigning.core.model.Task;
import org.kie.kogito.taskassigning.index.service.client.graphql.UserTaskInstance;
import org.kie.kogito.taskassigning.service.messaging.UserTaskEvent;

public class TaskUtil {

    private TaskUtil() {
    }

    public static Task fromUserTaskInstance(UserTaskInstance taskInstance) {
        return Task.newBuilder()
                .id(taskInstance.getId())
                .name(taskInstance.getName())
                .state(taskInstance.getState())
                .description(taskInstance.getDescription())
                .referenceName(taskInstance.getReferenceName())
                .priority(taskInstance.getPriority())
                .processInstanceId(taskInstance.getProcessInstanceId())
                .processId(taskInstance.getProcessId())
                .rootProcessInstanceId(taskInstance.getRootProcessInstanceId())
                .rootProcessId(taskInstance.getRootProcessId())
                .potentialUsers(toSet(taskInstance.getPotentialUsers()))
                .potentialGroups(toSet(taskInstance.getPotentialGroups()))
                .adminUsers(toSet(taskInstance.getAdminUsers()))
                .adminGroups(toSet(taskInstance.getAdminGroups()))
                .excludedUsers(toSet(taskInstance.getExcludedUsers()))
                .started(taskInstance.getStarted())
                .completed(taskInstance.getCompleted())
                .lastUpdate(taskInstance.getLastUpdate())
                //TODO Upcoming iteration
                //.inputData(taskInstance.getInputs())
                .endpoint(taskInstance.getEndpoint())
                .build();
    }

    //TODO ver porque se queja esta mierda por duplicados.
    public static Task fromUserTaskEvent(UserTaskEvent userTaskEvent) {
        return Task.newBuilder()
                .id(userTaskEvent.getId())
                .name(userTaskEvent.getName())
                .state(userTaskEvent.getState())
                .description(userTaskEvent.getDescription())
                .referenceName(userTaskEvent.getReferenceName())
                .priority(userTaskEvent.getPriority())
                .processInstanceId(userTaskEvent.getProcessInstanceId())
                .processId(userTaskEvent.getProcessId())
                .rootProcessInstanceId(userTaskEvent.getRootProcessInstanceId())
                .rootProcessId(userTaskEvent.getRootProcessId())
                .potentialUsers(toSet(userTaskEvent.getPotentialUsers()))
                .potentialGroups(toSet(userTaskEvent.getPotentialGroups()))
                .adminUsers(toSet(userTaskEvent.getAdminUsers()))
                .adminGroups(toSet(userTaskEvent.getAdminGroups()))
                .excludedUsers(toSet(userTaskEvent.getExcludedUsers()))
                .started(userTaskEvent.getStarted())
                .completed(userTaskEvent.getCompleted())
                .lastUpdate(userTaskEvent.getLastUpdate())
                //TODO Upcoming iteration
                //.inputData(taskInstance.getInputs())
                .endpoint(userTaskEvent.getEndpoint())
                .build();
    }

    private static <T> Set<T> toSet(Collection<T> collection) {
        return collection != null ? new HashSet<>(collection) : new HashSet<>();
    }
}
