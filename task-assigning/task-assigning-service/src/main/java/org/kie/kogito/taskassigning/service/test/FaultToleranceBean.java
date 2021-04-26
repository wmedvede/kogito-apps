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

package org.kie.kogito.taskassigning.service.test;

import java.time.temporal.ChronoUnit;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FaultToleranceBean {

    public static int INVOCATIONS = 3;
    private static final Logger LOGGER = LoggerFactory.getLogger(FaultToleranceBean.class);

    private int i = 0;

    @Retry(maxRetries = 5,
            delay = 3000,
            maxDuration = 2,
            durationUnit = ChronoUnit.MILLIS)
    //@Fallback(fallbackMethod = "doEchoFallback")
    @Timeout(value = 5000)
    @Fallback(value = FallbackHandlerTest.class)
    public String doEcho(String message) {
        if (i < INVOCATIONS) {
            LOGGER.debug(" Invocation: " + i++);
            //            for (int i = 0; i >= 0;) {
            //                LOGGER.debug("INFONITE LOOP");
            //            }
            throw new RuntimeException("Invocation : " + i + " always fail !");
        }
        return "Echo: " + message;
    }

    private String doEchoFallback(String message) {
        return "Return fallback value!!! " + message;
    }
}
