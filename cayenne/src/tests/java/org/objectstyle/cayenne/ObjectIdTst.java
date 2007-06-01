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
package org.objectstyle.cayenne;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class ObjectIdTst extends CayenneTestCase {

    public void testObjEntityName() throws Exception {
        Class class1 = Number.class;
        ObjectId oid = new ObjectId(class1, null);
        assertEquals(class1, oid.getObjClass());
    }

    public void testEquals0() throws Exception {
        Class class1 = Number.class;
        ObjectId oid1 = new ObjectId(class1, null);
        assertEquals(oid1, oid1);
        assertEquals(oid1.hashCode(), oid1.hashCode());
    }

    public void testEquals1() throws Exception {
        Class class1 = Number.class;
        ObjectId oid1 = new ObjectId(class1, null);
        ObjectId oid2 = new ObjectId(class1, null);
        assertEquals(oid1, oid2);
        assertEquals(oid1.hashCode(), oid2.hashCode());
    }

    public void testEquals2() throws Exception {
        Class class1 = Number.class;
        Map hm = new HashMap();
        ObjectId oid1 = new ObjectId(class1, hm);
        ObjectId oid2 = new ObjectId(class1, hm);
        assertEquals(oid1, oid2);
        assertEquals(oid1.hashCode(), oid2.hashCode());
    }

    public void testEquals3() throws Exception {
        Class class1 = Number.class;
        String pknm = "xyzabc";

        Map hm1 = new HashMap();
        hm1.put(pknm, "123");

        Map hm2 = new HashMap();
        hm2.put(pknm, "123");

        ObjectId oid1 = new ObjectId(class1, hm1);
        ObjectId oid2 = new ObjectId(class1, hm2);
        assertEquals(oid1, oid2);
        assertEquals(oid1.hashCode(), oid2.hashCode());
    }

    public void testEquals4() throws Exception {
        Class class1 = Number.class;
        String pknm = "xyzabc";

        Map hm1 = new HashMap();
        hm1.put(pknm, new Integer(1000));

        ObjectId ref = new ObjectId(class1, hm1);
        ObjectId oid = new ObjectId(class1, pknm, 1000);
        assertEquals(ref, oid);
        assertEquals(ref.hashCode(), oid.hashCode());
    }

    /**
     * This is a test case reproducing conditions for the bug "8458963".
     */
    public void testEquals5() throws Exception {
        Class class1 = Number.class;

        Map hm1 = new HashMap();
        hm1.put("key1", new Integer(1));
        hm1.put("key2", new Integer(11));

        Map hm2 = new HashMap();
        hm2.put("key1", new Integer(11));
        hm2.put("key2", new Integer(1));

        ObjectId ref = new ObjectId(class1, hm1);
        ObjectId oid = new ObjectId(class1, hm2);
        assertFalse(ref.equals(oid));
    }

    /**
        * Multiple key objectId
        */
    public void testEquals6() throws Exception {
        Class class1 = Number.class;

        Map hm1 = new HashMap();
        hm1.put("key1", new Integer(1));
        hm1.put("key2", new Integer(2));

        Map hm2 = new HashMap();
        hm2.put("key1", new Integer(1));
        hm2.put("key2", new Integer(2));

        ObjectId ref = new ObjectId(class1, hm1);
        ObjectId oid = new ObjectId(class1, hm2);
        assertTrue(ref.equals(oid));
        assertEquals(ref.hashCode(), oid.hashCode());
    }

    /**
     * Checks that hashCode works even if keys
     * are inserted in the map in a different order...
     */
    public void testEquals7() throws Exception {
        Class class1 = Number.class;
        
        // create maps with guaranteed iteration order

        Map hm1 = new LinkedMap();
        hm1.put("KEY1", new Integer(1));
        hm1.put("KEY2", new Integer(2));

        Map hm2 = new LinkedMap();
        // put same keys but in different order 
        hm2.put("KEY2", new Integer(2));
        hm2.put("KEY1", new Integer(1));

        ObjectId ref = new ObjectId(class1, hm1);
        ObjectId oid = new ObjectId(class1, hm2);
        assertTrue(ref.equals(oid));
        assertEquals(ref.hashCode(), oid.hashCode());
    }

    public void testEqualsBinaryKey() throws Exception {
        Class class1 = Artist.class;

        Map hm1 = new HashMap();
        hm1.put("key1", new byte[] { 3, 4, 10, -1 });

        Map hm2 = new HashMap();
        hm2.put("key1", new byte[] { 3, 4, 10, -1 });

        ObjectId ref = new ObjectId(class1, hm1);
        ObjectId oid = new ObjectId(class1, hm2);
        assertEquals(ref.hashCode(), oid.hashCode());
        assertTrue(ref.equals(oid));
    }

    public void testEqualsNull() {
        ObjectId o = new ObjectId(Artist.class, "ARTIST_ID", 42);
        assertFalse(o.equals(null));
    }

    public void testIdAsMapKey() throws Exception {
        Map map = new HashMap();
        Object o1 = new Object();

        Class class1 = Number.class;
        String pknm = "xyzabc";

        Map hm1 = new HashMap();
        hm1.put(pknm, "123");

        Map hm2 = new HashMap();
        hm2.put(pknm, "123");

        ObjectId oid1 = new ObjectId(class1, hm1);
        ObjectId oid2 = new ObjectId(class1, hm2);

        map.put(oid1, o1);
        assertSame(o1, map.get(oid2));
    }

    public void testNotEqual1() throws Exception {
        Class class1 = Number.class;
        Class class2 = Boolean.class;

        ObjectId oid1 = new ObjectId(class1, null);
        ObjectId oid2 = new ObjectId(class2, null);
        assertFalse(oid1.equals(oid2));
    }

    public void testNotEqual2() throws Exception {
        Class class1 = Number.class;

        Map hm1 = new HashMap();
        hm1.put("pk1", "123");

        Map hm2 = new HashMap();
        hm2.put("pk2", "123");

        ObjectId oid1 = new ObjectId(class1, hm1);
        ObjectId oid2 = new ObjectId(class1, hm2);
        assertFalse(oid1.equals(oid2));
    }
}
