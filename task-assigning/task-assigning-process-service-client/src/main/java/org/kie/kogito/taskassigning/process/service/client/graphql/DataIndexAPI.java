package org.kie.kogito.taskassigning.process.service.client.graphql;

import java.util.List;

import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

//@GraphQlClientApi
public interface DataIndexAPI {

    @Query("UserTaskInstances")
    List<UserTaskInstance> getUserTaskInstances(@Name("where") UserTaskInstanceArgument where,
                                                @Name("orderBy") UserTaskInstanceOrderBy orderBy,
                                                @Name("pagination") Pagination pagination);
}
