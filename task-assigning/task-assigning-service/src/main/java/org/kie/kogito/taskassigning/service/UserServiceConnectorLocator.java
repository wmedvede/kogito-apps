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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.kogito.taskassigning.service.config.TaskAssigningConfig;
import org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties;
import org.kie.kogito.taskassigning.user.service.UserServiceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UserServiceConnectorLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceConnectorLocator.class);

    private final UserServiceConnector instance;

    @Inject
    public UserServiceConnectorLocator(UserServiceConnectorRegistry userServiceConnectorRegistry,
            TaskAssigningConfig config) {
        instance = userServiceConnectorRegistry.get(config.getUserServiceConnector());
        if (instance == null) {
            throw new IllegalArgumentException("No user service connector was found for the configured name " +
                    TaskAssigningConfigProperties.USER_SERVICE_CONNECTOR + " = " + config.getUserServiceConnector());
        }
        if (TaskAssigningConfig.DEFAULT_USER_SERVICE_CONNECTOR.equals(config.getUserServiceConnector())) {
            LOGGER.warn("The default user service connector: {} should only be used in development or testing environments.", config.getUserServiceConnector());
        }
    }

    public UserServiceConnector getInstance() {
        return instance;
    }
}
