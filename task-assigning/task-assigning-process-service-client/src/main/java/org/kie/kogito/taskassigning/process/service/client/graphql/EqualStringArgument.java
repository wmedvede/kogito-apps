package org.kie.kogito.taskassigning.process.service.client.graphql;

public class EqualStringArgument implements StringArgument {

    private String equal;

    private EqualStringArgument(String equal) {
        this.equal = equal;
    }

    public String getEqual() {
        return equal;
    }

    public void setEqual(String equal) {
        this.equal = equal;
    }

    public static EqualStringArgument of(String value) {
        return new EqualStringArgument(value);
    }
}
