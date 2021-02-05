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

package org.kie.kogito.taskassigning.service.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.kie.kogito.taskassigning.messaging.UserTaskEvent;
import org.kie.kogito.taskassigning.messaging.UserTaskEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Startup
public class BackpressureBufferedEventConsumer implements UserTaskEventConsumer {

    private static final String DEFERRED_HANDLE_TIMEOUT = "kogito.task-assigning.buffered-user-task-event-consumer.deferred-handle-timout.ms";

    private static final Logger LOGGER = LoggerFactory.getLogger(BackpressureBufferedEventConsumer.class);

    private List<UserTaskEvent> buffer = new ArrayList<>();

    private Semaphore deliverEventsResource = new Semaphore(0);

    private AtomicBoolean deliverEvents = new AtomicBoolean(false);

    private ReentrantLock lock = new ReentrantLock();

    @Inject
    @ConfigProperty(name = DEFERRED_HANDLE_TIMEOUT, defaultValue = "10000")
    long deferredHandleTimout;

    @PostConstruct
    void setUp() {
        LOGGER.info("Deferred message handle timeout was configured with, {} = {}", DEFERRED_HANDLE_TIMEOUT, deferredHandleTimout);
    }

    private Consumer<List<UserTaskEvent>> consumer = userTaskEvents -> {
        LOGGER.debug("The consumer is very happy!!!!");
    };

    @Override
    public CompletionStage<Void> consume(Message<UserTaskEvent> message) {
        return CompletableFuture.runAsync(() -> handleEventProcessing(message.getPayload()))
                .thenRun(() -> {
                    LOGGER.debug("Acknowledging eventId: {}", message.getPayload().getId());
                    message.ack();
                })
                .exceptionally(t -> {
                    //TODO...
                    //En realidad acá que hago????
                    LOGGER.error("Handling event processing error, eventId: {}", t.getMessage(), t);
                    return null;
                });
    }

    public void resume() {
        lock.lock();
        LOGGER.debug("resume() was invoked with current buffer.size: {}", buffer.size());
        try {
            if (deliverEvents.compareAndSet(false, true)) {
                if (!buffer.isEmpty()) {
                    LOGGER.debug("there are buffer.size: {} cached messages for deliver to consumer", buffer.size());
                    deliverToConsumer();
                }
                deliverEventsResource.release();
            }
        } finally {
            lock.unlock();
        }
    }

    public void pause() {
        lock.lock();
        LOGGER.debug("pause() was invoked with current buffer.size: {}", buffer.size());
        try {
            deliverEvents.set(false);
            deliverEventsResource.drainPermits();
        } finally {
            lock.unlock();
        }
    }

    private void handleEventProcessing(UserTaskEvent event) {
        lock.lock();
        LOGGER.debug("handle arrived event, eventId: {}, buffer.size: {}, deliverEvents: {}", event.getId(), buffer.size(), deliverEvents);
        if (deliverEvents.get()) {
            checkAndDeliverEvents(event);
            lock.unlock();
        } else {
            try {
                lock.unlock();
                deliverEventsResource.tryAcquire(deferredHandleTimout, TimeUnit.MILLISECONDS);
                checkAndDeliverEvents(event);
            } catch (InterruptedException e) {
                LOGGER.error("an error was produced while waiting for the next event to arrive", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void checkAndDeliverEvents(UserTaskEvent event) {
        try {
            lock.lock();
            LOGGER.debug("check and deliver eventId: {}, deliverEvents; {}", event.getId(), deliverEvents);
            buffer.add(event);
            if (deliverEvents.get()) {
                deliverToConsumer();
            }
        } finally {
            lock.unlock();
        }
    }

    private void deliverToConsumer() {
        List<String> eventIds = buffer.stream().map(UserTaskEvent::getId).collect(Collectors.toList());
        LOGGER.debug("Delivering events to consumer, buffer.size: {}, eventIds: {}", buffer.size(), eventIds);
        consumer.accept(buffer);
        buffer.clear();
    }
}