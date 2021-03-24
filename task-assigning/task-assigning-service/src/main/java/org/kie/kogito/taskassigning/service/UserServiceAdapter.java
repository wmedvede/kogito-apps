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

package org.kie.kogito.taskassigning.service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;

import org.kie.kogito.taskassigning.service.config.TaskAssigningConfig;
import org.kie.kogito.taskassigning.service.event.TaskAssigningServiceEventConsumer;
import org.kie.kogito.taskassigning.service.event.UserDataEvent;
import org.kie.kogito.taskassigning.user.service.api.UserServiceConnector;

public class UserServiceAdapter {

    private TaskAssigningConfig config;

    private TaskAssigningServiceEventConsumer taskAssigningServiceEventConsumer;

    private ExecutorService executorService;

    private UserServiceConnector userServiceConnector;

    private UserServiceSynchronizer userServiceSynchronizer;

    public UserServiceAdapter(TaskAssigningConfig config,
            TaskAssigningServiceEventConsumer taskAssigningServiceEventConsumer,
            ExecutorService executorService,
            UserServiceConnector userServiceConnector) {
        this.config = config;
        this.taskAssigningServiceEventConsumer = taskAssigningServiceEventConsumer;
        this.executorService = executorService;
        this.userServiceConnector = userServiceConnector;
    }

    public void start() {
        userServiceSynchronizer = new UserServiceSynchronizer(userServiceConnector);
        executorService.execute(userServiceSynchronizer);
        userServiceSynchronizer.start(this::onUsersLoad,
                config.getUserServiceSyncInterval(),
                config.getUsersServiceSyncRetryInterval(),
                config.getUserServiceSyncRetries());
    }

    private void onUsersLoad(UserServiceSynchronizer.Result result) {
        if (result.hasErrors()) {
            userServiceSynchronizer.start(this::onUsersLoad, Duration.ZERO, null, 1);
        } else {
            taskAssigningServiceEventConsumer.accept(new UserDataEvent(result.getUsers(), ZonedDateTime.now()));
            userServiceSynchronizer.start(this::onUsersLoad,
                    config.getUserServiceSyncInterval(),
                    config.getUsersServiceSyncRetryInterval(),
                    config.getUserServiceSyncRetries());
        }
    }

    public void destroy() {
        if (userServiceSynchronizer != null) {
            userServiceSynchronizer.destroy();
        }
    }
}
