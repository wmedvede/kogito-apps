package org.kie.kogito.taskassigning.process.service.client.graphql;

import java.util.List;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientBuilder;
import io.smallrye.graphql.client.typesafe.impl.GraphQlClientBuilderImpl;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.microprofile.client.impl.MpClientBuilderImpl;
import org.junit.jupiter.api.Test;
import org.kie.kogito.taskassigning.process.service.client.BasicAuthenticationCredentials;
import org.kie.kogito.taskassigning.process.service.client.impl.mp.BasicAuthenticationFilter;

//@QuarkusTest
public class TestGraphqlClient {

    //@Inject
    DataIndexQueryFactory queryFactory;

    @Test
    void executeQuery() {

        queryFactory = new DataIndexQueryFactory();
        DataIndexAPI api = queryFactory.newDataIndexAPI("http://localhost:8180/graphql", "walter", "medvedeo");


        UserTaskInstanceArgument argument = new UserTaskInstanceArgument();
        IdArgument id = new IdArgument();
        id.setEqual("d4d8e690-a6aa-482c-bcfa-4df5d4eb0115");
        id.setIn(new String[]{"d4d8e690-a6aa-482c-bcfa-4df5d4eb0115", "539bb4a9-cf68-43b9-b913-a22fc8ede3c8"});
        //argument.setId(id);

        Pagination pagination = new Pagination();
        pagination.setLimit(2);
        pagination.setOffset(10);

        UserTaskInstanceOrderBy orderBy = new UserTaskInstanceOrderBy();
        orderBy.setState(OrderBy.DESC);
        orderBy.setActualOwner(OrderBy.DESC);
        orderBy.setDescription(OrderBy.DESC);
        orderBy.setName(OrderBy.DESC);
        orderBy.setPriority(OrderBy.DESC);
        orderBy.setProcessId(OrderBy.DESC);
        orderBy.setCompleted(OrderBy.DESC);
        orderBy.setStarted(OrderBy.DESC);
        orderBy.setReferenceName(OrderBy.DESC);
        orderBy.setLastUpdate(OrderBy.DESC);

        String theOrderBy = "{ \"state\": ASC}";


        UserTaskInstanceOrderBy orderByTest = new UserTaskInstanceOrderBy();
        orderByTest.setActualOwner(OrderBy.ASC);
        List<UserTaskInstance> tasks = api.getUserTaskInstances(argument, orderByTest, pagination);
        int i = 0;

    }
}
