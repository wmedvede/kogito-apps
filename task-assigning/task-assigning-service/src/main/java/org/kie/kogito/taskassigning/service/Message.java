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

package org.kie.kogito.taskassigning.service;

import java.time.ZonedDateTime;

public class Message {

    public enum Severity {
        INFO,
        WARN,
        ERROR
    }

    private Severity severity;
    private ZonedDateTime timestamp;
    private String value;

    private Message(Severity severity, ZonedDateTime timestamp, String value) {
        this.severity = severity;
        this.timestamp = timestamp;
        this.value = value;
    }

    public Severity getSeverity() {
        return severity;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public String getValue() {
        return value;
    }

    public static Message info(String value) {
        return new Message(Severity.INFO, ZonedDateTime.now(), value);
    }

    public static Message warn(String value) {
        return new Message(Severity.WARN, ZonedDateTime.now(), value);
    }

    public static Message error(String value) {
        return new Message(Severity.ERROR, ZonedDateTime.now(), value);
    }
}
