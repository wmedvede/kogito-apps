package org.kie.kogito.taskassigning.process.service.client.graphql.test;

import java.util.ArrayList;
import java.util.List;

public class ArgumentContainer implements Argument {

    public static class ArgumentEntry {
        private String name;
        private Argument value;

        public ArgumentEntry(String name, Argument value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Argument getValue() {
            return value;
        }
    }

    protected List<ArgumentEntry> arguments = new ArrayList<>();

    public void add(String name, Argument argument) {
        arguments.add(new ArgumentEntry(name, argument));
    }

    public List<ArgumentEntry> getArguments() {
        return arguments;
    }

    public boolean isEmpty() {
        return arguments == null || arguments.isEmpty();
    }
}
