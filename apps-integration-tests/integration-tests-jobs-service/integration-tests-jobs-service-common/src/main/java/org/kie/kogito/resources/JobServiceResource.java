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

package org.kie.kogito.resources;

import java.util.HashMap;
import java.util.Map;

import org.kie.kogito.test.resources.TestResource;
import org.kie.kogito.testcontainers.JobServiceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobServiceResource implements TestResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobServiceResource.class);

    private final JobServiceContainer jobService = new JobServiceContainer();
    private final Map<String, String> properties = new HashMap<>();

    @Override
    public String getResourceName() {
        return jobService.getResourceName();
    }

    @Override
    public void start() {
        LOGGER.info("Start JobService test resource");
        properties.clear();
        jobService.start();
        LOGGER.info("JobService test resource started");
    }

    @Override
    public void stop() {
        LOGGER.info("Stop JobService test resource");
        jobService.stop();
        LOGGER.info("JobService test resource stopped");
    }

    @Override
    public int getMappedPort() {
        return jobService.getMappedPort();
    }

    public Map<String, String> getProperties() {
        return properties;
    }

}
