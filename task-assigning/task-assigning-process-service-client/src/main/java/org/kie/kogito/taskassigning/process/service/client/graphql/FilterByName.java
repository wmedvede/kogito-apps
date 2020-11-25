package org.kie.kogito.taskassigning.process.service.client.graphql;

public class FilterByName implements UserTaskInstanceArgument {

    private StringArgument name;

    public FilterByName() {
    }

    private FilterByName(StringArgument name) {
        this.name = name;
    }

    public StringArgument getName() {
        return name;
    }

    public void setName(StringArgument name) {
        this.name = name;
    }

    public static FilterByName of(StringArgument value) {
        return new FilterByName(value);
    }
}
