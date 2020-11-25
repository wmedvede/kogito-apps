package org.kie.kogito.taskassigning.process.service.client.graphql.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.kie.kogito.taskassigning.process.service.client.graphql.test.QueryParamsGeneratorTest.OBJECT_MAPPER;

public class ToJsonGenerator {

    public interface ArgumentGenerator<T extends Argument> extends Function<T, JsonNode> {

    }

    public static Map<Class<? extends Argument>, ArgumentGenerator<? extends Argument>> argumentGenerators = new HashMap<>();

    public ToJsonGenerator() {
        argumentGenerators.put(StringInArgument.class, new StringInArgumentGenerator());
        argumentGenerators.put(StringEqualArgument.class, new StringEqualArgumentGenerator());
    }

    public JsonNode asJson(Argument argument) {

        if (argument == null) {
            return null;
        }
        if (argument instanceof ArgumentContainer) {
            ObjectNode result = OBJECT_MAPPER.createObjectNode();
            List<ArgumentContainer.ArgumentEntry> arguments = ((ArgumentContainer) argument).getArguments();
            if (arguments != null) {
                arguments.forEach(entry -> result.set(entry.getName(), asJson(entry.getValue())));
            }
            return result;
        } else if (argument instanceof ArgumentArray) {
            ArrayNode result = OBJECT_MAPPER.createArrayNode();
            Argument[] arguments = ((ArgumentArray) argument).getArguments();
            if (arguments != null) {
                Stream.of(arguments).forEach(value -> result.add(asJson(value)));
            }
            return result;
        } else {
            ArgumentGenerator<? extends Argument> generator = argumentGenerators.get(argument.getClass());
            return (JsonNode) ((ArgumentGenerator) generator).apply(argument);
        }
    }
}
