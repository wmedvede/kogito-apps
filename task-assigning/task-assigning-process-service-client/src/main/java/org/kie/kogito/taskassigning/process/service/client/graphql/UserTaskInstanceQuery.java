package org.kie.kogito.taskassigning.process.service.client.graphql;

import java.util.List;

import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

//@GraphQlClientApi
public interface UserTaskInstanceQuery {

    @Query("UserTaskInstances")
    List<UserTaskInstance> getUserTaskInstances(@Name("where") UserTaskInstanceArgument where,
                                                @Name("orderBy") UserTaskInstanceOrderBy orderBy,
                                                @Name("pagination") Pagination pagination);



    /*
    The final API method for reading all the tasks must be

    List<Task> findTasks(List<Status> status, ZonedDateTime fromStarted, int offset, int limit, String orderBy) {

    }

   And the interface for the TaskReader that reads all the tasks
   could be

       List<Task> findAllTasks(List<Status> status, ZonedDateTime fromStarted, int pageSize) {

       Internally executes the query N times until there are no more results.

       1) findTasks(status, 0 * pageSize, pageSize)
       2) findTasks(status, 1 * pageSize, pagesSize)
       3) findTasks(status, 2 + pageSize, pageSize)

       until there are no more results.
       }


    }





     */

    }
