/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.taskassigning.service.config;

import java.util.Optional;

import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_BASIC_AUTH_USER;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_KEYCLOAK_AUTH_CLIENT_ID;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_KEYCLOAK_AUTH_PASSWORD;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_KEYCLOAK_AUTH_REALM;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_KEYCLOAK_AUTH_SECRET;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_KEYCLOAK_AUTH_SERVER_URL;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_KEYCLOAK_AUTH_USER;

public class TaskAssigningConfigValidator {

    private TaskAssigningConfig config;

    private TaskAssigningConfigValidator(TaskAssigningConfig config) {
        this.config = config;
    }

    public static TaskAssigningConfigValidator of(TaskAssigningConfig config) {
        return new TaskAssigningConfigValidator(config);
    }

    public void validate() {
        if (config.isDataIndexKeycloakSet() && config.isDataIndexBasicAuthSet()) {
            throw new IllegalArgumentException("It looks like keycloak and basic authentication are configured at the same time." +
                                                       " Please use only one of these mechanisms.");
        }
        if (config.isDataIndexKeycloakSet()) {
            validateDataIndexKeycloak(config);
        }
        if (config.isDataIndexBasicAuthSet()) {
            validateDataIndexBasicAuth(config);
        }
    }

    private static void validateDataIndexKeycloak(TaskAssigningConfig config) {
        validateOptionalIsSet(DATA_INDEX_CLIENT_KEYCLOAK_AUTH_SERVER_URL, config.getDataIndexClientKeycloakAuthServerUrl());
        validateOptionalIsSet(DATA_INDEX_CLIENT_KEYCLOAK_AUTH_REALM, config.getDataIndexClientKeycloakAuthRealm());
        validateOptionalIsSet(DATA_INDEX_CLIENT_KEYCLOAK_AUTH_CLIENT_ID, config.getDataIndexClientKeycloakAuthClientId());
        validateOptionalIsSet(DATA_INDEX_CLIENT_KEYCLOAK_AUTH_SECRET, config.getDataIndexClientKeycloakAuthSecret());
        validateOptionalIsSet(DATA_INDEX_CLIENT_KEYCLOAK_AUTH_USER, config.getDataIndexClientKeycloakAuthUser());
        validateOptionalIsSet(DATA_INDEX_CLIENT_KEYCLOAK_AUTH_PASSWORD, config.getDataIndexClientKeycloakAuthPassword());
    }

    private static void validateDataIndexBasicAuth(TaskAssigningConfig config) {
        validateOptionalIsSet(DATA_INDEX_CLIENT_BASIC_AUTH_USER, config.getDataIndexClientBasicAuthUser());
    }

    private static void validateOptionalIsSet(String propertyName, Optional<?> value) {
        if (value.isEmpty()) {
            //TODO use a specific excpetion review the message
            throw new IllegalArgumentException("A config value must be set for the property: " + propertyName + " when the keycloak authentication is used.");
        }
    }
}


