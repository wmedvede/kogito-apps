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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import org.kie.kogito.taskassigning.ClientServices;
import org.kie.kogito.taskassigning.core.model.Task;
import org.kie.kogito.taskassigning.process.service.client.ProcessServiceClient;
import org.kie.kogito.taskassigning.service.config.TaskAssigningConfig;
import org.kie.kogito.taskassigning.service.config.TaskAssigningConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.kogito.taskassigning.service.RunnableBase.Status.STARTED;
import static org.kie.kogito.taskassigning.service.RunnableBase.Status.STARTING;
import static org.kie.kogito.taskassigning.service.RunnableBase.Status.STOPPED;

public class PlanningExecutor extends RunnableBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanningExecutor.class);

    private static final String CLAIM_PHASE = "claim";

    private TaskAssigningConfig config;
    private ClientServices clientServices;

    private Semaphore startPermit = new Semaphore(0);
    private List<PlanningItem> planningItems;
    private Consumer<PlanningExecutionResult> resultConsumer;
    private Map<String, ProcessServiceClient> serviceClientMap = new HashMap<>();

    public PlanningExecutor(ClientServices clientServices, TaskAssigningConfig config) {
        this.clientServices = clientServices;
        this.config = config;
    }

    public void execute(List<PlanningItem> planningItems, Consumer<PlanningExecutionResult> resultConsumer) {
        if (!status.compareAndSet(STOPPED, STARTING)) {
            throw new IllegalStateException("execute method can only be invoked when the status is STOPPED");
        }
        this.planningItems = planningItems;
        this.resultConsumer = resultConsumer;
        startPermit.release();
    }

    @Override
    public void destroy() {
        super.destroy();
        startPermit.release();
    }

    @Override
    public void run() {
        while (isAlive()) {
            try {
                startPermit.acquire();
                if (isAlive() && status.compareAndSet(STARTING, STARTED)) {
                    PlanningExecutionResult result = executePlanning(planningItems);
                    if (isAlive()) {
                        resultConsumer.accept(result);
                        //TODO, ver si este stopped lo meto antes o despues de que el consumer haga su trabajo.
                        status.compareAndSet(STARTED, STOPPED);
                    }
                }
            } catch (InterruptedException e) {
                super.destroy();
                Thread.currentThread().interrupt();
            }
        }
        closeServiceClients();
    }

    private PlanningExecutionResult executePlanning(List<PlanningItem> planningItems) {
        ArrayList<PlanningExecutionResultItem> resultItems = new ArrayList<>();
        //TODO acá podria poner el check de isAlive para tener un mayor control y poder mater la ejecucion.... si fuera necesario...
        //while(isAlive()) {
        //}
        for (PlanningItem planningItem : planningItems) {
            try {
                URL serviceURL = buildServiceURL(planningItem.getTask());
                ProcessServiceClient serviceClient = serviceClientMap.computeIfAbsent(serviceURL.toString(), ur -> TaskAssigningConfigUtil.createProcessServiceClient(clientServices, config, serviceURL));
                serviceClient.transitionTask(planningItem.getTask().getProcessId(),
                                             planningItem.getTask().getProcessInstanceId(),
                                             planningItem.getTask().getName(),
                                             planningItem.getTask().getId(),
                                             CLAIM_PHASE,
                                             planningItem.getTargetUser(),
                                             new ArrayList<>(planningItem.getTask().getPotentialGroups()));
                resultItems.add(new PlanningExecutionResultItem(planningItem));

            } catch (Exception e) {
                resultItems.add(new PlanningExecutionResultItem(planningItem, e));
            }
        }
        return new PlanningExecutionResult(resultItems);
    }

    private void closeServiceClients() {
        serviceClientMap.values().forEach(serviceClient -> {
            try {
                serviceClient.close();
            } catch (IOException e) {
                LOGGER.warn("Error while closing process service client: {}", e.getMessage());
            }
        });
    }

    private static URL buildServiceURL(Task task) throws MalformedURLException {
        int serviceUrlIndex = task.getEndpoint().indexOf(task.getProcessId() + "/" + task.getProcessInstanceId());
        return new URL(task.getEndpoint().substring(0, serviceUrlIndex - 1));
    }
}
