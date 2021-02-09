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
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.kie.kogito.taskassigning.core.model.TaskAssigningSolution;
import org.kie.kogito.taskassigning.service.config.TaskAssigningConfig;
import org.kie.kogito.taskassigning.service.config.TaskAssigningConfigValidator;
import org.kie.kogito.taskassigning.service.messaging.BackpressureBufferedEventConsumer;
import org.kie.kogito.taskassigning.user.service.UserServiceConnector;
import org.optaplanner.core.api.solver.SolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Entonces efectivamente el conector kafka se reetablece solo.
//lo pude ver tanto en el data-index como en el processo mio de prueba.
//ej, podemos bajar el servidor kafka y el data index puede seguir respondiendo consultas
//al arrancar el servidor kafka el data index restablecera la conexion.
//basicamente lo q vemos son warnings del kafka conector mientras hace el polling.

//tambien puedo arrancar los processo con el server kafka bajado y vere los mismos warnings
//cuando el servidor kafka se restablece el conector restablece la coneccion
//con lo cual el servidor kafka configurado con una URL validad PERO bajado, no impide que la app arranque
//Es mas el runtime me permite crear processos, eso no deberia poder pasar, porque luego
//cuando arrancamos el servidor kafka los mensajes NUNCA llegaran al data index, con lo cual a partir de ahi
//ya queda todo des coordinado....
// HMMMMM, wait, se ve que el cliente kafka guarda los mensajes localmetne y cuando el kafka server arranca
// los envia, asi que si que funciona. Cuando el kafka server arranca los mensajes igual le llegan al servidor del
//data index y tambien obviamente a mi servicio.
//Esto igual sea algun tipo de configuracion a nivel de kafka!!!! Verdaderamente
//no se hasta cuando el client kafka acumula los mensajes localmente si no logra acceder
//al servidor....

//Depende de la implementacion, pero al final lo que pasa es que los mensajes se bufferean en el servico en memoria.
//Si el servicio muere, los mensajes mueren. Habria q ver que pasa con los processos.... teoricamente
//si el mensaje no puede enviarse el proceso no deberia evolucionar.... A menos q se hacepte q el data index puede quear
//des coordinado con respecto runtime.

@ApplicationScoped
@Startup
public class TaskAssigningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskAssigningService.class);

    @Inject
    SolverFactory<TaskAssigningSolution> solverFactory;

    @Inject
    TaskAssigningConfig config;

    @Inject
    BackpressureBufferedEventConsumer eventConsumer;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    TaskServiceConnector taskServiceConnector;

    @Inject
    UserServiceConnector userServiceConnector;

    SolverExecutor solverExecutor;

    SolutionDataLoader solutionDataLoader;

    int totalChances = 3;

    @PostConstruct
    void start() {
        startUpValidation();
        solverExecutor = new SolverExecutor(solverFactory, solution -> {
            System.out.println("Dale papaaaaa, new Solution: " + solution);
        });

        managedExecutor.execute(solverExecutor);
        solutionDataLoader = new SolutionDataLoader(taskServiceConnector,
                                                    userServiceConnector,
                                                    Duration.ofMillis(5000));
        managedExecutor.execute(solutionDataLoader);
        solutionDataLoader.start(result -> processTaskLoadResult(result), 3);

        System.out.println("PEEEERO ANTES ESTA!");
    }

    @PreDestroy
    void destroy() {
        try {
            LOGGER.info("Service is going down and will be destroyed.");
            solverExecutor.destroy();
            solutionDataLoader.destroy();
            LOGGER.info("Service destroy sequence was executed successfully.");
        } catch (Throwable e) {
            LOGGER.error("An error was produced during service destroy, but it'll go down anyway.", e);
        }
    }

    public void startEvents() {
        LOGGER.debug("XXXXXXXXXX StartEvents");
        eventConsumer.resume();
    }

    public void pauseEvents() {
        LOGGER.debug("XXXXXXXXXX PauseEvents");
        eventConsumer.pause();
    }

    private void processTaskLoadResult(SolutionDataLoader.Result result) {
        if (result.hasErrors()) {
            LOGGER.debug("Errores al consultar las tareas: {}", result.getErrors());

        } else {
            LOGGER.debug("Data loading successful: tasks: {}, users: {}", result.getTasks().size(), result.getUsers().size());
            TaskAssigningSolution solution = SolutionBuilder.newBuilder()
                    .withTasks(result.getTasks())
                    .withUsers(result.getUsers())
                    .build();
            solverExecutor.start(solution);
        }
        if (totalChances-- > 0) {
            LOGGER.debug("Quedan chances, lanzamos el TaskLoader nuevamente");
            solutionDataLoader.start(this::processTaskLoadResult, 3);
        } else {
            LOGGER.debug("No quedan chances, Nos vamos juana!!!!");
            solutionDataLoader.destroy();
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
