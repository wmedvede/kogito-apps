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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TaskAssigningServiceContext {

    private class TaskContext {
        private boolean published;

        public void setPublished(boolean published) {
            this.published = published;
        }

        public boolean isPublished() {
            return published;
        }
    }

    private long changeSetIds;
    private long currentChangeSetId;
    private long lastProcessedChangeSetId = -1;
    private Map<String, TaskContext> taskContextMap = new HashMap<>();

    //TODO esto en principio se va si paso a dejar todo metido adentro de la TASK o en el task assignment en su defecto
    //podriamos tener problemas de performance si seteo la nueva tarea todo el tiempo???
    //pero seria digamos la forma correcta.
    //podrian haber trucos como por ejemplo ver un poco mas selectivamente si algo ha cambiado ademas de la fecha
    //OJO, al tener todo metido en la solution, al final tengo que como minimo cambiar
    // 1) last modification date
    // 2) status...? y si porque puede ir de modifyed a released....
    // 3) el assigned user, esto tiene que ir en concordancia con el que va cambiando.
    // 4) en la vesion de kie-server no tenia este problema porque cargaba las cosas mas selectivamente...
    // hay q ver como optimizar esto.
    // la "ventaja" entre comillas que al crear una nueva instancia de la tarea
    // al final me ahorro el clone PEEEEERO!!!!!, atento casco.... porque si empiezan a cambiar por ejemplo los inputs
    // ya se va todo al carajo pues podrian cambiar los skills affinities, etc
    // creo que ya seria irse muy al carajo....
    // tal vez ahi esta el limite de la integracion, razonablemente podemos tracear cambios en
    // la prioridad, estado obviamente, el last modificationDate y despues ya seria mucho creo.


    private Map<String, ZonedDateTime> taskChangeTimes = new HashMap<>();

    public long getCurrentChangeSetId() {
        return currentChangeSetId;
    }

    public void setCurrentChangeSetId(long currentChangeSetId) {
        this.currentChangeSetId = currentChangeSetId;
    }

    public long nextChangeSetId() {
        return ++changeSetIds;
    }

    public boolean isProcessedChangeSet(long changeSetId) {
        return changeSetId <= lastProcessedChangeSetId;
    }

    public boolean isCurrentChangeSetProcessed() {
        return isProcessedChangeSet(currentChangeSetId);
    }

    public void setProcessedChangeSet(long changeSetId) {
        this.lastProcessedChangeSetId = changeSetId;
    }

    public void clearProcessedChangeSet() {
        lastProcessedChangeSetId = -1;
    }

    public synchronized void setTaskPublished(String taskId, boolean published) {
        TaskContext taskContext = taskContextMap.computeIfAbsent(taskId, id -> new TaskContext());
        taskContext.setPublished(published);
    }
    public synchronized boolean isPublished(String taskId) {
        TaskContext taskContext = taskContextMap.get(taskId);
        return taskContext != null && taskContext.isPublished();
    }

    public void setTaskChangeTime(String taskId, ZonedDateTime changeTime) {
        taskChangeTimes.put(taskId, changeTime);
    }

    public ZonedDateTime getTaskChangeTime(String taskId) {
        return taskChangeTimes.get(taskId);
    }
}