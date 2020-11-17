package org.kie.kogito.taskassigning.process.service.client.graphql;

import org.eclipse.microprofile.graphql.Input;

@Input("UserTaskInstanceArgument")
public class UserTaskInstanceArgument {

    private IdArgument id;
    private StringArgument name;

    public IdArgument getId() {
        return id;
    }

    public void setId(IdArgument id) {
        this.id = id;
    }

    public StringArgument getName() {
        return name;
    }

    public void setName(StringArgument name) {
        this.name = name;
    }
}
