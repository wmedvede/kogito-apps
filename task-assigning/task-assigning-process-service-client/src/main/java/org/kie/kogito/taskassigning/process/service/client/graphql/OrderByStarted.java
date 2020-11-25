package org.kie.kogito.taskassigning.process.service.client.graphql;

public class OrderByStarted implements UserTaskInstanceOrderBy {

    private OrderBy started;

    public OrderByStarted(OrderBy started) {
        this.started = started;
    }

    public OrderBy getStarted() {
        return started;
    }

    public void setStarted(OrderBy started) {
        this.started = started;
    }
}
