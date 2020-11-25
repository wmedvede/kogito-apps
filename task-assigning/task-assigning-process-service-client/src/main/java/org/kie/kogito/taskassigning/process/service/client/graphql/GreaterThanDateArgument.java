package org.kie.kogito.taskassigning.process.service.client.graphql;

/**
 * DateArgument for specifying the greaterThan operation.
 */
public class GreaterThanDateArgument implements DateArgument {

    private String greaterThan;

    public GreaterThanDateArgument(String greaterThan) {
        this.greaterThan = greaterThan;
    }

    public String getGreaterThan() {
        return greaterThan;
    }

    public void setGreaterThan(String greaterThan) {
        this.greaterThan = greaterThan;
    }
}
