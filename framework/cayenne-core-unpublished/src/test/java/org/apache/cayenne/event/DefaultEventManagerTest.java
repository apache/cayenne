/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.event;

import java.util.EventListener;
import java.util.EventObject;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.cayenne.test.parallel.ParallelTestContainer;

public class DefaultEventManagerTest extends TestCase implements EventListener {

    // used for counting received events on the class
    public static int numberOfReceivedEventsForClass;

    // used for counting received events per listener instance
    public int numberOfReceivedEvents;

    // the event manager used for testing
    private EventManager eventManager;

    @Override
    public void setUp() {
        eventManager = new DefaultEventManager();
        numberOfReceivedEvents = 0;
        numberOfReceivedEventsForClass = 0;
    }

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

    public void testNullListener() {
        try {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
            eventManager.addListener(null, null, null, subject);
            Assert.fail();
        }

        catch (IllegalArgumentException ia) {
            // expected
        }
    }

    public void testNullNotification() {
        // null notification
        try {
            eventManager.addListener(this, "testNullObserver", CayenneEvent.class, null);
            Assert.fail();
        }

        catch (IllegalArgumentException e) {
            // expected
        }

        // invalid event class
        try {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "");
            eventManager.addListener(this, "testNullObserver", null, subject);
            Assert.fail();
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
            Assert.fail();
        }

        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testNonexistingMethod() {
        try {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
            eventManager.addListener(
                    this,
                    "thisMethodDoesNotExist",
                    CayenneEvent.class,
                    subject);
            Assert.fail();
        }

        catch (RuntimeException e) {
            // expected
        }
    }

    public void testInvalidArgumentTypes() {
        try {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
            eventManager.addListener(
                    this,
                    "seeTheWrongMethod",
                    CayenneEvent.class,
                    subject);
            Assert.fail();
        }

        catch (RuntimeException e) {
            // expected
        }
    }

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
        Assert.assertEquals(0, numberOfReceivedEventsForClass);
    }

    public void testValidSubclassOfRegisteredEventClass() throws Exception {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
        eventManager.postEvent(new MyCayenneEvent(this), subject);

        assertReceivedEvents(1, this);
    }

    public void testWrongRegisteredEventClass() throws Exception {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");

        // we register a method that takes a CayenneEvent or subclass thereof..
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);

        // ..but post a subclass of EventObject that is not compatible with CayenneEvent
        eventManager.postEvent(new EventObject(this), subject);

        assertReceivedEvents(0, this);
    }

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

    public void testRemoveOnEmptyList() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        Assert.assertFalse(eventManager.removeListener(this, subject));
    }

    public void testRemoveOnNullSubject() {
        Assert.assertFalse(eventManager.removeListener(this, null));
    }

    public void testRemoveFromDefaultQueue() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
        Assert.assertTrue(eventManager.removeListener(this, subject));
        Assert.assertFalse(eventManager.removeListener(this));
    }

    public void testRemoveSpecificQueue() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(
                this,
                "seeNotification",
                CayenneEvent.class,
                subject,
                this);
        Assert.assertTrue(eventManager.removeListener(this, subject));
        Assert.assertFalse(eventManager.removeListener(this));
    }

    public void testRemoveSpecificSender() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(
                this,
                "seeNotification",
                CayenneEvent.class,
                subject,
                this);
        Assert.assertTrue(eventManager.removeListener(this, subject, this));
        Assert.assertFalse(eventManager.removeListener(this));
    }

    public void testRemoveNullSender() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(
                this,
                "seeNotification",
                CayenneEvent.class,
                subject,
                this);
        Assert.assertTrue(eventManager.removeListener(this, subject, null));
        Assert.assertFalse(eventManager.removeListener(this));
    }

    public void testRemoveNonexistingSender() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(
                this,
                "seeNotification",
                CayenneEvent.class,
                subject,
                this);
        Assert.assertFalse(eventManager.removeListener(this, subject, "foo"));
        Assert.assertTrue(eventManager.removeListener(this));
    }

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

        Assert.assertTrue(eventManager.removeListener(this));
        Assert.assertFalse(eventManager.removeListener(this));
        Assert.assertFalse(eventManager.removeListener(this, subject1));
        Assert.assertFalse(eventManager.removeListener(this, subject2));
        Assert.assertFalse(eventManager.removeListener(this, subject3));
    }

    public void testSubjectGarbageCollection() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);

        // let go of the subject & (hopefully) release queue
        subject = null;
        System.gc();
        System.gc();

        Assert.assertFalse(eventManager.removeListener(this));
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
