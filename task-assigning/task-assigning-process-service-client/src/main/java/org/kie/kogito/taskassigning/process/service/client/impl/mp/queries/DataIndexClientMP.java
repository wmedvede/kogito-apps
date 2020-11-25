package org.kie.kogito.taskassigning.process.service.client.impl.mp.queries;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DataIndexClientMP implements DataIndexClient {

    private GraphQLQueryServiceRest graphQLClient;

    public static ObjectMapper OBJECT_MAPPER;

    {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new SimpleModule().addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer()));
    }

    public DataIndexClientMP(GraphQLQueryServiceRest graphQLClient) {

        this.graphQLClient = graphQLClient;
    }

    @Override
    public List<UserTaskInstance> findTasks(List<String> state, ZonedDateTime started, String orderBy, int offset, int limit) {

        String payload = "{\n" +
                "  \"query\": \"query{UserTaskInstances(where:{state:{in:[\\\"Ready\\\"]}},orderBy:{started:ASC},pagination:{limit:100,offset:0}){id name started }}\"\n" +
                "}";

        ObjectNode queryResult = graphQLClient.executeQuery(payload);
        JsonNode data = queryResult.get("data");
        JsonNode userTaskInstancesNode = data.get("UserTaskInstances");
        UserTaskInstance[] userTaskInstances;
        try {
            userTaskInstances = OBJECT_MAPPER.treeToValue(userTaskInstancesNode, UserTaskInstance[].class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Arrays.asList(userTaskInstances);
    }
}
