package org.kie.kogito.taskassigning.process.service.client.graphql;

public class FilterByStarted implements UserTaskInstanceArgument {

    private DateArgument started;

    public FilterByStarted() {
    }

    public FilterByStarted(DateArgument started) {
        this.started = started;
    }

    public DateArgument getStarted() {
        return started;
    }

    public void setStarted(DateArgument started) {
        this.started = started;
    }
//
//    public static FilterByStarted of(DateArgument value) {
//        return new FilterByStarted(value);
//    }
}
