/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.event;

import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EventListener;
import java.util.EventObject;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultEventManagerTest implements EventListener {

    // used for counting received events on the class
    public static int numberOfReceivedEventsForClass;

    // used for counting received events per listener instance
    public int numberOfReceivedEvents;

    // the event manager used for testing
    private EventManager eventManager;

    @BeforeEach
    public void setUp() {
        eventManager = new DefaultEventManager();
        numberOfReceivedEvents = 0;
        numberOfReceivedEventsForClass = 0;
    }

    @AfterEach
    public void tearDown() {
        ((DefaultEventManager)eventManager).shutdown();
    }

    @Test
    public void subjectListenerWouldRegisterListener() {

        MockListener listener = new MockListener(eventManager);
        eventManager.addListener(
                listener,
                "processEvent",
                EventObject.class,
                MockListener.mockSubject);

        // test concurrent modification of the queue ... on event listener would attempt
        // adding another listener

        // add more than one listener to see that dispatch can proceed after one of the
        // listeners recats to event

        eventManager.addListener(
                new MockListener(eventManager),
                "processEvent",
                EventObject.class,
                MockListener.mockSubject);

        eventManager.postEvent(new EventObject(this), MockListener.mockSubject);
    }

    @Test
    public void objectListenerWouldRegisterListener() {

        MockListener listener = new MockListener(eventManager, this);
        eventManager.addListener(
                listener,
                "processEvent",
                EventObject.class,
                MockListener.mockSubject,
                this);

        // test concurrent modification of the queue ... on event listener would attempt
        // adding another listener

        // add more than one listener to see that dispatch can proceed after one of the
        // listeners recats to event

        eventManager.addListener(
                new MockListener(eventManager, this),
                "processEvent",
                EventObject.class,
                MockListener.mockSubject,
                this);

        eventManager.postEvent(new EventObject(this), MockListener.mockSubject);
    }

    @Test
    public void nullListener() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
        assertThrows(IllegalArgumentException.class, () -> eventManager.addListener(null, null, null, subject));
    }

    @Test
    public void nullNotification() {
        // null notification
        assertThrows(IllegalArgumentException.class,
                () -> eventManager.addListener(this, "testNullObserver", CayenneEvent.class, null));

        // invalid event class
        assertThrows(IllegalArgumentException.class, () -> {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "");
            eventManager.addListener(this, "testNullObserver", null, subject);
        });

        // empty string notification
        assertThrows(IllegalArgumentException.class, () -> {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "");
            eventManager.addListener(this, "testNullObserver", CayenneEvent.class, subject);
        });
    }

    @Test
    public void nonexistingMethod() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
        assertThrows(RuntimeException.class,
                () -> eventManager.addListener(this, "thisMethodDoesNotExist", CayenneEvent.class, subject));
    }

    @Test
    public void invalidArgumentTypes() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
        assertThrows(RuntimeException.class,
                () -> eventManager.addListener(this, "seeTheWrongMethod", CayenneEvent.class, subject));
    }

    @Test
    public void nonretainedListener() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(
                new DefaultEventManagerTest(),
                "seeNotification",
                CayenneEvent.class,
                subject);

        // (hopefully) make the listener go away
        System.gc();
        System.gc();

        eventManager.postEvent(new CayenneEvent(this), subject);
        assertEquals(0, numberOfReceivedEventsForClass);
    }

    @Test
    public void validSubclassOfRegisteredEventClass() throws Exception {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
        eventManager.postEvent(new MyCayenneEvent(this), subject);

        assertReceivedEvents(1, this);
    }

    @Test
    public void wrongRegisteredEventClass() throws Exception {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");

        // we register a method that takes a CayenneEvent or subclass thereof..
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);

        // ..but post a subclass of EventObject that is not compatible with CayenneEvent
        eventManager.postEvent(new EventObject(this), subject);

        assertReceivedEvents(0, this);
    }

    @Test
    public void successfulNotificationDefaultSender() throws Exception {
        DefaultEventManagerTest listener1 = this;
        DefaultEventManagerTest listener2 = new DefaultEventManagerTest();

        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(
                listener1,
                "seeNotification",
                CayenneEvent.class,
                subject);
        eventManager.addListener(
                listener2,
                "seeNotification",
                CayenneEvent.class,
                subject);

        eventManager.postEvent(new CayenneEvent(this), subject);

        assertReceivedEvents(1, listener1);
        assertReceivedEvents(1, listener2);
        assertReceivedEventsForClass(2);
    }

    @Test
    public void successfulNotificationIndividualSender() throws Exception {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(
                this,
                "seeNotification",
                CayenneEvent.class,
                subject,
                this);
        eventManager.postEvent(new CayenneEvent(this), subject);

        assertReceivedEvents(1, this);
        assertReceivedEventsForClass(1);
    }

    @Test
    public void successfulNotificationIndividualSenderTwice() throws Exception {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
        eventManager.addListener(
                this,
                "seeNotification",
                CayenneEvent.class,
                subject,
                this);
        eventManager.postEvent(new CayenneEvent(this), subject);

        assertReceivedEvents(2, this);
        assertReceivedEventsForClass(2);
    }

    @Test
    public void successfulNotificationBothDefaultAndIndividualSender()
            throws Exception {
        DefaultEventManagerTest listener1 = this;
        DefaultEventManagerTest listener2 = new DefaultEventManagerTest();

        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(
                listener1,
                "seeNotification",
                CayenneEvent.class,
                subject,
                listener1);
        eventManager.addListener(
                listener2,
                "seeNotification",
                CayenneEvent.class,
                subject);

        eventManager.postEvent(new CayenneEvent(this), subject);

        assertReceivedEvents(1, listener1);
        assertReceivedEvents(1, listener2);
        assertReceivedEventsForClass(2);
    }

    @Test
    public void removeOnEmptyList() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        assertFalse(eventManager.removeListener(this, subject));
    }

    @Test
    public void removeOnNullSubject() {
        assertFalse(eventManager.removeListener(this, null));
    }

    @Test
    public void removeFromDefaultQueue() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
        assertTrue(eventManager.removeListener(this, subject));
        assertFalse(eventManager.removeListener(this));
    }

    @Test
    public void removeSpecificQueue() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(
                this,
                "seeNotification",
                CayenneEvent.class,
                subject,
                this);
        assertTrue(eventManager.removeListener(this, subject));
        assertFalse(eventManager.removeListener(this));
    }

    @Test
    public void removeSpecificSender() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(
                this,
                "seeNotification",
                CayenneEvent.class,
                subject,
                this);
        assertTrue(eventManager.removeListener(this, subject, this));
        assertFalse(eventManager.removeListener(this));
    }

    @Test
    public void removeNullSender() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(
                this,
                "seeNotification",
                CayenneEvent.class,
                subject,
                this);
        assertTrue(eventManager.removeListener(this, subject, null));
        assertFalse(eventManager.removeListener(this));
    }

    @Test
    public void removeNonexistingSender() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(
                this,
                "seeNotification",
                CayenneEvent.class,
                subject,
                this);
        assertFalse(eventManager.removeListener(this, subject, "foo"));
        assertTrue(eventManager.removeListener(this));
    }

    @Test
    public void removeAll() {
        EventSubject subject1 = EventSubject.getSubject(this.getClass(), "XXX1");
        EventSubject subject2 = EventSubject.getSubject(this.getClass(), "XXX2");
        EventSubject subject3 = EventSubject.getSubject(this.getClass(), "XXX3");
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject1);
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject2);
        eventManager.addListener(
                this,
                "seeNotification",
                CayenneEvent.class,
                subject3,
                this);

        assertTrue(eventManager.removeListener(this));
        assertFalse(eventManager.removeListener(this));
        assertFalse(eventManager.removeListener(this, subject1));
        assertFalse(eventManager.removeListener(this, subject2));
        assertFalse(eventManager.removeListener(this, subject3));
    }

    @Test
    public void subjectGarbageCollection() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);

        // let go of the subject & (hopefully) release queue
        subject = null;
        System.gc();
        System.gc();

        assertFalse(eventManager.removeListener(this));
    }

    // notification method
    public void seeNotification(CayenneEvent event) {
        numberOfReceivedEvents++;
        numberOfReceivedEventsForClass++;
    }

    // allows just enough time for the event threads to run
    private static void assertReceivedEvents(
            final int expected,
            final DefaultEventManagerTest listener) throws Exception {

        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(expected, listener.numberOfReceivedEvents);
            }
        };
        helper.runTest(5000);
    }

    // allows just enough time for the event threads to run
    private static void assertReceivedEventsForClass(final int expected) throws Exception {
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(expected, numberOfReceivedEventsForClass);
            }
        };
        helper.runTest(5000);
    }

}

// dummy class to test for incompatible events
class MyCayenneEvent extends CayenneEvent {

    public MyCayenneEvent(EventListener l) {
        super(l);
    }
}
