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

package org.kie.kogito.taskassigning.service;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.kie.kogito.taskassigning.core.model.Task;

class PlanningExecutorTest {

    @Test
    void dale() throws Exception {
        String dale = "http://myapplication.cloud.com:8280/CheckConcurrency/d698c37b-679f-4c90-890b-270971328bcb/TaskA/c489f148-6526-4934-879f-1d18281434e2";
        Task task = Task.newBuilder()
                .processId("CheckConcurrency")
                .processInstanceId("d698c37b-679f-4c90-890b-270971328bcb")
                .endpoint(dale)
                .build();

       // PlanningExecutor planningExecutor = new PlanningExecutor(null);
        //URL resultado = PlanningExecutor.buildServiceUrl(task);

        int i = 0;
    }

}
