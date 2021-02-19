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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.kie.kogito.taskassigning.core.model.Task;
import org.kie.kogito.taskassigning.core.model.TaskAssigningSolution;
import org.kie.kogito.taskassigning.core.model.TaskAssignment;
import org.kie.kogito.taskassigning.core.model.User;
import org.kie.kogito.taskassigning.core.model.solver.realtime.AddTaskProblemFactChange;
import org.kie.kogito.taskassigning.core.model.solver.realtime.AssignTaskProblemFactChange;
import org.kie.kogito.taskassigning.core.model.solver.realtime.ReleaseTaskProblemFactChange;
import org.kie.kogito.taskassigning.core.model.solver.realtime.RemoveTaskProblemFactChange;
import org.kie.kogito.taskassigning.service.messaging.UserTaskEvent;
import org.kie.kogito.taskassigning.user.service.api.UserServiceConnector;
import org.optaplanner.core.api.solver.ProblemFactChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.kogito.taskassigning.service.util.EventUtil.filterNewestEventsMap;
import static org.kie.kogito.taskassigning.service.util.UserUtil.fromExternalUser;

public class SolutionChangesBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolutionChangesBuilder.class);

    private TaskAssigningServiceContext context;
    private TaskAssigningSolution solution;
    private UserServiceConnector userServiceConnector;
    private List<UserTaskEvent> userTaskEvents;

    List<ProblemFactChange<TaskAssigningSolution>> buildAfterPlanningExecutionChanges(PlanningExecutionResult executionResult) {
        Map<String, UserTaskEvent> inParallelEvents = filterNewestEventsMap(userTaskEvents);
        Map<String, User> usersById = solution.getUserList()
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<ProblemFactChange<TaskAssigningSolution>> calculatedChanges = new ArrayList<>();
        List<RemoveTaskProblemFactChange> removeTaskChanges = new ArrayList<>();
        List<AssignTaskProblemFactChange> assignTaskChanges = new ArrayList<>();
        List<ReleaseTaskProblemFactChange> releaseTaskChanges = new ArrayList<>();

        Task resultItemTask;
        ZonedDateTime resultItemTaskLastUpdate;

        for (PlanningExecutionResultItem resultItem : executionResult.getItems()) {
            resultItemTask = resultItem.getItem().getTask();
            resultItemTaskLastUpdate = context.getTaskChangeTime(resultItemTask.getId());
            //TODO OJO me falta procesar el caso donde una tarea viene modificada... por ejemplo la prioridad...
            if (!resultItem.hasError()) {
                UserTaskEvent inParallelEvent = inParallelEvents.remove(resultItemTask.getId());
                if (inParallelEvent != null && inParallelEvent.getLastUpdate().isAfter(resultItemTaskLastUpdate)) {
                    if (TaskStatus.isTerminal(inParallelEvent.getState())) {
                        // task was finalized in parallel
                        removeTaskChanges.add(new RemoveTaskProblemFactChange(new TaskAssignment(resultItemTask)));
                    } else if (TaskStatus.RESERVED.value().equals(inParallelEvent.getState())) {
                        // normally the event corresponding to the just produced assignment....
                        //OJO acá en realidad casi q tengo que crear una nueva task a partir de la que llego en el UserData
                        //y NO la del resultItemTask....
                        // task reserved in parallel, normally the event corresponding to the just produced assignment
                        User user = getUser(usersById, inParallelEvent.getActualOwner());
                        assignTaskChanges.add(new AssignTaskProblemFactChange(new TaskAssignment(resultItemTask), user));
                    } else {
                        // the task was released

                    }
                    //OJO la tarea puede tabien haber sido modificda.... con la nueva api
                } else {

                }
            } else {
                //podria haber legado justo el evento q soluciona el error....!!!!!

            }
        }




            /*
            0) get the events that arrived in the middle
            1) Using this information and the results from the execution
                Prepare a new set of problem fact changes
                    SUCESSFULL_CHANGES:
                    1) if the execution of an assignment was successful and no new event
                           that changes this situation arrived in the middle for this event
                           has arrived
                           Then create Pin(Task, "user") for it

                       if the execution was successfull BUT we received and event for this task that
                       might change this situation. e.g. the task was quickly completed
                       Then generate the proper change instead....
                       Maybe instead of pinning the task we must simple remove it, or even assign to someone else....

                    FAILING_CHANGES:
                        if the execution of a task failed and no new event was received
                            All right, create the proper RemoveTask(Task) PFC
                            and start to monitor this task....


                        if the execution of the task has changed BUT we received an event that might
                        change this situation, create the proper PFC
                            e.g. the task was completed in the middle and this is why it failed.
                            All right, it's ok to remove the task BUT don't monitor it!
                            Or the task was assigned to someone else in the middle, all right
                            instead of removing it program the add to the new user.

                    Finally we could have more events that are not necessary related with the tasks
                    that were part of the plan, well, use lifecycle to process them.

                    When we have this new list of changes execute them please....



            */

        return calculatedChanges;
    }

    //TODO must never fail....
    private User getUser(Map<String, User> usersById, String userId) {
        User user = usersById.get(userId);
        if (user == null) {
            LOGGER.debug("User {} was not found in current solution, it'll we looked up in the external user system .", userId);
            org.kie.kogito.taskassigning.user.service.api.User externalUser = userServiceConnector.findUser(userId);
            if (externalUser != null) {
                user = fromExternalUser(externalUser);
            } else {
                // We add it by convention, since the task list administration supports the delegation to non-existent users.
                LOGGER.debug("User {} was not found in the external user system, it looks like it's a manual" +
                                     " assignment from the tasks administration. It'll be added to the solution" +
                                     " to respect the assignment.", userId);
                user = new User(userId);
            }
        }
        return user;
    }
}
