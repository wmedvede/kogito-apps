package org.kie.kogito.taskassigning.process.service.client.graphql;

import org.eclipse.microprofile.graphql.Input;

@Input("StringArgument")
public class StringArgument {

    private String[] in;
    private String like;
    private boolean isNull;
    private String equal;

    public String[] getIn() {
        return in;
    }

    public void setIn(String[] in) {
        this.in = in;
    }

    public String getLike() {
        return like;
    }

    public void setLike(String like) {
        this.like = like;
    }

    public boolean isNull() {
        return isNull;
    }

    public void setNull(boolean aNull) {
        isNull = aNull;
    }

    public String getEqual() {
        return equal;
    }

    public void setEqual(String equal) {
        this.equal = equal;
    }
}
