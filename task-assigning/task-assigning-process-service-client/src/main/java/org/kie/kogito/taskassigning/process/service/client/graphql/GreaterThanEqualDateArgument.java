package org.kie.kogito.taskassigning.process.service.client.graphql;

public class GreaterThanEqualDateArgument implements DateArgument {

    private String greaterThanEqual;

    public GreaterThanEqualDateArgument(String greaterThanEqual) {
        this.greaterThanEqual = greaterThanEqual;
    }

    public String getGreaterThanEqual() {
        return greaterThanEqual;
    }

    public void setGreaterThanEqual(String greaterThanEqual) {
        this.greaterThanEqual = greaterThanEqual;
    }
}
