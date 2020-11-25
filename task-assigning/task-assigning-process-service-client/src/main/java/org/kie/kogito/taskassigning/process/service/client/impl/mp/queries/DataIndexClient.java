package org.kie.kogito.taskassigning.process.service.client.impl.mp.queries;

import java.time.ZonedDateTime;
import java.util.List;

public interface DataIndexClient {

    List<UserTaskInstance> findTasks(List<String> state, ZonedDateTime started, String orderBy, int offset, int limit);

}
