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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.kie.kogito.taskassigning.core.model.solver.realtime.TaskPriorityChangeProblemFactChange;
import org.kie.kogito.taskassigning.core.model.solver.realtime.TaskStateChangeProblemFactChange;
import org.kie.kogito.taskassigning.service.messaging.UserTaskEvent;
import org.kie.kogito.taskassigning.service.util.IndexedElement;
import org.kie.kogito.taskassigning.user.service.api.UserServiceConnector;
import org.optaplanner.core.api.solver.ProblemFactChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.kogito.taskassigning.core.model.ModelConstants.DUMMY_TASK_ASSIGNMENT_PLANNER_241;
import static org.kie.kogito.taskassigning.core.model.ModelConstants.IS_NOT_DUMMY_TASK_ASSIGNMENT;
import static org.kie.kogito.taskassigning.core.model.ModelConstants.PLANNING_USER;
import static org.kie.kogito.taskassigning.service.util.EventUtil.filterNewestEventsMap;
import static org.kie.kogito.taskassigning.service.util.IndexedElement.addInOrder;
import static org.kie.kogito.taskassigning.service.util.TaskUtil.fromUserTaskEvent;
import static org.kie.kogito.taskassigning.service.util.UserUtil.fromExternalUser;

public class SolutionChangesBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolutionChangesBuilder.class);

    private TaskAssigningServiceContext context;
    private UserServiceConnector userServiceConnector;
    private TaskAssigningSolution solution;
    private List<UserTaskEvent> userTaskEvents;

    private SolutionChangesBuilder() {
    }

    public static SolutionChangesBuilder create() {
        return new SolutionChangesBuilder();
    }

    public SolutionChangesBuilder withContext(TaskAssigningServiceContext context) {
        this.context = context;
        return this;
    }

    public SolutionChangesBuilder withUserServiceConnector(UserServiceConnector userServiceConnector) {
        this.userServiceConnector = userServiceConnector;
        return this;
    }

    public SolutionChangesBuilder forSolution(TaskAssigningSolution solution) {
        this.solution = solution;
        return this;
    }

    public SolutionChangesBuilder fromUserTaskEvents(List<UserTaskEvent> userTaskEvents) {
        this.userTaskEvents = userTaskEvents;
        return this;
    }

    public List<ProblemFactChange<TaskAssigningSolution>> build() {
        Map<String, User> usersById = solution.getUserList()
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<String, TaskAssignment> taskAssignmentById = solution.getTaskAssignmentList()
                .stream()
                .filter(IS_NOT_DUMMY_TASK_ASSIGNMENT)
                .collect(Collectors.toMap(TaskAssignment::getId, Function.identity()));

        Map<String, UserTaskEvent> filteredEvents = filterNewestEventsMap(userTaskEvents);

        List<AddTaskProblemFactChange> newTaskChanges = new ArrayList<>();
        List<RemoveTaskProblemFactChange> removedTaskChanges = new ArrayList<>();
        Set<TaskAssignment> removedTasksSet = new HashSet<>();
        List<ReleaseTaskProblemFactChange> releasedTasksChanges = new ArrayList<>();
        Map<String, List<IndexedElement<AssignTaskProblemFactChange>>> assignToUserChangesByUserId = new HashMap<>();
        List<ProblemFactChange<TaskAssigningSolution>> propertyChanges = new ArrayList<>();

        TaskAssignment taskAssignment;
        for (UserTaskEvent taskEvent : filteredEvents.values()) {
            taskAssignment = taskAssignmentById.remove(taskEvent.getId());
            if (taskAssignment == null) {
                addNewTaskChanges(taskEvent, usersById, newTaskChanges, assignToUserChangesByUserId);
            } else {
                addTaskChanges(taskAssignment, taskEvent, usersById, releasedTasksChanges, removedTasksSet,
                               propertyChanges, assignToUserChangesByUserId);
            }
        }

        for (TaskAssignment removedTask : removedTasksSet) {
            removedTaskChanges.add(new RemoveTaskProblemFactChange(removedTask));
        }

        List<ProblemFactChange<TaskAssigningSolution>> totalChanges = new ArrayList<>();

        // TODO totalChanges.addAll(newUserChanges);
        totalChanges.addAll(removedTaskChanges);
        totalChanges.addAll(releasedTasksChanges);
        assignToUserChangesByUserId.values().forEach(byUserChanges -> byUserChanges.forEach(change -> totalChanges.add(change.getElement())));
        totalChanges.addAll(propertyChanges);
        // TODO totalChanges.addAll(userUpdateChanges);
        totalChanges.addAll(newTaskChanges);
        //TODO totalChanges.addAll(removableUserChanges);

        applyWorkaroundForPLANNER241(solution, totalChanges);

        if (!totalChanges.isEmpty()) {
            totalChanges.add(0, scoreDirector -> context.setCurrentChangeSetId(context.nextChangeSetId()));
        }
        return totalChanges;
    }

    private void addNewTaskChanges(UserTaskEvent taskEvent,
                                   Map<String, User> usersById,
                                   List<AddTaskProblemFactChange> newTasksChanges,
                                   Map<String, List<IndexedElement<AssignTaskProblemFactChange>>> assignToUserChangesByUserId) {
        Task newTask;
        if (TaskStatus.READY.value().equals(taskEvent.getState())) {
            newTask = fromUserTaskEvent(taskEvent);
            newTasksChanges.add(new AddTaskProblemFactChange(new TaskAssignment(newTask)));
        } else if (TaskStatus.RESERVED.value().equals(taskEvent.getState())) {
            newTask = fromUserTaskEvent(taskEvent);
            final User user = getUser(usersById, taskEvent.getActualOwner());
            AssignTaskProblemFactChange change = new AssignTaskProblemFactChange(new TaskAssignment(newTask), user, true);
            context.setTaskPublished(taskEvent.getId(), true);
            addChangeToUser(assignToUserChangesByUserId, change, user, -1, true);
        }
    }

    private static void addChangeToUser(Map<String, List<IndexedElement<AssignTaskProblemFactChange>>> changesByUserId,
                                        AssignTaskProblemFactChange change,
                                        User user,
                                        int index,
                                        boolean pinned) {
        final List<IndexedElement<AssignTaskProblemFactChange>> userChanges = changesByUserId.computeIfAbsent(user.getId(), key -> new ArrayList<>());
        addInOrder(userChanges, new IndexedElement<>(change, index, pinned));
    }

    private void addTaskChanges(TaskAssignment taskAssignment,
                                UserTaskEvent taskEvent,
                                Map<String, User> usersById,
                                List<ReleaseTaskProblemFactChange> releasedTasksChanges,
                                Set<TaskAssignment> removedTasksSet,
                                List<ProblemFactChange<TaskAssigningSolution>> propertyChanges,
                                Map<String, List<IndexedElement<AssignTaskProblemFactChange>>> changesByUserId) {
        String taskState = taskEvent.getState();
        if (TaskStatus.READY.value().equals(taskState)) {
            context.setTaskPublished(taskEvent.getId(), false);
            if (!TaskStatus.READY.value().equals(taskAssignment.getTask().getState())) {
                //TODO ver si tiene sentido este caso
                // task was probably assigned to someone else in the past and released from the task
                // list administration
                releasedTasksChanges.add(new ReleaseTaskProblemFactChange(taskAssignment));
            }
        } else if (TaskStatus.RESERVED.value().equals(taskState)) {
            context.setTaskPublished(taskEvent.getId(), true);
            if (!taskEvent.getActualOwner().equals(taskAssignment.getUser().getId())) {
                // if Reserved:
                //       the task was probably manually re-assigned from the task list to another user.
                //       We must respect this assignment.

                final User user = getUser(usersById, taskEvent.getActualOwner());

                // assign and ensure the task is published since the task was already seen by the public audience.
                AssignTaskProblemFactChange change = new AssignTaskProblemFactChange(taskAssignment, user, true);
                addChangeToUser(changesByUserId, change, user, -1, true);
            }
        } else if (TaskStatus.isTerminal(taskState)) {
            context.clearTaskContext(taskEvent.getId());
            removedTasksSet.add(taskAssignment);
        }

        //TODO agregar el tema de ver otras cosas que puedan cambiar de valor priority, status...., input parameters.
        if (!removedTasksSet.contains(taskAssignment)) {
            if (!Objects.equals(taskAssignment.getTask().getPriority(), taskEvent.getPriority())) {
                propertyChanges.add(new TaskPriorityChangeProblemFactChange(taskAssignment, taskEvent.getPriority()));
            }
            if (!Objects.equals(taskAssignment.getTask().getState(), taskEvent.getState())) {
                propertyChanges.add(new TaskStateChangeProblemFactChange(taskAssignment, taskEvent.getState()));
            }
        }
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

    //TODO remove this code
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

    /**
     * This method adds a second dummy task for avoiding the issue produced by https://issues.jboss.org/browse/PLANNER-241
     * and will be removed as soon it's fixed. Note that workaround doesn't have a huge impact on the solution since
     * the dummy task is added only once and to the planning user.
     */
    private void applyWorkaroundForPLANNER241(TaskAssigningSolution solution, List<ProblemFactChange<TaskAssigningSolution>> changes) {
        boolean hasDummyTask241 = solution.getTaskAssignmentList().stream().anyMatch(taskAssignment -> DUMMY_TASK_ASSIGNMENT_PLANNER_241.getId().equals(taskAssignment.getId()));
        if (!hasDummyTask241) {
            changes.add(new AssignTaskProblemFactChange(DUMMY_TASK_ASSIGNMENT_PLANNER_241, PLANNING_USER));
        }
    }
}
