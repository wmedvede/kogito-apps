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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FaultToleranceSyncTests {

    public static int INVOCATIONS = 3;
    private static final Logger LOGGER = LoggerFactory.getLogger(FaultToleranceSyncTests.class);

    private int i = 0;

    @Retry(maxRetries = 5,
            delay = 3000,
            maxDuration = 1,
            durationUnit = ChronoUnit.HOURS)
    //@Fallback(fallbackMethod = "doEchoFallback")
    //@Fallback(value = FallbackHandlerTest.class)
    public String doEcho(String message) {
        if (i < INVOCATIONS) {
            LOGGER.debug(" Invocation: " + i++);
            throw new RuntimeException("Invocation : " + i + " always fail !");
        }
        return "Echo: " + message;
    }

    private String doEchoFallback(String message) {
        return "Return fallback value!!! " + message;
    }

    public static int FAILING_INVOCATIONS = 2;
    public static int INVOCATIONS_COUNT = 1;

    private static class EchoFallback implements FallbackHandler<CompletionStage<String>> {

        @Override
        public CompletionStage<String> handle(ExecutionContext context) {
            String message = (String) context.getParameters()[1];
            LOGGER.debug("Ejecutando el fallback para el metodo: " + context.getMethod().getName() + ", con message = " + message);
            LOGGER.debug("exception en el fallback: " + context.getFailure().getMessage());
            CompletableFuture<String> result = new CompletableFuture<>();
            result.complete("Fallback echo: " + message);
            return result;
        }
    }

    /**
     * Timeout:
     * In the asynchronous case the timeout is reset per each invocation. Each invocation is produced in a different
     * Thread.
     * 1) the invocation starts
     * 2) if the Timeout.value timeout is reached the invocation is "cancelled"
     * 3) after the Retry.delay value, a new invocation is started in a different Thread.
     * 4) if the last retry is being executed, i.e. the Retry.maxRetries has been reached, and the timout is elapsed
     * then:
     * 4.1) a TimoutException org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException:
     * Timeout[org.kie.kogito.taskassigning.service.test.FaultToleranceSyncTests#doEchoAsynchronous] timed out is produced.
     * And returned in the CompletionStage.exceptionally(...) result case.
     * 4.2) if the @Fallback is configured, then the execution control is transferred to it.
     *
     */
    @Asynchronous
    //    @Timeout(value = 2000,
    //            unit = ChronoUnit.MILLIS)
    @Retry(maxRetries = 5,
            delay = 4000)
    @Fallback(value = EchoFallback.class)
    public CompletionStage<String> doEchoAsynchronous(long delay, String message) {
        int count = INVOCATIONS_COUNT++;
        LOGGER.debug("Arrancamos la invocacion: " + count);

        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            LOGGER.debug("Me salta el interrupted exception en la invocacion: " + count);
            Thread.currentThread().interrupt();
        }
        if (count <= FAILING_INVOCATIONS) {
            future.completeExceptionally(new RuntimeException("Echo generated failure en la invocaion: " + count + ", " + message));
        } else {
            future.complete("Echo: en la invocation: " + count + ", " + message);
        }
        return future;
    }

}
