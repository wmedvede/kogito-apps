/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.taskassigning.service;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.runtime.Startup;
import org.kie.kogito.taskassigning.core.model.TaskAssigningSolution;
import org.optaplanner.core.api.solver.SolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Startup
public class TaskAssigningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskAssigningService.class);

    @Inject
    SolverFactory<TaskAssigningSolution> solverFactory;

    @PostConstruct
    void start() {
        LOGGER.error("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        checkSolverConfig();

    }

    void checkSolverConfig() {
        LOGGER.debug("Checking solver configuration!");
        solverFactory.buildSolver();
    }

}
