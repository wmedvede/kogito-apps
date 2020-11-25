package org.kie.kogito.taskassigning.process.service.client.impl.mp.queries;

import org.kie.kogito.taskassigning.process.service.client.AuthenticationCredentials;
import org.kie.kogito.taskassigning.process.service.client.ProcessServiceClient;
import org.kie.kogito.taskassigning.process.service.client.ProcessServiceClientConfig;

public interface DataIndexClientFactory {

    DataIndexClient newClient(ProcessServiceClientConfig config, AuthenticationCredentials credentials);

}
