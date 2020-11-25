package org.kie.kogito.taskassigning.process.service.client.graphql.test;

import java.util.List;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kie.kogito.taskassigning.process.service.client.graphql.DataIndexQueryFactory;
import org.kie.kogito.taskassigning.process.service.client.graphql.FilterByName;
import org.kie.kogito.taskassigning.process.service.client.graphql.FilterByStarted;
import org.kie.kogito.taskassigning.process.service.client.graphql.FilterByStateAndStarted;
import org.kie.kogito.taskassigning.process.service.client.graphql.GreaterThanDateArgument;
import org.kie.kogito.taskassigning.process.service.client.graphql.InStringArgument;
import org.kie.kogito.taskassigning.process.service.client.graphql.OrderBy;
import org.kie.kogito.taskassigning.process.service.client.graphql.OrderByName;
import org.kie.kogito.taskassigning.process.service.client.graphql.OrderByStarted;
import org.kie.kogito.taskassigning.process.service.client.graphql.Pagination;
import org.kie.kogito.taskassigning.process.service.client.graphql.UserTaskInstance;
import org.kie.kogito.taskassigning.process.service.client.graphql.UserTaskInstanceArgument;
import org.kie.kogito.taskassigning.process.service.client.graphql.UserTaskInstanceQuery;

@QuarkusTest
public class TestGraphqlClientTest {

    @Inject
    DataIndexQueryFactory queryFactory;

    @Test
    void executeQueryByStateAndStartedDateGreaterThan() {

        //queryFactory = new DataIndexClientFactory();
        UserTaskInstanceQuery api = queryFactory.newUserTaskInstanceQuery("http://localhost:8180/graphql", "walter", "medvedeo");

        FilterByStateAndStarted filterByStateAndStarted = new FilterByStateAndStarted(new InStringArgument(new String[]{"Ready22", "Ready"}),
                                                                                      new GreaterThanDateArgument("2020-11-19T09:04:09.519Z"));

//        filterByStateAndStarted.setState());
//        filterByStateAndStarted.setAnd(new UserTaskInstanceArgument[]{new FilterByStarted(new GreaterThanDateArgument("2020-11-19T09:04:09.519Z"))});

        //FilterByStarted filterByStarted = new FilterByStarted(new GreaterThanEqualDateArgument("2020-11-19T09:04:09.519Z"));
        FilterByStarted filterByStarted = new FilterByStarted(new GreaterThanDateArgument("2020-11-19T09:04:09.519Z"));
        //FilterByStarted filterByStarted = new FilterByStarted(new EqualDateArgument("2020-11-19T09:04:09.519Z"));

        filterByStateAndStarted.setAnd(new UserTaskInstanceArgument[]{filterByStarted});

        Pagination pagination = new Pagination();
        pagination.setLimit(20);
        pagination.setOffset(0);

        OrderByStarted orderByStarted = new OrderByStarted(OrderBy.ASC);

//        List<UserTaskInstance> tasks = api.getUserTaskInstances(filterByStarted, orderByStarted, pagination);
        List<UserTaskInstance> tasks = api.getUserTaskInstances(filterByStateAndStarted, orderByStarted, pagination);
        System.out.println("Tasks.size = " + tasks.size());

        System.out.println(tasks);
        int i = 0;

    }


    @Test
    void executeQueryByName() {

        /**
         *
         * where status in ("A", "B", "C")
         * and started >= abc
         * and
         */
        queryFactory = new DataIndexQueryFactory();
        UserTaskInstanceQuery api = queryFactory.newUserTaskInstanceQuery("http://localhost:8180/graphql", "walter", "medvedeo");

        FilterByName filterByName = FilterByName.of(new InStringArgument(new String[]{"CompleteInfo"}));

        Pagination pagination = new Pagination();
        pagination.setLimit(20);
        pagination.setOffset(0);

        OrderByName orderByName = new OrderByName(OrderBy.ASC);

        List<UserTaskInstance> tasks = api.getUserTaskInstances(filterByName, orderByName, pagination);
        System.out.println("Tasks.size = " + tasks.size());

        System.out.println(tasks);
        int i = 0;

    }

}
