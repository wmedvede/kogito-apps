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

public class TaskAssigningConfigProperties {

    private TaskAssigningConfigProperties() {
    }

    public static final String TASK_ASSIGNING_PROPERTY_PREFIX = "kogito.task-assigning";

    public static final String DATA_INDEX_SERVER_URL = TASK_ASSIGNING_PROPERTY_PREFIX + ".data-index.server-url";

    public static final String DATA_INDEX_CLIENT_BASIC_AUTH_USER = TASK_ASSIGNING_PROPERTY_PREFIX + ".data-index.client.auth.basic.user";

    public static final String DATA_INDEX_CLIENT_BASIC_AUTH_PASSWORD = TASK_ASSIGNING_PROPERTY_PREFIX + ".data-index.client.auth.basic.password";

    public static final String DATA_INDEX_CLIENT_KEYCLOAK_AUTH_SERVER_URL = TASK_ASSIGNING_PROPERTY_PREFIX + ".data-index.client.auth.keycloak.auth-server-url";

    public static final String DATA_INDEX_CLIENT_KEYCLOAK_AUTH_REALM = TASK_ASSIGNING_PROPERTY_PREFIX + ".data-index.client.auth.keycloak.realm";

    public static final String DATA_INDEX_CLIENT_KEYCLOAK_AUTH_CLIENT_ID = TASK_ASSIGNING_PROPERTY_PREFIX + ".data-index.client.auth.keycloak.client-id";

    public static final String DATA_INDEX_CLIENT_KEYCLOAK_AUTH_SECRET = TASK_ASSIGNING_PROPERTY_PREFIX + ".data-index.client.auth.keycloak.credentials.secret";

    public static final String DATA_INDEX_CLIENT_KEYCLOAK_AUTH_USER = TASK_ASSIGNING_PROPERTY_PREFIX + ".data-index.client.auth.keycloak.user";

    public static final String DATA_INDEX_CLIENT_KEYCLOAK_AUTH_PASSWORD = TASK_ASSIGNING_PROPERTY_PREFIX + ".data-index.client.auth.keycloak.password";

}
