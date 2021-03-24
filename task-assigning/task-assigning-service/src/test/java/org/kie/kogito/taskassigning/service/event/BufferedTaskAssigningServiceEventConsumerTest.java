/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.taskassigning.service.event;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BufferedTaskAssigningServiceEventConsumerTest {
    /*
     * private BufferedTaskAssigningServiceEventConsumer userTaskEventConsumer;
     * 
     * @Mock
     * private Consumer<List<UserTaskEvent>> consumer;
     * 
     * @Captor
     * private ArgumentCaptor<List<UserTaskEvent>> eventsCaptor;
     * 
     * @Mock
     * private UserTaskEvent event1;
     * 
     * @Mock
     * private UserTaskEvent event2;
     * 
     * @BeforeEach
     * void setUp() {
     * userTaskEventConsumer = new BufferedTaskAssigningServiceEventConsumer();
     * userTaskEventConsumer.setConsumer(consumer);
     * }
     * 
     * @Test
     * void pause() {
     * userTaskEventConsumer.pause();
     * userTaskEventConsumer.accept(event1);
     * userTaskEventConsumer.accept(event2);
     * verify(consumer, never()).accept(anyList());
     * }
     * 
     * @Test
     * void resume() {
     * userTaskEventConsumer.pause();
     * userTaskEventConsumer.accept(event1);
     * userTaskEventConsumer.accept(event2);
     * verify(consumer, never()).accept(anyList());
     * userTaskEventConsumer.resume();
     * verify(consumer).accept(eventsCaptor.capture());
     * assertThat(eventsCaptor.getValue()).isNotNull();
     * assertThat(eventsCaptor.getValue()).containsExactlyElementsOf(Arrays.asList(event1, event2));
     * }
     * 
     * @Test
     * void pollEvents() {
     * userTaskEventConsumer.pause();
     * userTaskEventConsumer.accept(event1);
     * userTaskEventConsumer.accept(event2);
     * verify(consumer, never()).accept(anyList());
     * List<UserTaskEvent> events = userTaskEventConsumer.pollEvents();
     * assertThat(events)
     * .hasSize(2)
     * .containsExactlyElementsOf(Arrays.asList(event1, event2));
     * userTaskEventConsumer.resume();
     * verify(consumer, never()).accept(anyList());
     * }
     * 
     * @Test
     * void queuedEvents() {
     * userTaskEventConsumer.pause();
     * userTaskEventConsumer.accept(event1);
     * userTaskEventConsumer.accept(event2);
     * assertThat(userTaskEventConsumer.queuedEvents()).isEqualTo(2);
     * }
     */

}
