package org.kie.kogito.taskassigning.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.kogito.taskassigning.ClientServices;
import org.kie.kogito.taskassigning.index.service.client.DataIndexServiceClient;
import org.kie.kogito.taskassigning.index.service.client.DataIndexServiceClientFactory;
import org.kie.kogito.taskassigning.index.service.client.graphql.UserTaskInstance;
import org.kie.kogito.taskassigning.service.config.TaskAssigningConfig;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TaskServiceConnectorTest {

    private static final String TASK1 = "TASK1";
    private static final String TASK2 = "TASK2";
    private static final String TASK3 = "TASK3";
    private static final String TASK4 = "TASK4";
    private static final String TASK5 = "TASK5";
    private static final String TASK6 = "TASK6";
    private static final String TASK7 = "TASK7";
    private static final String TASK8 = "TASK8";
    private static final String TASK9 = "TASK9";

    @Mock
    TaskAssigningConfig config;

    @Mock
    ClientServices clientServices;

    @Mock
    DataIndexServiceClientFactory dataIndexServiceClientFactory;

    @Mock
    DataIndexServiceClient dataIndexServiceClient;

    @Test
    void findAllTasks() throws MalformedURLException {
        doReturn(dataIndexServiceClientFactory).when(clientServices).dataIndexClientFactory();
        doReturn(dataIndexServiceClient).when(dataIndexServiceClientFactory).newClient(any(), any());
        doReturn(false).when(config).isKeycloakSet();
        doReturn(false).when(config).isBasicAuthSet();
        doReturn(new URL("http://localhost:8180/graphql")).when(config).getDataIndexServerUrl();

        List<String> state = Collections.singletonList("Ready");
        int pageSize = 3;

        List<UserTaskInstance> result0 = Arrays.asList(mockUserTaskInstance(TASK1, parseZonedDateTime("2021-02-08T10:00:00.001Z"), "Ready"),
                                                       mockUserTaskInstance(TASK2, parseZonedDateTime("2021-02-08T11:00:00.001Z"), "Ready"),
                                                       mockUserTaskInstance(TASK3, parseZonedDateTime("2021-02-08T12:00:00.001Z"), "Reserved"));
        lenient().doReturn(result0).when(dataIndexServiceClient).findTasks(null, null, "STARTED", true, 0, pageSize);
        ZonedDateTime nextTime = parseZonedDateTime("2021-02-08T12:00:00.000Z");

        List<UserTaskInstance> result1 = Arrays.asList(mockUserTaskInstance(TASK4, parseZonedDateTime("2021-02-08T13:00:00.001Z"), "Ready"),
                                                       mockUserTaskInstance(TASK5, parseZonedDateTime("2021-02-08T14:00:00.001Z"), "Ready"),
                                                       mockUserTaskInstance(TASK6, parseZonedDateTime("2021-02-08T14:00:00.001Z"), "Ready"));
        lenient().doReturn(result1).when(dataIndexServiceClient).findTasks(null, nextTime, "STARTED", true, 1, pageSize);
        nextTime = parseZonedDateTime("2021-02-08T14:00:00.000Z");

        List<UserTaskInstance> result2 = Arrays.asList(mockUserTaskInstance(TASK7, parseZonedDateTime("2021-02-08T15:00:00.001Z"), "Ready"),
                                                       mockUserTaskInstance(TASK8, parseZonedDateTime("2021-02-08T16:00:00.001Z"), "Ready"),
                                                       mockUserTaskInstance(TASK9, parseZonedDateTime("2021-02-08T17:00:00.001Z"), "Ready"));

        lenient().doReturn(result2).when(dataIndexServiceClient).findTasks(null, nextTime, "STARTED", true, 2, pageSize);
        nextTime = parseZonedDateTime("2021-02-08T17:00:00.000Z");

        lenient().doReturn(new ArrayList<>()).when(dataIndexServiceClient).findTasks(null, nextTime, "STARTED", true, 1, pageSize);

        TaskServiceConnector connector = new TaskServiceConnector(config, clientServices);
        List<UserTaskInstance> result = connector.findAllTasks(Collections.singletonList("Ready"), 3);
        assertThat(result.size()).isEqualTo(8);
        List<String> expectedTasks = Arrays.asList(TASK1, TASK2, TASK4, TASK5, TASK6, TASK7, TASK8, TASK9);
        assertThat(expectedTasks).isEqualTo(result.stream().map(UserTaskInstance::getId).collect(Collectors.toList()));
    }

    private UserTaskInstance mockUserTaskInstance(String taskId, ZonedDateTime started, String state) {
        UserTaskInstance userTaskInstance = new UserTaskInstance();
        userTaskInstance.setId(taskId);
        userTaskInstance.setStarted(started);
        userTaskInstance.setState(state);
        return userTaskInstance;
    }

    private static ZonedDateTime parseZonedDateTime(String value) {
        return ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
