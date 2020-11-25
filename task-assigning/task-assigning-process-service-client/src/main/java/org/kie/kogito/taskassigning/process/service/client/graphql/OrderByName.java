package org.kie.kogito.taskassigning.process.service.client.graphql;

public class OrderByName implements UserTaskInstanceOrderBy {

    private OrderBy name;

    public OrderByName(OrderBy name) {
        this.name = name;
    }

    public OrderBy getName() {
        return name;
    }

    public void setName(OrderBy name) {
        this.name = name;
    }
}
