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

import java.lang.annotation.Annotation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ScopeUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScopeUtil.class);
    @Inject
    BeanManager beanManager;

    public boolean isScopeActive(String moment, Class<? extends Annotation> scopeType) {
        boolean active;
        try {
            if (beanManager.getContext(scopeType).isActive()) {
                active = true;
            } else {
                active = false;
            }
        } catch (final ContextNotActiveException e) {
            LOGGER.error("ERRRRRRRRRRRRROOR", e.getCause().getMessage());
            active = false;
        }
        LOGGER.debug(moment + " ------> " + scopeType + " is active = " + active);
        return active;
    }

}
