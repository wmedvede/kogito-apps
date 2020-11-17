package org.kie.kogito.taskassigning.process.service.client.graphql;

import org.eclipse.microprofile.graphql.Input;

@Input("IdArgument")
public class IdArgument {

    String[] in;
    String equal;
   // boolean isNull = false;

    public String[] getIn() {
        return in;
    }

    public void setIn(String[] in) {
        this.in = in;
    }

    public String getEqual() {
        return equal;
    }

    public void setEqual(String equal) {
        this.equal = equal;
    }

//    public boolean isNull() {
//        return isNull;
//    }
//
//    public void setNull(boolean aNull) {
//        isNull = aNull;
//    }
}
