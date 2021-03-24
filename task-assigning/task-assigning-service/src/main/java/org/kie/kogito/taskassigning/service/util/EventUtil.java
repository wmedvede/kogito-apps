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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kie.kogito.taskassigning.service.TaskAssigningServiceContext;
import org.kie.kogito.taskassigning.service.event.DataEvent;
import org.kie.kogito.taskassigning.service.event.TaskDataEvent;
import org.kie.kogito.taskassigning.service.event.UserDataEvent;

public class EventUtil {

    private EventUtil() {
    }

    public static List<TaskDataEvent> filterTaskDataEvents(List<DataEvent<?>> dataEvents) {
        return dataEvents.stream()
                .filter(dataEvent -> dataEvent.getDataEventType() == DataEvent.DataEventType.TASK_DATA_EVENT)
                .map(TaskDataEvent.class::cast)
                .collect(Collectors.toList());
    }

    public static List<UserDataEvent> filterUserDataEvents(List<DataEvent<?>> dataEvents) {
        return dataEvents.stream()
                .filter(dataEvent -> dataEvent.getDataEventType() == DataEvent.DataEventType.USER_DATA_EVENT)
                .map(UserDataEvent.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Given a list of events finds the newest event per each task (in case if any) that were never processed in current
     * context and adds it to the results list.
     * The returned events are automatically marked as processed in the context.
     *
     * @param context the context instance that holds the processed events information.
     * @param taskDataEvents a list of events to filter.
     * @return a list of events were each event is the newest one that could be found for the given task that was never
     *         processed in the current context before.
     */
    public static List<TaskDataEvent> filterNewestTaskEventsInContext(TaskAssigningServiceContext context, List<DataEvent<?>> dataEvents) {
        List<TaskDataEvent> result = new ArrayList<>();
        List<TaskDataEvent> newestTaskEvents = new ArrayList<>(filterNewestTaskEvents(taskDataEvents));
        for (TaskDataEvent taskEvent : newestTaskEvents) {
            if (context.isNewTaskEventTime(taskEvent.getTaskId(), taskEvent.getEventTime())) {
                context.setTaskLastEventTime(taskEvent.getTaskId(), taskEvent.getEventTime());
                result.add(taskEvent);
            }
        }
        return result;
    }

    /**
     * Given a list of events finds the newest event per each task (in case if any) and it to the results list.
     * 
     * @param taskDataEvents a list of events to filter.
     * @return a list of events were each event is the newest one that could be found for the given task.
     */
    public static List<TaskDataEvent> filterNewestTaskEvents(List<DataEvent<?>> dataEvents) {
        Map<String, TaskDataEvent> lastEventPerTask = new HashMap<>();
        TaskDataEvent previousEventForTask;
        dataEvents.stream()
                .filter(dataEvent -> dataEvent.getDataEventType() == DataEvent.DataEventType.TASK_DATA_EVENT)
                .map(TaskDataEvent.class::cast)
                .forEach(event -> {
            previousEventForTask = lastEventPerTask.get(event.getTaskId());
            if (previousEventForTask == null || event.getEventTime().isAfter(previousEventForTask.getEventTime())) {
                lastEventPerTask.put(event.getData().getId(), event);
            }
        })
        return new ArrayList<>(lastEventPerTask.values());
    }

    public static class UserDataEventSet {
        private Map<String, UserDataEvent> lastEventPerUser;
        private UserDataEvent lastFullSyncEvent;

        public UserDataEventSet(Map<String, UserDataEvent> lastEventPerUser, UserDataEvent lastFullSyncEvent) {
            this.lastEventPerUser = lastEventPerUser;
            this.lastFullSyncEvent = lastFullSyncEvent;
        }

        public boolean isEmpty() {
            return lastFullSyncEvent == null && lastEventPerUser.isEmpty();
        }

        public Map<String, UserDataEvent> getLastEventPerUser() {
            return lastEventPerUser;
        }

        public UserDataEvent getLastFullSyncEvent() {
            return lastFullSyncEvent;
        }
    }

    public static UserDataEvent filterNewestUserEvent(List<DataEvent<?>> dataEvents) {
        UserDataEvent[] newestUserEvent = new UserDataEvent[1];
        dataEvents.stream()
                .filter(event -> event.getDataEventType() == DataEvent.DataEventType.USER_DATA_EVENT)
                .map(UserDataEvent.class::cast)
                .forEach(userEvent -> {
                    if (newestUserEvent[0] == null || userEvent.getEventTime().isAfter(newestUserEvent[0].getEventTime())) {
                        newestUserEvent[0] = userEvent;
                    }
                });
        return newestUserEvent[0];
    }
}
