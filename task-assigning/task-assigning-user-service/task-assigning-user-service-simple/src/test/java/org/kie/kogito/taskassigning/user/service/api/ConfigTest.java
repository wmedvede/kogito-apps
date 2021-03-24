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

package org.kie.kogito.taskassigning.user.service.api;

import java.util.List;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ConfigTest {

    private static final String USER_DEF_PREFIX = "simple.user.service";

    @Test
    void readConfig() {

        Config config = ConfigProvider.getConfig();

        List<String> userDefsLines;

        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(USER_DEF_PREFIX)) {

            }
        }

        //config.getPropertyNames()
    }

    private static class ElementLine {

        private String elementId;
        private List<String> values;

        public ElementLine(String elementId, List<String> values) {
            this.elementId = elementId;
            this.values = values;
        }

        public String getElementId() {
            return elementId;
        }

        public List<String> getValues() {
            return values;
        }
    }

}
