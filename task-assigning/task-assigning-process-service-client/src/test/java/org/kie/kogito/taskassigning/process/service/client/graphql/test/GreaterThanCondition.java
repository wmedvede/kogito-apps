package org.kie.kogito.taskassigning.process.service.client.graphql.test;

public class GreaterThanCondition {

    private String fieldName;
    private String value;

    public GreaterThanCondition(String fieldName, String value) {
        this.fieldName = fieldName;
        this.value = value;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
