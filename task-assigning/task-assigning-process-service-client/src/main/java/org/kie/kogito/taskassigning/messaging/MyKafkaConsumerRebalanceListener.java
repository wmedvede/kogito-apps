/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.kogito.taskassigning.messaging;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;

@ApplicationScoped
@Named("ExampleConsumerRebalanceListener")
public class MyKafkaConsumerRebalanceListener implements KafkaConsumerRebalanceListener {


    private Consumer<?, ?> consumer;
    private Collection<TopicPartition> partitions;
    private boolean started = false;

    @Override
    public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        this.consumer = consumer;
        this.partitions = partitions;
        System.out.println("NO SEAS MALO");

        System.out.println("WMMMMMMMMMMMMMMMMMMM OnPartitions assigned: " + partitions + ", currentThread: "
                                   + Thread.currentThread().getId()
        + " thread name: " + Thread.currentThread().getName());
        System.out.println("consumer: " + consumer);
    }

    @Override
    public void onPartitionsRevoked(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {

        System.out.println("MMMMMMMMMMMMMMMMMMM OnPartitions revoked: " + partitions);
    }

    public void pause() {
        System.out.println("CCCCCCCCCCCCCCC pause in currentThread: "+ Thread.currentThread().getId());
        consumer.pause(partitions);
    }

    public void resume() {
        System.out.println("KKKKKKKKKKKKKK resume in currentThread: "+ Thread.currentThread().getId());

        consumer.resume(partitions);
    }
}
