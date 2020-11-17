package org.kie.kogito.taskassigning.process.service.client.graphql;

import org.eclipse.microprofile.graphql.Input;

@Input("UserTaskInstanceOrderBy")
public class UserTaskInstanceOrderBy {

    private OrderBy state;
    private OrderBy actualOwner;
    private OrderBy description;
    private OrderBy name;
    private OrderBy priority;
    private OrderBy processId;
    private OrderBy completed;
    private OrderBy started;
    private OrderBy referenceName;
    private OrderBy lastUpdate;

    public OrderBy getState() {
        return state;
    }

    public void setState(OrderBy state) {
        this.state = state;
    }



    public OrderBy getActualOwner() {
        return actualOwner;
    }

    public void setActualOwner(OrderBy actualOwner) {
        this.actualOwner = actualOwner;
    }

    public OrderBy getDescription() {
        return description;
    }

    public void setDescription(OrderBy description) {
        this.description = description;
    }

    public OrderBy getName() {
        return name;
    }

    public void setName(OrderBy name) {
        this.name = name;
    }

    public OrderBy getPriority() {
        return priority;
    }

    public void setPriority(OrderBy priority) {
        this.priority = priority;
    }

    public OrderBy getProcessId() {
        return processId;
    }

    public void setProcessId(OrderBy processId) {
        this.processId = processId;
    }

    public OrderBy getCompleted() {
        return completed;
    }

    public void setCompleted(OrderBy completed) {
        this.completed = completed;
    }

    public OrderBy getStarted() {
        return started;
    }

    public void setStarted(OrderBy started) {
        this.started = started;
    }

    public OrderBy getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(OrderBy referenceName) {
        this.referenceName = referenceName;
    }

    public OrderBy getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OrderBy lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

}
