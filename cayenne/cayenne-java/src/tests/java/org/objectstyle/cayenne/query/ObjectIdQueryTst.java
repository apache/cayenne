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
package org.objectstyle.cayenne.query;

import junit.framework.TestCase;

import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.remote.hessian.service.HessianUtil;
import org.objectstyle.cayenne.util.Util;

public class ObjectIdQueryTst extends TestCase {

    public void testConstructorObjectId() {

        ObjectId oid = new ObjectId("MockDataObject", "a", "b");
        ObjectIdQuery query = new ObjectIdQuery(oid);

        assertSame(oid, query.getObjectId());
    }

    public void testSerializability() throws Exception {
        ObjectId oid = new ObjectId("test", "a", "b");
        ObjectIdQuery query = new ObjectIdQuery(oid);

        Object o = Util.cloneViaSerialization(query);
        assertNotNull(o);
        assertTrue(o instanceof ObjectIdQuery);
        assertEquals(oid, ((ObjectIdQuery) o).getObjectId());
    }

    public void testSerializabilityWithHessian() throws Exception {
        ObjectId oid = new ObjectId("test", "a", "b");
        ObjectIdQuery query = new ObjectIdQuery(oid);

        Object o = HessianUtil.cloneViaClientServerSerialization(query, new EntityResolver());
        assertNotNull(o);
        assertTrue(o instanceof ObjectIdQuery);
        assertEquals(oid, ((ObjectIdQuery) o).getObjectId());
    }

    /**
     * Proper 'equals' and 'hashCode' implementations are important when mapping results
     * obtained in a QueryChain back to the query.
     */
    public void testEquals() throws Exception {
        ObjectIdQuery q1 = new ObjectIdQuery(new ObjectId("abc", "a", 1));
        ObjectIdQuery q2 = new ObjectIdQuery(new ObjectId("abc", "a", 1));
        ObjectIdQuery q3 = new ObjectIdQuery(new ObjectId("abc", "a", 3));
        ObjectIdQuery q4 = new ObjectIdQuery(new ObjectId("123", "a", 1));

        assertTrue(q1.equals(q2));
        assertEquals(q1.hashCode(), q2.hashCode());

        assertFalse(q1.equals(q3));
        assertFalse(q1.hashCode() == q3.hashCode());

        assertFalse(q1.equals(q4));
        assertFalse(q1.hashCode() == q4.hashCode());
    }

    public void testMetadata() {
        ObjectIdQuery q1 = new ObjectIdQuery(
                new ObjectId("abc", "a", 1),
                true,
                ObjectIdQuery.CACHE_REFRESH);

        assertTrue(q1.isFetchAllowed());
        assertTrue(q1.isFetchMandatory());

        QueryMetadata md1 = q1.getMetaData(null);
        assertTrue(md1.isFetchingDataRows());

        ObjectIdQuery q2 = new ObjectIdQuery(
                new ObjectId("abc", "a", 1),
                false,
                ObjectIdQuery.CACHE);

        assertTrue(q2.isFetchAllowed());
        assertFalse(q2.isFetchMandatory());

        QueryMetadata md2 = q2.getMetaData(null);
        assertFalse(md2.isFetchingDataRows());

        ObjectIdQuery q3 = new ObjectIdQuery(
                new ObjectId("abc", "a", 1),
                false,
                ObjectIdQuery.CACHE_NOREFRESH);

        assertFalse(q3.isFetchAllowed());
        assertFalse(q3.isFetchMandatory());

        QueryMetadata md3 = q3.getMetaData(null);
        assertFalse(md3.isFetchingDataRows());
    }
}
