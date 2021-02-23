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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.kie.kogito.taskassigning.ClientServices;
import org.kie.kogito.taskassigning.core.model.Task;
import org.kie.kogito.taskassigning.core.model.TaskAssigningSolution;
import org.kie.kogito.taskassigning.core.model.TaskAssignment;
import org.kie.kogito.taskassigning.core.model.User;
import org.kie.kogito.taskassigning.core.model.solver.realtime.AssignTaskProblemFactChange;
import org.kie.kogito.taskassigning.service.config.TaskAssigningConfig;
import org.kie.kogito.taskassigning.service.config.TaskAssigningConfigValidator;
import org.kie.kogito.taskassigning.service.messaging.BufferedUserTaskEventConsumer;
import org.kie.kogito.taskassigning.service.messaging.UserTaskEvent;
import org.kie.kogito.taskassigning.service.messaging.UserTaskEventConsumer;
import org.kie.kogito.taskassigning.user.service.api.UserServiceConnector;
import org.optaplanner.core.api.solver.ProblemFactChange;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.event.BestSolutionChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class in experimental status! don't waste time here!
 * The only objective by now is to be sure the tasks can be consumed and the solver started.
 */
@ApplicationScoped
@Startup
public class TaskAssigningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskAssigningService.class);

    //TODO parametrize this value
    private static final int PUBLISH_WINDOW_SIZE = 2;

    @Inject
    SolverFactory<TaskAssigningSolution> solverFactory;

    @Inject
    TaskAssigningConfig config;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    TaskServiceConnector taskServiceConnector;

    @Inject
    UserServiceConnector userServiceConnector;

    @Inject
    UserTaskEventConsumer userTaskEventConsumer;

    @Inject
    ClientServices clientServices;

    private SolverExecutor solverExecutor;

    private SolutionDataLoader solutionDataLoader;

    private PlanningExecutor planningExecutor;

    private TaskAssigningServiceContext context;

    private AtomicReference<TaskAssigningSolution> currentSolution = new AtomicReference<>(null);

    private AtomicReference<TaskAssigningSolution> lastBestSolution = new AtomicReference<>(null);

    private AtomicReference<Boolean> applyingPlanningExecutionResult = new AtomicReference<>(false);

    /**
     * Synchronizes potential concurrent accesses between the different components that invoke callbacks on the service.
     */
    private final ReentrantLock lock = new ReentrantLock();

    AtomicReference<String> serviceStatus = new AtomicReference<>("Stopped");

    int totalChances = 1;

    @PostConstruct
    void start() {
        serviceStatus.set("Starting");
        startUpValidation();
        context = new TaskAssigningServiceContext();
        //TODO revisar este cast
        ((BufferedUserTaskEventConsumer) userTaskEventConsumer).setConsumer(this::onTaskEvents);
        solverExecutor = new SolverExecutor(solverFactory, this::onBestSolutionChange);
        managedExecutor.execute(solverExecutor);
        planningExecutor = new PlanningExecutor(clientServices, config);
        managedExecutor.execute(planningExecutor);
        solutionDataLoader = new SolutionDataLoader(taskServiceConnector,
                                                    userServiceConnector,
                                                    Duration.ofMillis(5000));
        managedExecutor.execute(solutionDataLoader);
        solutionDataLoader.start(this::onSolutionDataLoad, 1);
    }

    private void pauseEvents() {
        userTaskEventConsumer.pause();
    }

    private void resumeEvents() {
        userTaskEventConsumer.resume();
    }

    private List<UserTaskEvent> pollEvents() {
        return userTaskEventConsumer.pollEvents();
    }

    private void onTaskEvents(List<UserTaskEvent> events) {
        lock.lock();
        try {
            pauseEvents();
            if (currentSolution.get() == null) {
                //we are at the very beginning, build the solution from the events.!!!!
                solutionDataLoader.start(this::onSolutionDataLoad, 1);

            } else {
                List<ProblemFactChange<TaskAssigningSolution>> changes = SolutionChangesBuilder.create()
                        .forSolution(currentSolution.get())
                        .withContext(context)
                        .withUserServiceConnector(userServiceConnector)
                        .fromUserTaskEvents(events)
                        .build();
                if (!changes.isEmpty()) {
                    solverExecutor.addProblemFactChanges(changes);
                } else {
                    resumeEvents();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Invoked when the solver produces a new solution.
     * @param event event produced by the solver.
     */
    private void onBestSolutionChange(BestSolutionChangedEvent<TaskAssigningSolution> event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("onBestSolutionChange: isEveryProblemFactChangeProcessed: {}, currentChangeSetId: {}," +
                                 " isCurrentChangeSetProcessed: {}, newBestSolution: {}",
                         event.isEveryProblemFactChangeProcessed(), context.getCurrentChangeSetId(),
                         context.isCurrentChangeSetProcessed(), event.getNewBestSolution());
        }

        TaskAssigningSolution newBestSolution = event.getNewBestSolution();
        if (event.isEveryProblemFactChangeProcessed() && newBestSolution.getScore().isSolutionInitialized()) {
            lastBestSolution.set(newBestSolution);
            /*
            TODO ver si agrego esto de procesar la solution en diferido.
            if (hasWaitForImprovedSolutionDuration()) {
                scheduleOnBestSolutionChange(newBestSolution, config.getWaitForImprovedSolutionDuration().toMillis());
            } else {
                onBestSolutionChange(newBestSolution);
            }
             */
            onBestSolutionChange(newBestSolution);
        }
    }

    private void onBestSolutionChange(TaskAssigningSolution newBestSolution) {
        if (!context.isCurrentChangeSetProcessed()) {
            executeSolutionChange(newBestSolution);
        }
    }

    private void executeSolutionChange(TaskAssigningSolution solution) {
        lock.lock();
        try {
            currentSolution.set(solution);
            context.setProcessedChangeSet(context.getCurrentChangeSetId());
            List<ProblemFactChange<TaskAssigningSolution>> changes = null;
            if (applyingPlanningExecutionResult.get()) {
                List<UserTaskEvent> pendingEvents = pollEvents();
                if (!pendingEvents.isEmpty()) {
                    changes = SolutionChangesBuilder.create()
                            .forSolution(solution)
                            .withContext(context)
                            .withUserServiceConnector(userServiceConnector)
                            .fromUserTaskEvents(pendingEvents)
                            .build();
                }
            }

            if (changes != null && !changes.isEmpty()) {
                applyingPlanningExecutionResult.set(false);
                solverExecutor.addProblemFactChanges(changes);
            } else {
                List<PlanningItem> planningItems = PlanningBuilder.create()
                        .forSolution(solution)
                        .withContext(context)
                        .withPublishWindowSize(PUBLISH_WINDOW_SIZE)
                        .build();
                if (!planningItems.isEmpty()) {
                    planningExecutor.execute(planningItems, this::onPlanningExecuted);
                } else {
                    resumeEvents();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void onPlanningExecuted(PlanningExecutionResult result) {
        lock.lock();
        try {
            TaskAssigningSolution solution = currentSolution.get();
            Map<String, User> usersById = solution.getUserList()
                    .stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
            List<ProblemFactChange<TaskAssigningSolution>> changes = new ArrayList<>();
            Task task;
            User user;
            for (PlanningExecutionResultItem resultItem : result.getItems()) {
                task = resultItem.getItem().getTask();
                if (!resultItem.hasError()) {
                    user = usersById.get(resultItem.getItem().getTargetUser());
                    changes.add(new AssignTaskProblemFactChange(new TaskAssignment(task), user));
                    context.setTaskPublished(task.getId(), true);
                } else {
                    context.setTaskPublished(task.getId(), false);
                }
            }
            if (!changes.isEmpty()) {
                changes.add(0, scoreDirector -> context.setCurrentChangeSetId(context.nextChangeSetId()));
                applyingPlanningExecutionResult.set(true);
                solverExecutor.addProblemFactChanges(changes);
            } else {
                applyingPlanningExecutionResult.set(false);
                resumeEvents();
            }
        } finally {
            lock.unlock();
        }
    }

    // use the observer instead of the @PreDestroy alternative.
    // https://github.com/quarkusio/quarkus/issues/15026
    void onShutDownEvent(@Observes ShutdownEvent ev) {
        destroy();
    }

    void destroy() {
        try {
            serviceStatus.set("Destroying");
            LOGGER.info("Service is going down and will be destroyed.");
            solverExecutor.destroy();
            solutionDataLoader.destroy();
            LOGGER.info("Service destroy sequence was executed successfully.");
        } catch (Exception e) {
            LOGGER.error("An error was produced during service destroy, but it'll go down anyway.", e);
        }
    }

    public String getStatus() {
        return serviceStatus.get();
    }

    private void onSolutionDataLoad(SolutionDataLoader.Result result) {
        lock.lock();
        try {
            if (result.hasErrors()) {
                LOGGER.error("The following error was produced during initial solution loading", result.getErrors().get(0));
                if (totalChances-- > 0) {
                    LOGGER.debug("Initial solution load failed but we have totalChances {} to retry", totalChances);
                    solutionDataLoader.start(this::onSolutionDataLoad, 1);
                } else {
                    LOGGER.debug("There are no more chances left for starting the solution, service won't be able to start");
                    solutionDataLoader.destroy();
                    solverExecutor.destroy();
                }
            } else {
                LOGGER.debug("Data loading successful: tasks: {}, users: {}", result.getTasks().size(), result.getUsers().size());
                TaskAssigningSolution solution = SolutionBuilder.newBuilder()
                        .withTasks(result.getTasks())
                        .withUsers(result.getUsers())
                        .build();

                if (solution.getTaskAssignmentList().size() > 1) {
                    serviceStatus.set("Starting Solver");
                    solverExecutor.start(solution);
                } else {
                    resumeEvents();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void startUpValidation() {
        validateConfig();
        validateSolver();
    }

    private void validateConfig() {
        TaskAssigningConfigValidator.of(config).validate();
    }

    private void validateSolver() {
        solverFactory.buildSolver();
    }
}