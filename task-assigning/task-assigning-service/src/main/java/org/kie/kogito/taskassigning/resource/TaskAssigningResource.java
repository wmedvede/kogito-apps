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

package org.kie.kogito.taskassigning.resource;

import java.util.ArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.kie.kogito.taskassigning.core.model.Task;
import org.kie.kogito.taskassigning.core.model.TaskAssigningSolution;
import org.kie.kogito.taskassigning.core.model.TaskAssignment;
import org.kie.kogito.taskassigning.messaging.MyKafkaConsumerRebalanceListener;
import org.kie.kogito.taskassigning.service.TaskAssigningService;
import org.optaplanner.core.api.solver.SolverFactory;

import static org.kie.kogito.taskassigning.core.model.ModelConstants.PLANNING_USER;

@Path("/task-assigning")
@ApplicationScoped
public class TaskAssigningResource {

    @Inject
    SolverFactory<TaskAssigningSolution> solverFactory;

    @Inject
    TaskAssigningService service;

    @Inject
    @RestClient
    CheckConcurrencyClient checkConcurrencyClient;

    @Inject
    MyKafkaConsumerRebalanceListener listener;

    @GET
    @Path("/executeSolver")
    @Produces({"application/json"})
    public String executeSolver() {
        System.out.println("Starting solver execution");

        TaskAssigningSolution solution = new TaskAssigningSolution("1", new ArrayList<>(), new ArrayList<>());
        solution.getUserList().add(PLANNING_USER);
        solution.getTaskAssignmentList().add(new TaskAssignment(Task.newBuilder().id("Task1").build()));
        solution.getTaskAssignmentList().add(new TaskAssignment(Task.newBuilder().id("Task2").build()));
        TaskAssigningSolution result = solverFactory.buildSolver().solve(solution);
        System.out.println("All Good!");
        return "{\"result\": \"Success!\"}";
    }

    @POST
    @Path("/startEvents")
    @Produces({"application/json"})
    public String startEvents() {
        service.startEvents();
        //listener.resume();
        return "{\"result\": \"Events Started!\"}";
    }

    @POST
    @Path("/pauseEvents")
    @Produces({"application/json"})
    public String pauseEvents() {
        service.pauseEvents();
        //listener.pause();
        return "{\"result\": \"Events Paused!\"}";
    }

    @POST
    @Path("/createProcessInstances")
    @Produces({"application/json"})
    public void createProcessInstances(@QueryParam("instances") int instances) {
        for (int i= 0; i < instances; i++) {

            checkConcurrencyClient.newProcessInstance("process_" + System.currentTimeMillis(),
                                                      "{}");

        }

    }
}
