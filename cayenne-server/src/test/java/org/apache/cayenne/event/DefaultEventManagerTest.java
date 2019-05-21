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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.EventListener;
import java.util.EventObject;

import static org.junit.Assert.*;

public class DefaultEventManagerTest implements EventListener {

    // used for counting received events on the class
    public static int numberOfReceivedEventsForClass;

    // used for counting received events per listener instance
    public int numberOfReceivedEvents;

    // the event manager used for testing
    private EventManager eventManager;

    @Before
    public void setUp() {
        eventManager = new DefaultEventManager();
        numberOfReceivedEvents = 0;
        numberOfReceivedEventsForClass = 0;
    }

    @After
    public void tearDown() {
        ((DefaultEventManager)eventManager).shutdown();
    }

    @Test
    public void testSubjectListenerWouldRegisterListener() {

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
    public void testObjectListenerWouldRegisterListener() {

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
    public void testNullListener() {
        try {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
            eventManager.addListener(null, null, null, subject);
            fail();
        }

        catch (IllegalArgumentException ia) {
            // expected
        }
    }

    @Test
    public void testNullNotification() {
        // null notification
        try {
            eventManager.addListener(this, "testNullObserver", CayenneEvent.class, null);
            fail();
        }

        catch (IllegalArgumentException e) {
            // expected
        }

        // invalid event class
        try {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "");
            eventManager.addListener(this, "testNullObserver", null, subject);
            fail();
        }

        catch (IllegalArgumentException e) {
            // expected
        }

        // empty string notification
        try {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "");
            eventManager.addListener(
                    this,
                    "testNullObserver",
                    CayenneEvent.class,
                    subject);
            fail();
        }

        catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testNonexistingMethod() {
        try {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
            eventManager.addListener(
                    this,
                    "thisMethodDoesNotExist",
                    CayenneEvent.class,
                    subject);
            fail();
        }

        catch (RuntimeException e) {
            // expected
        }
    }

    @Test
    public void testInvalidArgumentTypes() {
        try {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
            eventManager.addListener(
                    this,
                    "seeTheWrongMethod",
                    CayenneEvent.class,
                    subject);
            fail();
        }

        catch (RuntimeException e) {
            // expected
        }
    }

    @Test
    public void testNonretainedListener() {
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
    public void testValidSubclassOfRegisteredEventClass() throws Exception {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
        eventManager.postEvent(new MyCayenneEvent(this), subject);

        assertReceivedEvents(1, this);
    }

    @Test
    public void testWrongRegisteredEventClass() throws Exception {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");

        // we register a method that takes a CayenneEvent or subclass thereof..
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);

        // ..but post a subclass of EventObject that is not compatible with CayenneEvent
        eventManager.postEvent(new EventObject(this), subject);

        assertReceivedEvents(0, this);
    }

    @Test
    public void testSuccessfulNotificationDefaultSender() throws Exception {
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
    public void testSuccessfulNotificationIndividualSender() throws Exception {
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
    public void testSuccessfulNotificationIndividualSenderTwice() throws Exception {
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
    public void testSuccessfulNotificationBothDefaultAndIndividualSender()
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
    public void testRemoveOnEmptyList() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        assertFalse(eventManager.removeListener(this, subject));
    }

    @Test
    public void testRemoveOnNullSubject() {
        assertFalse(eventManager.removeListener(this, null));
    }

    @Test
    public void testRemoveFromDefaultQueue() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
        assertTrue(eventManager.removeListener(this, subject));
        assertFalse(eventManager.removeListener(this));
    }

    @Test
    public void testRemoveSpecificQueue() {
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
    public void testRemoveSpecificSender() {
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
    public void testRemoveNullSender() {
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
    public void testRemoveNonexistingSender() {
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
    public void testRemoveAll() {
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
    public void testSubjectGarbageCollection() {
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
