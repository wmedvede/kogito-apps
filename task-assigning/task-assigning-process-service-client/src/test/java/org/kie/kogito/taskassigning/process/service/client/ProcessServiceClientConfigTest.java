/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

package org.kie.kogito.taskassigning.process.service.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessServiceClientConfigTest {

    private static final String SERVICE_URL = "SERVICE_URL";
    private static final long CONNECT_TIMEOUT = 1;
    private static final long READ_TIMOUT = 2;
    private static final String NEW_STRING_VALUE = "NEW_STRING_VALUE";
    private static final long NEW_LONG_VALUE = 0;

    private ProcessServiceClientConfig config;

    @BeforeEach
    public void setUp() {
        config = ProcessServiceClientConfig.newBuilder()
                .serviceUrl(SERVICE_URL)
                .connectTimeoutMillis(CONNECT_TIMEOUT)
                .readTimeoutMillis(READ_TIMOUT).build();
    }

    @Test
    public void getServiceURL() {
        assertThat(config.getServiceUrl()).isEqualTo(SERVICE_URL);
    }

    @Test
    public void setServiceURL() {
        config.setServiceUrl(NEW_STRING_VALUE);
        assertThat(config.getServiceUrl()).isEqualTo(NEW_STRING_VALUE);
    }

    @Test
    public void getConnectTimoutMillis() {
        assertThat(config.getConnectTimeoutMillis()).isEqualTo(CONNECT_TIMEOUT);
    }

    @Test
    public void setConnectTimoutMillis() {
        config.setConnectTimeoutMillis(NEW_LONG_VALUE);
        assertThat(config.getConnectTimeoutMillis()).isEqualTo(NEW_LONG_VALUE);
    }

    @Test
    public void getReadTimoutMillis() {
        assertThat(config.getReadTimeoutMillis()).isEqualTo(READ_TIMOUT);
    }

    @Test
    public void setReadTimoutMillis() {
        config.setReadTimeoutMillis(NEW_LONG_VALUE);
        assertThat(config.getReadTimeoutMillis()).isEqualTo(NEW_LONG_VALUE);
    }
}
