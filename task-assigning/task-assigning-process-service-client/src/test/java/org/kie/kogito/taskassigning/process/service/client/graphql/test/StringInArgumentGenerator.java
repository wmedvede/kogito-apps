package org.kie.kogito.taskassigning.process.service.client.graphql.test;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.kie.kogito.taskassigning.process.service.client.graphql.test.QueryParamsGeneratorTest.OBJECT_MAPPER;

public class StringInArgumentGenerator implements ToJsonGenerator.ArgumentGenerator<StringInArgument> {

    public JsonNode apply(StringInArgument argument) {
        ObjectNode result = OBJECT_MAPPER.createObjectNode();
        if (argument.getValue() != null) {
            ArrayNode arrayNode = result.putArray(StringInArgument.IN);
            Stream.of(argument.getValue()).forEach(arrayNode::add);
        } else {
            result.putNull(StringInArgument.IN);
        }
        return result;
    }
}
