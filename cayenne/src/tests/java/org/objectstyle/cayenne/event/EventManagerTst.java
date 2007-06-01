/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.event;

import java.util.EventListener;
import java.util.EventObject;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.util.ThreadedTestHelper;

public class EventManagerTst extends CayenneTestCase implements EventListener {
    private static final Logger log = Logger.getLogger(EventManagerTst.class);

    // used for counting received events on the class
    public static int _numberOfReceivedEventsForClass;

    // used for counting received events per listener instance
    public int _numberOfReceivedEvents;

    // the event manager used for testing
    private EventManager _eventManager;

    public void setUp() throws Exception {
        _eventManager = new EventManager();
        _numberOfReceivedEvents = 0;
        _numberOfReceivedEventsForClass = 0;
    }

    public void testNullListener() throws Exception {
        try {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "hansi");
            _eventManager.addListener(null, null, null, subject);
            Assert.fail();
        }

        catch (IllegalArgumentException ia) {
            // expected
        }
    }

    public void testNullNotification() throws Exception {
        // null notification
        try {
            _eventManager.addListener(this, "testNullObserver", CayenneEvent.class, null);
            Assert.fail();
        }

        catch (IllegalArgumentException e) {
            // expected
        }

        // invalid event class
        try {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "");
            _eventManager.addListener(this, "testNullObserver", null, subject);
            Assert.fail();
        }

        catch (IllegalArgumentException e) {
            // expected
        }

        // empty string notification
        try {
            EventSubject subject = EventSubject.getSubject(this.getClass(), "");
            _eventManager.addListener(
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
            _eventManager.addListener(
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
            _eventManager.addListener(
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

    public void testNonretainedListener() throws NoSuchMethodException {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        _eventManager.addListener(
            new EventManagerTst(),
            "seeNotification",
            CayenneEvent.class,
            subject);

        // (hopefully) make the listener go away
        System.gc();
        System.gc();

        _eventManager.postEvent(new CayenneEvent(this), subject);
        Assert.assertEquals(0, _numberOfReceivedEventsForClass);
    }

    public void testValidSubclassOfRegisteredEventClass() throws Exception {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        _eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
        _eventManager.postEvent(new MyCayenneEvent(this), subject);

        assertReceivedEvents(1, this);
    }

    public void testWrongRegisteredEventClass() throws Exception {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");

        // we register a method that takes a CayenneEvent or subclass thereof.. 
        _eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);

        // ..but post a subclass of EventObject that is not compatible with CayenneEvent
        _eventManager.postEvent(new EventObject(this), subject);

        assertReceivedEvents(0, this);
    }

    public void testSuccessfulNotificationDefaultSender() throws Exception {
        EventManagerTst listener1 = this;
        EventManagerTst listener2 = new EventManagerTst();

        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        _eventManager.addListener(
            listener1,
            "seeNotification",
            CayenneEvent.class,
            subject);
        _eventManager.addListener(
            listener2,
            "seeNotification",
            CayenneEvent.class,
            subject);

        _eventManager.postEvent(new CayenneEvent(this), subject);

        assertReceivedEvents(1, listener1);
        assertReceivedEvents(1, listener2);
        assertReceivedEventsForClass(2);
    }

    public void testSuccessfulNotificationIndividualSender() throws Exception {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        _eventManager.addListener(
            this,
            "seeNotification",
            CayenneEvent.class,
            subject,
            this);
        _eventManager.postEvent(new CayenneEvent(this), subject);

        assertReceivedEvents(1, this);
        assertReceivedEventsForClass(1);
    }

    public void testSuccessfulNotificationIndividualSenderTwice() throws Exception {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        _eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
        _eventManager.addListener(
            this,
            "seeNotification",
            CayenneEvent.class,
            subject,
            this);
        _eventManager.postEvent(new CayenneEvent(this), subject);

        assertReceivedEvents(2, this);
        assertReceivedEventsForClass(2);
    }

    public void testSuccessfulNotificationBothDefaultAndIndividualSender()
        throws Exception {
        EventManagerTst listener1 = this;
        EventManagerTst listener2 = new EventManagerTst();

        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        _eventManager.addListener(
            listener1,
            "seeNotification",
            CayenneEvent.class,
            subject,
            listener1);
        _eventManager.addListener(
            listener2,
            "seeNotification",
            CayenneEvent.class,
            subject);

        _eventManager.postEvent(new CayenneEvent(this), subject);

        assertReceivedEvents(1, listener1);
        assertReceivedEvents(1, listener2);
        assertReceivedEventsForClass(2);
    }

    public void testRemoveOnEmptyList() {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        Assert.assertFalse(_eventManager.removeListener(this, subject));
    }

    public void testRemoveOnNullSubject() {
        Assert.assertFalse(_eventManager.removeListener(this, null));
    }

    public void testRemoveFromDefaultQueue() throws NoSuchMethodException {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        _eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);
        Assert.assertTrue(_eventManager.removeListener(this, subject));
        Assert.assertFalse(_eventManager.removeListener(this));
    }

    public void testRemoveSpecificQueue() throws NoSuchMethodException {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        _eventManager.addListener(
            this,
            "seeNotification",
            CayenneEvent.class,
            subject,
            this);
        Assert.assertTrue(_eventManager.removeListener(this, subject));
        Assert.assertFalse(_eventManager.removeListener(this));
    }

    public void testRemoveSpecificSender() throws NoSuchMethodException {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        _eventManager.addListener(
            this,
            "seeNotification",
            CayenneEvent.class,
            subject,
            this);
        Assert.assertTrue(_eventManager.removeListener(this, subject, this));
        Assert.assertFalse(_eventManager.removeListener(this));
    }

    public void testRemoveNullSender() throws NoSuchMethodException {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        _eventManager.addListener(
            this,
            "seeNotification",
            CayenneEvent.class,
            subject,
            this);
        Assert.assertTrue(_eventManager.removeListener(this, subject, null));
        Assert.assertFalse(_eventManager.removeListener(this));
    }

    public void testRemoveNonexistingSender() throws NoSuchMethodException {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        _eventManager.addListener(
            this,
            "seeNotification",
            CayenneEvent.class,
            subject,
            this);
        Assert.assertFalse(_eventManager.removeListener(this, subject, "foo"));
        Assert.assertTrue(_eventManager.removeListener(this));
    }

    public void testRemoveAll() throws NoSuchMethodException {
        EventSubject subject1 = EventSubject.getSubject(this.getClass(), "XXX1");
        EventSubject subject2 = EventSubject.getSubject(this.getClass(), "XXX2");
        EventSubject subject3 = EventSubject.getSubject(this.getClass(), "XXX3");
        _eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject1);
        _eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject2);
        _eventManager.addListener(
            this,
            "seeNotification",
            CayenneEvent.class,
            subject3,
            this);

        Assert.assertTrue(_eventManager.removeListener(this));
        Assert.assertFalse(_eventManager.removeListener(this));
        Assert.assertFalse(_eventManager.removeListener(this, subject1));
        Assert.assertFalse(_eventManager.removeListener(this, subject2));
        Assert.assertFalse(_eventManager.removeListener(this, subject3));
    }

    public void testSubjectGarbageCollection() throws NoSuchMethodException {
        EventSubject subject = EventSubject.getSubject(this.getClass(), "XXX");
        _eventManager.addListener(this, "seeNotification", CayenneEvent.class, subject);

        // let go of the subject & (hopefully) release queue
        subject = null;
        System.gc();
        System.gc();

        Assert.assertFalse(_eventManager.removeListener(this));
    }

    // notification method
    public void seeNotification(CayenneEvent event) {
        log.debug("seeNotification. source: " + event.getSource().getClass().getName());
        _numberOfReceivedEvents++;
        _numberOfReceivedEventsForClass++;
    }

    public void seeTheWrongMethod(int hansi) {
        log.debug("seeTheWrongMethod: " + hansi);
    }

    // allows just enough time for the event threads to run
    private static void assertReceivedEvents(
        final int expected,
        final EventManagerTst listener)
        throws Exception {

        ThreadedTestHelper helper = new ThreadedTestHelper() {
            protected void assertResult() throws Exception {
                assertEquals(expected, listener._numberOfReceivedEvents);
            }
        };
        helper.assertWithTimeout(5000);
    }

    // allows just enough time for the event threads to run
    private static void assertReceivedEventsForClass(final int expected)
        throws Exception {
        ThreadedTestHelper helper = new ThreadedTestHelper() {
            protected void assertResult() throws Exception {
                assertEquals(expected, _numberOfReceivedEventsForClass);
            }
        };
        helper.assertWithTimeout(5000);
    }
}

// dummy class to test for incompatible events 
class MyCayenneEvent extends CayenneEvent {
    public MyCayenneEvent(EventListener l) {
        super(l);
    }
}
