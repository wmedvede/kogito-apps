package org.kie.kogito.taskassigning.process.service.client.graphql;

public class OrderByState implements UserTaskInstanceOrderBy {

    private OrderBy state;

    public OrderByState(OrderBy state) {
        this.state = state;
    }

    public OrderBy getState() {
        return state;
    }

    public void setState(OrderBy state) {
        this.state = state;
    }
}
