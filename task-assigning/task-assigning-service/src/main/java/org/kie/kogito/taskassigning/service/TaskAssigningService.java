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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.kie.kogito.taskassigning.core.model.TaskAssigningSolution;
import org.kie.kogito.taskassigning.service.config.TaskAssigningConfig;
import org.kie.kogito.taskassigning.service.config.TaskAssigningConfigValidator;
import org.kie.kogito.taskassigning.service.messaging.BufferedUserTaskEventConsumer;
import org.kie.kogito.taskassigning.service.messaging.UserTaskEvent;
import org.kie.kogito.taskassigning.service.messaging.UserTaskEventConsumer;
import org.kie.kogito.taskassigning.user.service.api.UserServiceConnector;
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

    private SolverExecutor solverExecutor;

    private SolutionDataLoader solutionDataLoader;

    private PlanningExecutor planningExecutor;

    private TaskAssigningServiceContext context;

    private AtomicReference<TaskAssigningSolution> currentSolution = new AtomicReference<>(null);

    private AtomicReference<TaskAssigningSolution> lastBestSolution = new AtomicReference<>(null);

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
        ((BufferedUserTaskEventConsumer) userTaskEventConsumer).setConsumer(this::onTaskEvents);
        solverExecutor = new SolverExecutor(solverFactory, this::onBestSolutionChange);
        managedExecutor.execute(solverExecutor);
        solutionDataLoader = new SolutionDataLoader(taskServiceConnector,
                                                    userServiceConnector,
                                                    Duration.ofMillis(5000));
        managedExecutor.execute(solutionDataLoader);
        solutionDataLoader.start(this::onSolutionDataLoad, 1);
    }

    private void pauseEvents() {
        userTaskEventConsumer.pause();
        /*
         1) the events are initially paused
         2) only when the service is up, and the solver executor is properly initialized, the events are enabled.
         3) important thing!!!! while we load the initial solution the events are also paused
            So we have that
            1) we try to load the initial solution
            2) when the loading is successful we have that
                    if (we have tasks) {
                        good, create the initial solution
                        and start the solver
                    } else {
                        well, there where no tasks tasks at this moment
                        no problem, as soon any task is created, etc,
                        events will arrive.
                        So simply sit and wait for events
                        resumeEvents();
                    }

        */
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
            /*
            if (no hay solution) {
                quiere decir que comenzamos sin tareas y nos quedamos escuchando...
                analizar los cambios y ver si tiene sentido crear la solucion....
                tendríamos q volver leer los usuarios..... o los tomamos de lo que se logro cargar
                anteriormente....

            } else if (ok ya hay una solution) {
                calcular los cambios que se han producido....
                si hay cambios ejecutarlos
            } else {
                si por algun motivo ejemplo me llega un evento atrasado
                o estoy justo en el caso ejecucion de un plan etc
                y realmente ho nay nada para hacer
                resumeEvents();
            }
             */

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
            List<PlanningItem> planningItems = PlanningBuilder.create()
                    .withSolution(solution)
                    //TODO parametrize this value
                    .withPublishWindowSize(2)
                    .build();

            if (!planningItems.isEmpty()) {
                planningExecutor.execute(planningItems, this::onPlanningExecuted);
            } else {
                resumeEvents();
            }
        } finally {
            lock.unlock();
        }
    }

    private void onPlanningExecuted(PlanningExecutionResult result) {
        lock.lock();
        try {
            LOGGER.debug("dale fiera!!!!");

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

    //TODO refactor this method shomewere


}