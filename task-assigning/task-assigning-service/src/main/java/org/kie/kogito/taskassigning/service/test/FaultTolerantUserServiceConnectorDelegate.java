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

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.kie.kogito.taskassigning.user.service.User;
import org.kie.kogito.taskassigning.user.service.UserServiceConnector;

@ApplicationScoped
public class FaultTolerantUserServiceConnectorDelegate {

    public static class Result {
        private User user;
        private Throwable failure;

        public Result(User user, Throwable failure) {
            this.user = user;
            this.failure = failure;
        }
    }

    private static class FallbackHandler implements org.eclipse.microprofile.faulttolerance.FallbackHandler<Result> {
        @Override
        public Result handle(ExecutionContext context) {
            return new Result(null, context.getFailure());
        }
    }

    @Retry(maxRetries = 5,
            delay = 3000)
    @Fallback(value = FallbackHandler.class)
    public Result findUser(UserServiceConnector connector, String id) {
        return new Result(connector.findUser(id), null);
    }

}
