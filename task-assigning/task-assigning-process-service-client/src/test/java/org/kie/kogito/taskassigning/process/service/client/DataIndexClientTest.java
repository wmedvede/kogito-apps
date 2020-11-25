package org.kie.kogito.taskassigning.process.service.client;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kie.kogito.taskassigning.process.service.client.graphql.DataIndexQueryFactory;
import org.kie.kogito.taskassigning.process.service.client.impl.mp.queries.DataIndexClient;
import org.kie.kogito.taskassigning.process.service.client.impl.mp.queries.DataIndexClientFactory;
import org.kie.kogito.taskassigning.process.service.client.impl.mp.queries.UserTaskInstance;

@QuarkusTest
public class DataIndexClientTest {

    @Inject
    DataIndexClientFactory clientFactory;

    @Test
    void findTasks() {
        DataIndexClient client = clientFactory.newClient(ProcessServiceClientConfig.newBuilder()
                                        .serviceUrl("http://localhost:8180/graphql")
                                        .build(),
                                                NoAuthenticationCredentials.INSTANCE);

        List<UserTaskInstance> tasks = client.findTasks(Arrays.asList("Ready", "Kaka"), ZonedDateTime.now(), "what?", 0, 10);
        System.out.println(tasks);

    }
}
