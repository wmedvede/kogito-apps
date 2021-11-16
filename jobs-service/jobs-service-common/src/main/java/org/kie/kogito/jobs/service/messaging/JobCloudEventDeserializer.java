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
package org.kie.kogito.jobs.service.messaging;

import java.io.IOException;
import java.util.Objects;

import org.apache.kafka.common.serialization.Deserializer;
import org.kie.kogito.event.CloudEventExtensionConstants;
import org.kie.kogito.jobs.api.Job;
import org.kie.kogito.jobs.api.event.CancelJobRequestEvent;
import org.kie.kogito.jobs.api.event.CreateProcessInstanceJobRequestEvent;
import org.kie.kogito.jobs.api.event.JobCloudEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.jackson.JsonFormat;

public class JobCloudEventDeserializer implements Deserializer<JobCloudEvent<?>> {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(JsonFormat.getCloudEventJacksonModule());

    @Override
    public JobCloudEvent<?> deserialize(String topic, byte[] data) {
        try {
            CloudEvent cloudEvent = OBJECT_MAPPER.readValue(data, CloudEvent.class);
            CloudEventData cloudEventData = Objects.requireNonNull(cloudEvent.getData(), "JobCloudEvent data field must not be null");
            if (cloudEvent.getType().equals(CreateProcessInstanceJobRequestEvent.CREATE_PROCESS_INSTANCE_JOB_REQUEST)) {
                Job job = OBJECT_MAPPER.readValue(cloudEventData.toBytes(), Job.class);
                return new CreateProcessInstanceJobRequestEvent(cloudEvent.getSource(),
                        job,
                        getExtensionAsString(cloudEvent, CloudEventExtensionConstants.PROCESS_INSTANCE_ID),
                        getExtensionAsString(cloudEvent, CloudEventExtensionConstants.PROCESS_ID),
                        getExtensionAsString(cloudEvent, CloudEventExtensionConstants.PROCESS_ROOT_PROCESS_INSTANCE_ID),
                        getExtensionAsString(cloudEvent, CloudEventExtensionConstants.PROCESS_ROOT_PROCESS_ID),
                        getExtensionAsString(cloudEvent, CloudEventExtensionConstants.ADDONS));
            } else if (cloudEvent.getType().equals(CancelJobRequestEvent.CANCEL_JOB_REQUEST)) {
                CancelJobRequestEvent.JobId jobId = OBJECT_MAPPER.readValue(cloudEventData.toBytes(), CancelJobRequestEvent.JobId.class);
                return new CancelJobRequestEvent(cloudEvent.getSource(),
                        jobId.getId(),
                        getExtensionAsString(cloudEvent, CloudEventExtensionConstants.PROCESS_INSTANCE_ID),
                        getExtensionAsString(cloudEvent, CloudEventExtensionConstants.PROCESS_ID),
                        getExtensionAsString(cloudEvent, CloudEventExtensionConstants.PROCESS_ROOT_PROCESS_INSTANCE_ID),
                        getExtensionAsString(cloudEvent, CloudEventExtensionConstants.PROCESS_ROOT_PROCESS_ID),
                        getExtensionAsString(cloudEvent, CloudEventExtensionConstants.ADDONS));
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getExtensionAsString(CloudEvent cloudEvent, String extensionName) {
        return Objects.toString(cloudEvent.getExtension(extensionName));
    }
}
