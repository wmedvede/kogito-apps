package org.kie.kogito.taskassigning.process.service.client.graphql;

public class UserTaskInstance {

    private String id;
    private String processId;
    private String name;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "UserTaskInstance{" +
                "id='" + id + '\'' +
                ", processId='" + processId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
