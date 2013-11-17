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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.cayenne.util.Util;

public class EventSubjectTest extends TestCase {

    public void testIllegalArguments() {
        try {
            EventSubject.getSubject(null, "Subject");
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // OK
        }

        try {
            EventSubject.getSubject(Object.class, null);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // OK
        }

        try {
            EventSubject.getSubject(Object.class, "");
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // OK
        }
    }

    public void testEqualityOfClonedSubjects() throws Exception {
        EventSubject s1 = EventSubject.getSubject(EventSubjectTest.class, "MySubject");
        EventSubject s2 = (EventSubject) Util.cloneViaSerialization(s1);

        assertNotSame(s1, s2);
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    public void testIdenticalSubject() {
        EventSubject s1 = EventSubject.getSubject(EventSubjectTest.class, "MySubject");
        EventSubject s2 = EventSubject.getSubject(EventSubjectTest.class, "MySubject");
        Assert.assertSame(s1, s2);
    }

    public void testEqualityOfIdenticalSubjects() {
        EventSubject s1 = EventSubject.getSubject(EventSubjectTest.class, "MySubject");
        EventSubject s2 = EventSubject.getSubject(EventSubjectTest.class, "MySubject");
        Assert.assertEquals(s1, s2);
    }

    public void testEqualityOfSubjectsByDifferentOwner() {
        EventSubject s1 = EventSubject.getSubject(EventSubject.class, "MySubject");
        EventSubject s2 = EventSubject.getSubject(EventSubjectTest.class, "MySubject");
        Assert.assertFalse(s1.equals(s2));
    }

    public void testEqualityOfSubjectsByDifferentTopic() {
        EventSubject s1 = EventSubject.getSubject(EventSubjectTest.class, "Subject1");
        EventSubject s2 = EventSubject.getSubject(EventSubjectTest.class, "Subject2");
        Assert.assertFalse(s1.equals(s2));
    }

    public void testSubjectEqualsNull() {
        EventSubject s1 = EventSubject.getSubject(EventSubjectTest.class, "MySubject");
        Assert.assertFalse(s1.equals(null));
    }

    // TODO: (Andrus) This test can not be run reliably and in fact consistently
    // fails in some environments, since forcing GC at a certain time is not
    // guaranteed.
    /*
     * public void testSubjectGC() { EventSubject s =
     * EventSubject.getSubject(EventSubjectTst.class, "GCSubject"); long hash1 =
     * s.hashCode(); // try to make the subject go away s = null; System.gc();
     * System.gc(); s = EventSubject.getSubject(EventSubjectTst.class,
     * "GCSubject"); long hash2 = s.hashCode(); Assert.assertTrue(hash1 !=
     * hash2); }
     */

}
