package org.kie.kogito.taskassigning.process.service.client.graphql.test;

public abstract class StringArgument<T> implements Argument {

    private static final String IN = "in";
    private static final String LIKE = "like";

    private String operator;
    private T value;


}
