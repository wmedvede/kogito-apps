package org.kie.kogito.taskassigning.process.service.client.graphql.test;

public class ArgumentArray implements Argument {

    private Argument[] arguments;

    public ArgumentArray(Argument[] arguments) {
        this.arguments = arguments;
    }

    public Argument[] getArguments() {
        return arguments;
    }

    public void setArguments(Argument[] arguments) {
        this.arguments = arguments;
    }
}
