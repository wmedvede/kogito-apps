package org.kie.kogito.taskassigning.process.service.client.graphql;

public class InStringArgument implements StringArgument {

    private String[] in;

    public InStringArgument(String[] in) {
        this.in = in;
    }

    public String[] getIn() {
        return in;
    }

    public void setIn(String[] in) {
        this.in = in;
    }

}
