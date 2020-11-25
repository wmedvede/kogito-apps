package org.kie.kogito.taskassigning.process.service.client.graphql.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.kie.kogito.taskassigning.process.service.client.graphql.test.QueryParamsGeneratorTest.OBJECT_MAPPER;

public class StringEqualArgumentGenerator implements ToJsonGenerator.ArgumentGenerator<StringEqualArgument> {

    @Override
    public JsonNode apply(StringEqualArgument argument) {
        ObjectNode result = OBJECT_MAPPER.createObjectNode();
        if (argument.getValue() != null) {
            result.put(StringEqualArgument.EQUAL, argument.getValue());
        } else {
            result.putNull(StringEqualArgument.EQUAL);
        }
        return result;
    }
}
