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

package org.kie.kogito.taskassigning.service.processing;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kie.kogito.taskassigning.core.model.DefaultLabels;
import org.kie.kogito.taskassigning.model.processing.TaskAttributesProcessor;
import org.kie.kogito.taskassigning.model.processing.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.TASK_ASSIGNING_PROPERTY_PREFIX;
import static org.kie.kogito.taskassigning.service.processing.AttributeProcessorUtil.extractTokenizedValues;

@ApplicationScoped
public class DefaultTaskAttributesProcessor implements TaskAttributesProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTaskAttributesProcessor.class);

    private static final String SEPARATOR = ",";

    private static final String PROCESSOR_PREFIX = TASK_ASSIGNING_PROPERTY_PREFIX + ".default-task-attributes-processor";

    private static final String PROCESSOR_ENABLED_PROPERTY_NAME = PROCESSOR_PREFIX + ".enabled";

    private static final String TASK_SKILLS_ATTRIBUTE_PROPERTY_NAME = PROCESSOR_PREFIX + ".skills";

    private static final String TASK_AFFINITIES_ATTRIBUTE_PROPERTY_NAME = PROCESSOR_PREFIX + "affinities";

    @Inject
    @ConfigProperty(name = PROCESSOR_ENABLED_PROPERTY_NAME, defaultValue = "true")
    boolean enabled;

    @Inject
    @ConfigProperty(name = TASK_SKILLS_ATTRIBUTE_PROPERTY_NAME, defaultValue = "skills")
    String skillsAttribute;

    @Inject
    @ConfigProperty(name = TASK_AFFINITIES_ATTRIBUTE_PROPERTY_NAME, defaultValue = "affinities")
    String affinitiesAttribute;

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void process(TaskInfo entity, Map<String, Object> targetAttributes) {
        if (enabled) {
            LOGGER.debug("Executing {} for task: {}", getClass().getName(), entity.getTaskId());
            targetAttributes.put(DefaultLabels.SKILLS.name(),
                    extractTokenizedValues(entity.getInputs().get(skillsAttribute), SEPARATOR));
            targetAttributes.put(DefaultLabels.AFFINITIES.name(),
                    extractTokenizedValues(entity.getInputs().get(affinitiesAttribute), SEPARATOR));
        }
    }
}
