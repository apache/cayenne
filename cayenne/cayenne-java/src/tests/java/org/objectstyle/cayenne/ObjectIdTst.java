/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.collections.map.LinkedMap;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.remote.hessian.service.HessianUtil;
import org.objectstyle.cayenne.util.Util;

public class ObjectIdTst extends TestCase {

    public void testConstructor() {
        ObjectId temp1 = new ObjectId("e");
        assertEquals("e", temp1.getEntityName());
        assertTrue(temp1.isTemporary());
        assertNotNull(temp1.getKey());

        byte[] key = new byte[] {
                1, 2, 3
        };
        ObjectId temp2 = new ObjectId("e1", key);
        assertEquals("e1", temp2.getEntityName());
        assertTrue(temp2.isTemporary());
        assertSame(key, temp2.getKey());
    }

    /**
     * @deprecated since 1.2
     */
    public void testClassConstructor() {
        ObjectId temp1 = new ObjectId(Number.class);
        assertEquals("Number", temp1.getEntityName());
    }

    public void testSerializabilityTemp() throws Exception {
        ObjectId temp1 = new ObjectId("e");
        ObjectId temp2 = (ObjectId) Util.cloneViaSerialization(temp1);

        assertTrue(temp1.isTemporary());
        assertNotSame(temp1, temp2);
        assertEquals(temp1, temp2);
    }

    public void testSerializabilityPerm() throws Exception {
        ObjectId perm1 = new ObjectId("e", "a", "b");

        // make sure hashcode is resolved
        int h = perm1.hashCode();
        assertEquals(h, perm1.hashCode);
        assertTrue(perm1.hashCode != 0);

        ObjectId perm2 = (ObjectId) Util.cloneViaSerialization(perm1);

        // make sure hashCode is reset to 0
        assertTrue(perm2.hashCode == 0);

        assertFalse(perm2.isTemporary());
        assertNotSame(perm1, perm2);
        assertEquals(perm1, perm2);
    }

    public void testHessianSerializabilityTemp() throws Exception {
        ObjectId temp1 = new ObjectId("e");

        // make sure hashcode is resolved
        int h = temp1.hashCode();
        assertEquals(h, temp1.hashCode);
        assertTrue(temp1.hashCode != 0);

        ObjectId temp2 = (ObjectId) HessianUtil.cloneViaClientServerSerialization(
                temp1,
                new EntityResolver());

        // make sure hashCode is reset to 0
        assertTrue(temp2.hashCode == 0);

        assertTrue(temp1.isTemporary());
        assertNotSame(temp1, temp2);
        assertEquals(temp1, temp2);
    }

    public void testHessianSerializabilityPerm() throws Exception {
        ObjectId perm1 = new ObjectId("e", "a", "b");

        // make sure hashcode is resolved
        int h = perm1.hashCode();
        assertEquals(h, perm1.hashCode);
        assertTrue(perm1.hashCode != 0);

        ObjectId perm2 = (ObjectId) HessianUtil.cloneViaClientServerSerialization(
                perm1,
                new EntityResolver());

        // make sure hashCode is reset to 0
        assertTrue(perm2.hashCode == 0);

        assertFalse(perm2.isTemporary());
        assertNotSame(perm1, perm2);
        assertEquals(perm1, perm2);
    }

    public void testHessianSerializabilityPerm1() throws Exception {
        // test serializing an id created with unmodifiable map

        Map id = Collections.unmodifiableMap(Collections.singletonMap("a", "b"));
        ObjectId perm1 = new ObjectId("e", id);
        ObjectId perm2 = (ObjectId) HessianUtil.cloneViaClientServerSerialization(
                perm1,
                new EntityResolver());

        assertFalse(perm2.isTemporary());
        assertNotSame(perm1, perm2);
        assertEquals(perm1, perm2);
    }

    public void testEquals0() {
        ObjectId oid1 = new ObjectId("TE");
        assertEquals(oid1, oid1);
        assertEquals(oid1.hashCode(), oid1.hashCode());
    }

    public void testEquals1() {
        ObjectId oid1 = new ObjectId("T", "a", "b");
        ObjectId oid2 = new ObjectId("T", "a", "b");
        assertEquals(oid1, oid2);
        assertEquals(oid1.hashCode(), oid2.hashCode());
    }

    public void testEquals2() {
        Map hm = new HashMap();
        ObjectId oid1 = new ObjectId("T", hm);
        ObjectId oid2 = new ObjectId("T", hm);
        assertEquals(oid1, oid2);
        assertEquals(oid1.hashCode(), oid2.hashCode());
    }

    public void testEquals3() {
        String pknm = "xyzabc";

        Map hm1 = new HashMap();
        hm1.put(pknm, "123");

        Map hm2 = new HashMap();
        hm2.put(pknm, "123");

        ObjectId oid1 = new ObjectId("T", hm1);
        ObjectId oid2 = new ObjectId("T", hm2);
        assertEquals(oid1, oid2);
        assertEquals(oid1.hashCode(), oid2.hashCode());
    }

    /**
     * This is a test case reproducing conditions for the bug "8458963".
     */
    public void testEquals5() {

        Map hm1 = new HashMap();
        hm1.put("key1", new Integer(1));
        hm1.put("key2", new Integer(11));

        Map hm2 = new HashMap();
        hm2.put("key1", new Integer(11));
        hm2.put("key2", new Integer(1));

        ObjectId ref = new ObjectId("T", hm1);
        ObjectId oid = new ObjectId("T", hm2);
        assertFalse(ref.equals(oid));
    }

    /**
     * Multiple key objectId
     */
    public void testEquals6() {

        Map hm1 = new HashMap();
        hm1.put("key1", new Integer(1));
        hm1.put("key2", new Integer(2));

        Map hm2 = new HashMap();
        hm2.put("key1", new Integer(1));
        hm2.put("key2", new Integer(2));

        ObjectId ref = new ObjectId("T", hm1);
        ObjectId oid = new ObjectId("T", hm2);
        assertTrue(ref.equals(oid));
        assertEquals(ref.hashCode(), oid.hashCode());
    }

    /**
     * Checks that hashCode works even if keys are inserted in the map in a different
     * order...
     */
    public void testEquals7() {

        // create maps with guaranteed iteration order

        Map hm1 = new LinkedMap();
        hm1.put("KEY1", new Integer(1));
        hm1.put("KEY2", new Integer(2));

        Map hm2 = new LinkedMap();
        // put same keys but in different order
        hm2.put("KEY2", new Integer(2));
        hm2.put("KEY1", new Integer(1));

        ObjectId ref = new ObjectId("T", hm1);
        ObjectId oid = new ObjectId("T", hm2);
        assertTrue(ref.equals(oid));
        assertEquals(ref.hashCode(), oid.hashCode());
    }

    public void testEqualsBinaryKey() {

        Map hm1 = new HashMap();
        hm1.put("key1", new byte[] {
                3, 4, 10, -1
        });

        Map hm2 = new HashMap();
        hm2.put("key1", new byte[] {
                3, 4, 10, -1
        });

        ObjectId ref = new ObjectId("T", hm1);
        ObjectId oid = new ObjectId("T", hm2);
        assertEquals(ref.hashCode(), oid.hashCode());
        assertTrue(ref.equals(oid));
    }

    public void testEqualsNull() {
        ObjectId o = new ObjectId("T", "ARTIST_ID", new Integer(42));
        assertFalse(o.equals(null));
    }

    public void testIdAsMapKey() {
        Map map = new HashMap();
        Object o1 = new Object();

        String pknm = "xyzabc";

        Map hm1 = new HashMap();
        hm1.put(pknm, "123");

        Map hm2 = new HashMap();
        hm2.put(pknm, "123");

        ObjectId oid1 = new ObjectId("T", hm1);
        ObjectId oid2 = new ObjectId("T", hm2);

        map.put(oid1, o1);
        assertSame(o1, map.get(oid2));
    }

    public void testNotEqual1() {

        ObjectId oid1 = new ObjectId("T1");
        ObjectId oid2 = new ObjectId("T2");
        assertFalse(oid1.equals(oid2));
    }

    public void testNotEqual2() {

        Map hm1 = new HashMap();
        hm1.put("pk1", "123");

        Map hm2 = new HashMap();
        hm2.put("pk2", "123");

        ObjectId oid1 = new ObjectId("T", hm1);
        ObjectId oid2 = new ObjectId("T", hm2);
        assertFalse(oid1.equals(oid2));
    }
    
    /**
     * Test different numeric types.
     */
    public void testEquals8() {

        // create maps with guaranteed iteration order

        Map hm1 = new LinkedMap();
        hm1.put("KEY1", new Integer(1));
        hm1.put("KEY2", new Integer(2));

        Map hm2 = new LinkedMap();
        // put same keys but in different order
        hm2.put("KEY2", new BigDecimal(2.00));
        hm2.put("KEY1", new Long(1));

        ObjectId ref = new ObjectId("T", hm1);
        ObjectId oid = new ObjectId("T", hm2);
        assertTrue(ref.equals(oid));
        assertEquals(ref.hashCode(), oid.hashCode());
    }
}
