package org.kie.kogito.taskassigning.process.service.client.graphql.test;

public abstract class StringValueArgument implements Argument {

    private String value;

    public StringValueArgument(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
