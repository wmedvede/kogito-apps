package org.kie.kogito.taskassigning.process.service.client.graphql.test;

public class StringArgumentFactory {

    public static StringInArgument newStringInArgument(String[] value) {
        return new StringInArgument(value);
    }
}
