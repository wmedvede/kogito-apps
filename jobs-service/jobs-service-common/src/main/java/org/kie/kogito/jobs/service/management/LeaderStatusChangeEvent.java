/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.jobs.service.management;

import javax.interceptor.Interceptor;

public class LeaderStatusChangeEvent {

    /**
     * LeaderStatusChangeEvent interceptors with a very short processing time, and thus it's convenient to invoke
     * them ASAP.
     */
    public static final int LEADER_STATUS_CHANGE_INTERCEPTOR_HIGH_PRIORITY = Interceptor.Priority.APPLICATION;

    /**
     * LeaderStatusChangeEvent interceptors with a longer processing time, and thus it has not effects on the
     * overall processing if we consider to execute them later.
     */
    public static final int LEADER_STATUS_CHANGE_INTERCEPTOR_LOW_PRIORITY = Interceptor.Priority.APPLICATION + 1;

    private final boolean leader;

    private LeaderStatusChangeEvent(boolean leader) {
        this.leader = leader;
    }

    public static LeaderStatusChangeEvent leaderOn() {
        return new LeaderStatusChangeEvent(true);
    }

    public static LeaderStatusChangeEvent leaderOff() {
        return new LeaderStatusChangeEvent(false);
    }

    public boolean isLeader() {
        return leader;
    }
}
