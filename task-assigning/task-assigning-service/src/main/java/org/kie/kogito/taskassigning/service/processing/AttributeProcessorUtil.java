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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class AttributeProcessorUtil {

    private AttributeProcessorUtil() {
    }

    public static Set<Object> extractTokenizedValues(Object attributeValue, String separator) {
        if (attributeValue == null) {
            return Collections.emptySet();
        } else {
            return Arrays.stream(attributeValue.toString().split(separator))
                    .map(StringUtils::trim)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toSet());
        }
    }

}
