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

import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_BASIC_AUTH_PASSWORD;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_BASIC_AUTH_USER;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_KEYCLOAK_AUTH_CLIENT_ID;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_KEYCLOAK_AUTH_PASSWORD;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_KEYCLOAK_AUTH_REALM;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_KEYCLOAK_AUTH_SECRET;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_KEYCLOAK_AUTH_SERVER_URL;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_CLIENT_KEYCLOAK_AUTH_USER;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.DATA_INDEX_SERVER_URL;

@ApplicationScoped
public class TaskAssigningConfig {

    @Inject
    @ConfigProperty(name = DATA_INDEX_SERVER_URL)
    URL dataIndexServerUrl;

    @Inject
    @ConfigProperty(name = DATA_INDEX_CLIENT_BASIC_AUTH_USER)
    Optional<String> dataIndexClientBasicAuthUser;

    @Inject
    @ConfigProperty(name = DATA_INDEX_CLIENT_BASIC_AUTH_PASSWORD)
    Optional<String> dataIndexClientBasicAuthPassword;

    @Inject
    @ConfigProperty(name = DATA_INDEX_CLIENT_KEYCLOAK_AUTH_SERVER_URL)
    Optional<URL> dataIndexClientKeycloakAuthServerUrl;

    @Inject
    @ConfigProperty(name = DATA_INDEX_CLIENT_KEYCLOAK_AUTH_REALM)
    Optional<String> dataIndexClientKeycloakAuthRealm;

    @Inject
    @ConfigProperty(name = DATA_INDEX_CLIENT_KEYCLOAK_AUTH_CLIENT_ID)
    Optional<String> dataIndexClientKeycloakAuthClientId;

    @Inject
    @ConfigProperty(name = DATA_INDEX_CLIENT_KEYCLOAK_AUTH_SECRET)
    Optional<String> dataIndexClientKeycloakAuthSecret;

    @Inject
    @ConfigProperty(name = DATA_INDEX_CLIENT_KEYCLOAK_AUTH_USER)
    Optional<String> dataIndexClientKeycloakAuthUser;

    @Inject
    @ConfigProperty(name = DATA_INDEX_CLIENT_KEYCLOAK_AUTH_PASSWORD)
    Optional<String> dataIndexClientKeycloakAuthPassword;

    public URL getDataIndexServerUrl() {
        return dataIndexServerUrl;
    }

    public Optional<String> getDataIndexClientBasicAuthUser() {
        return dataIndexClientBasicAuthUser;
    }

    public Optional<String> getDataIndexClientBasicAuthPassword() {
        return dataIndexClientBasicAuthPassword;
    }

    public Optional<URL> getDataIndexClientKeycloakAuthServerUrl() {
        return dataIndexClientKeycloakAuthServerUrl;
    }

    public Optional<String> getDataIndexClientKeycloakAuthRealm() {
        return dataIndexClientKeycloakAuthRealm;
    }

    public Optional<String> getDataIndexClientKeycloakAuthClientId() {
        return dataIndexClientKeycloakAuthClientId;
    }

    public Optional<String> getDataIndexClientKeycloakAuthSecret() {
        return dataIndexClientKeycloakAuthSecret;
    }

    public Optional<String> getDataIndexClientKeycloakAuthUser() {
        return dataIndexClientKeycloakAuthUser;
    }

    public Optional<String> getDataIndexClientKeycloakAuthPassword() {
        return dataIndexClientKeycloakAuthPassword;
    }

    public boolean isDataIndexKeycloakSet() {
        return isAnyOptionalSet(getDataIndexClientKeycloakAuthServerUrl(),
                                getDataIndexClientKeycloakAuthRealm(),
                                getDataIndexClientKeycloakAuthClientId(),
                                getDataIndexClientKeycloakAuthSecret(),
                                getDataIndexClientKeycloakAuthUser(),
                                getDataIndexClientKeycloakAuthPassword());
    }

    public boolean isDataIndexBasicAuthSet() {
        return isAnyOptionalSet(getDataIndexClientBasicAuthUser(),
                                getDataIndexClientBasicAuthPassword());
    }

    @Override
    public String toString() {
        return "TaskAssigningConfig{" +
                "dataIndexServerUrl=" + dataIndexServerUrl +
                ", dataIndexClientBasicAuthUser=" + dataIndexClientBasicAuthUser +
                ", dataIndexClientBasicAuthPassword=" + dataIndexClientBasicAuthPassword +
                ", dataIndexClientKeycloakAuthServerUrl=" + dataIndexClientKeycloakAuthServerUrl +
                ", dataIndexClientKeycloakAuthServerRealm=" + dataIndexClientKeycloakAuthRealm +
                ", dataIndexClientKeycloakAuthClientId=" + dataIndexClientKeycloakAuthClientId +
                ", dataIndexClientKeycloakAuthSecret=" + dataIndexClientKeycloakAuthSecret +
                ", dataIndexClientKeycloakAuthUser=" + dataIndexClientKeycloakAuthUser +
                ", dataIndexClientKeycloakAuthPassword=" + dataIndexClientKeycloakAuthPassword +
                '}';
    }

    private static boolean isAnyOptionalSet(Optional<?>... optionals) {
        return Stream.of(optionals).anyMatch(Optional::isPresent);
    }
}
