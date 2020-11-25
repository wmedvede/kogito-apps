package org.kie.kogito.taskassigning.process.service.client.graphql;

public class FilterByStateAndStarted implements UserTaskInstanceArgument {

    private StringArgument state;

    private UserTaskInstanceArgument[] and;

    public FilterByStateAndStarted(StringArgument state, DateArgument started) {
        this.state = state;
        this.and = new UserTaskInstanceArgument[]{new FilterByStarted(started)};
    }

    public StringArgument getState() {
        return state;
    }

    public void setState(StringArgument state) {
        this.state = state;
    }

    public UserTaskInstanceArgument[] getAnd() {
        return and;
    }

    public void setAnd(UserTaskInstanceArgument[] and) {
        this.and = and;
    }
}
