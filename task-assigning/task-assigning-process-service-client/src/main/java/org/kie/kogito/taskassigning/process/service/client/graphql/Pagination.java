package org.kie.kogito.taskassigning.process.service.client.graphql;

import org.eclipse.microprofile.graphql.Input;

@Input("Pagination")
public class Pagination {

    private int limit;
    private int offset;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
