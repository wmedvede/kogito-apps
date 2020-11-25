package org.kie.kogito.taskassigning.process.service.client.graphql.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

public class QueryParamsGeneratorTest {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void createWhere() {

        ObjectNode whereNode = OBJECT_MAPPER.createObjectNode();

        ObjectNode stateNode = OBJECT_MAPPER.createObjectNode();
        ArrayNode options = stateNode.putArray("in");
        options.add("Ready");
        options.add("Completed");

        whereNode.set("state", stateNode);

        ArrayNode andNode = whereNode.putArray("and");

        GreaterThanCondition greaterThanCondition = new GreaterThanCondition("started", "2020-11-19T09:04:09.519Z");
        ObjectNode greaterThanConditionNode = asJson(greaterThanCondition);

        andNode.add(greaterThanConditionNode);


        System.out.println(whereNode.toPrettyString());

    }

    @Test
    void userTaskArgument() {
        UserTaskInstanceArgument userTaskInstanceArgument = new UserTaskInstanceArgument();
        userTaskInstanceArgument.add("state", new StringInArgument(new String[]{"A", "B"}));
    }


    ObjectNode asJson(GreaterThanCondition condition) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        ObjectNode conditionNode = node.putObject(condition.getFieldName());
        conditionNode.put("greaterThan", condition.getValue());
        return node;
    }

    @Test
    void toJSONGeneratorTest() {
        ToJsonGenerator generator = new ToJsonGenerator();

        UserTaskInstanceArgument userTaskInstanceArgument = new UserTaskInstanceArgument();
        userTaskInstanceArgument.add("state", new StringInArgument(new String[]{"A", "B"}));

        Argument argument1 = new StringInArgument(new String[]{"C", "D"});
        Argument argument2 = new StringInArgument(new String[]{"E", "F"});

        ArgumentArray argumentArray = new ArgumentArray(new Argument[]{argument1, argument2});
        userTaskInstanceArgument.add("elArray", argumentArray);

        ArgumentContainer container = new ArgumentContainer();
        container.add("name", new StringInArgument(new String[]{"Walter", "Ignacio"}));
        container.add("surname", new StringEqualArgument("Medvedeo"));

        userTaskInstanceArgument.add("userInfo", container);

        userTaskInstanceArgument.add("NIE", new StringEqualArgument("X9513200D"));

        JsonNode node = generator.asJson(userTaskInstanceArgument);
        System.out.println(node.toPrettyString());
    }
}
