package org.kie.kogito.taskassigning.process.service.client.graphql;

public class InIdArgument implements IdArgument {

    private String[] in;

    public InIdArgument(String[] in) {
        this.in = in;
    }

    public String[] getIn() {
        return in;
    }

    public void setIn(String[] in) {
        this.in = in;
    }

}
