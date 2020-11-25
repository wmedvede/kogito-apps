package org.kie.kogito.taskassigning.process.service.client.impl.mp.queries;

import java.time.ZonedDateTime;

public class UserTaskInstance {

    private String id;
    private String description;
    private String name;
    /*
    priority: String
    processInstanceId: String!
    processId: String!
    rootProcessInstanceId: String
    rootProcessId: String
    state: String!
    actualOwner: String

    adminGroups: [String!]
    adminUsers: [String!]
    completed: DateTime
    */

    private ZonedDateTime started;
    /*
    excludedUsers: [String!]
    potentialGroups: [String!]
    potentialUsers: [String!]
    */

    private String inputs;
    /*
    outputs: String
    referenceName: String
    lastUpdate: DateTime!
    endpoint: String
    */

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ZonedDateTime getStarted() {
        return started;
    }

    public void setStarted(ZonedDateTime started) {
        this.started = started;
    }
}
