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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.kie.kogito.taskassigning.user.service.api.User;
import org.kie.kogito.taskassigning.user.service.api.UserServiceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.kogito.taskassigning.service.RunnableBase.Status.STARTED;
import static org.kie.kogito.taskassigning.service.RunnableBase.Status.STARTING;
import static org.kie.kogito.taskassigning.service.RunnableBase.Status.STOPPED;

public class UserServiceSynchronizer extends RunnableBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceSynchronizer.class);
    private UserServiceConnector userServiceConnector;
    private Consumer<Result> resultConsumer;
    private Duration startTime;
    private Duration retryInterval;
    private int remainingRetries;

    public static class Result {

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

    public UserServiceSynchronizer(UserServiceConnector userServiceConnector) {
        this.userServiceConnector = userServiceConnector;
    }

    public void start(Consumer<Result> resultConsumer, Duration startTime, Duration retryInterval, int retries) {
        startCheck();
        this.resultConsumer = resultConsumer;
        this.startTime = startTime;
        this.retryInterval = retryInterval;
        this.remainingRetries = retries;
        startPermit.release();
    }

    @Override
    public void run() {
        while (isAlive()) {
            try {
                startPermit.acquire();
                if (isAlive() && status.compareAndSet(STARTING, STARTED)) {
                    Thread.sleep(startTime.toMillis());
                }

                if (isAlive() && status.get() == STARTED) {
                    Result result = loadData();
                    if (result.hasErrors() && hasRemainingRetries()) {
                        decreaseRemainingRetries();
                        Thread.sleep(retryInterval.toMillis());
                        startPermit.release();
                    } else if (isAlive() && status.compareAndSet(STARTED, STOPPED)) {
                        applyResult(result);
                    }
                }
            } catch (InterruptedException e) {
                super.destroy();
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean hasRemainingRetries() {
        return remainingRetries > 0;
    }

    private void decreaseRemainingRetries() {
        remainingRetries--;
    }

    protected Result loadData() {
        try {
            List<User> users = userServiceConnector.findAllUsers();
            return Result.successful(users);
        } catch (Exception e) {
            String msg = "blabla";
            LOGGER.warn(msg);
            LOGGER.debug(msg, e);
            return Result.error(Collections.singletonList(e));
        }
    }

    protected void applyResult(Result result) {
        resultConsumer.accept(result);
    }

}
