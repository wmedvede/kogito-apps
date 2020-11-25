package org.kie.kogito.taskassigning.process.service.client.graphql;

/**
 * DateArgument for specifying the equal operation.
 */
public class EqualDateArgument implements DateArgument {

    private String equal;

    public EqualDateArgument(String equal) {
        this.equal = equal;
    }

    public String getEqual() {
        return equal;
    }

    public void setEqual(String equal) {
        this.equal = equal;
    }
}
