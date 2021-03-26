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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.kie.kogito.taskassigning.service.config.TaskAssigningConfig;
import org.kie.kogito.taskassigning.service.event.TaskAssigningServiceEventConsumer;
import org.kie.kogito.taskassigning.service.event.UserDataEvent;
import org.kie.kogito.taskassigning.user.service.User;
import org.kie.kogito.taskassigning.user.service.UserServiceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserServiceAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceAdapter.class);

    private static final String QUERY_ERROR = "An error was produced during users information synchronization." +
            " Next attempt will be in a period of %s, error: %s";

    private TaskAssigningConfig config;

    private TaskAssigningServiceEventConsumer taskAssigningServiceEventConsumer;

    private ExecutorService executorService;

    private UserServiceConnector userServiceConnector;

    private final AtomicBoolean destroyed = new AtomicBoolean(false);

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
        programExecution(config.getUserServiceSyncInterval(),
                config.getUsersServiceSyncRetryInterval(),
                config.getUserServiceSyncRetries());
    }

    public void destroy() {
        destroyed.set(true);
    }

    private void programExecution(Duration nextStartTime, Duration retryInterval, int retries) {
        if (!destroyed.get()) {
            LOGGER.debug("Next users information synchronization will be executed in a period of: {} from now", nextStartTime);
            CompletableFuture.delayedExecutor(nextStartTime.toMillis(),
                    TimeUnit.MILLISECONDS,
                    executorService)
                    .execute(() -> executeQuery(retryInterval, retries));
        }
    }

    private void onQueryResult(Result result) {
        Duration nextStartTime;
        if (!result.hasErrors()) {
            taskAssigningServiceEventConsumer.accept(new UserDataEvent(result.getUsers(), ZonedDateTime.now()));
            nextStartTime = config.getUserServiceSyncInterval();
        } else {
            nextStartTime = config.getUsersServiceSyncRetryInterval();
        }
        programExecution(nextStartTime, config.getUsersServiceSyncRetryInterval(), config.getUserServiceSyncRetries());
    }

    private void executeQuery(Duration retryInterval, int retries) {
        int remainingRetries = retries;
        boolean exit = false;
        while (!destroyed.get() && !exit) {
            try {
                Result result = loadUsers();
                if (result.hasErrors()) {
                    if (remainingRetries > 0) {
                        remainingRetries--;
                        Thread.sleep(retryInterval.toMillis());
                    } else {
                        exit = true;
                    }
                } else if (!destroyed.get()) {
                    exit = true;
                    onQueryResult(result);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private Result loadUsers() {
        try {
            List<User> users = userServiceConnector.findAllUsers();
            return Result.successful(users);
        } catch (Exception e) {
            String msg = String.format(QUERY_ERROR, config.getUsersServiceSyncRetryInterval(), e.getMessage());
            LOGGER.warn(msg);
            LOGGER.debug(msg, e);
            return Result.error(Collections.singletonList(e));
        }
    }

    private static class Result {

        private List<User> users = new ArrayList<>();
        private List<Exception> errors;

        private Result() {
        }

        public static Result successful(List<User> users) {
            Result result = new Result();
            result.users = users;
            return result;
        }

        public static Result error(List<Exception> errors) {
            Result result = new Result();
            result.errors = errors;
            return result;
        }

        public List<User> getUsers() {
            return users;
        }

        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }

        public List<Exception> getErrors() {
            return errors;
        }
    }

}
