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
import org.kie.kogito.testcontainers.KogitoKafkaContainerWithoutBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class JobServiceKafkaResource implements TestResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobServiceKafkaResource.class);

    private final KogitoKafkaContainerWithoutBridge kafka = new KogitoKafkaContainerWithoutBridge();
    private final JobServiceContainer jobService = new JobServiceContainer();
    private final Map<String, String> properties = new HashMap<>();

    @Override
    public String getResourceName() {
        return jobService.getResourceName();
    }

    @Override
    public void start() {
        LOGGER.info("Start JobServiceKafka test resource");
        properties.clear();
        LOGGER.info("Start Kafka");
        Network network = Network.newNetwork();
        kafka.withNetwork(network);
        kafka.withNetworkAliases("kafka");
        kafka.waitingFor(Wait.forListeningPort());
        kafka.start();
        // external access url
        String kafkaURL = kafka.getBootstrapServers();
        LOGGER.info("kafkaURL: {}", kafka.getBootstrapServers());
        // internal access url
        String kafkaInternalUrl = "kafka:9092";
        properties.put("kafka.bootstrap.servers", kafkaURL);
        properties.put("spring.kafka.bootstrap-servers", kafkaURL);
        properties.put("quarkus.profile", "events-support");

        jobService.withNetwork(network);
        jobService.setKafkaURL(kafkaInternalUrl);
        jobService.setQuarkusProfile("events-support");
        jobService.start();
        LOGGER.info("JobServiceKafka test resource started");
    }

    @Override
    public void stop() {
        LOGGER.info("Stop JobServiceKafka test resource");
        jobService.stop();
        kafka.stop();
        LOGGER.info("JobServiceKafka test resource stopped");
    }

    @Override
    public int getMappedPort() {
        return jobService.getMappedPort();
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
