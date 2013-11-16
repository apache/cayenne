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

package org.apache.cayenne.query;

import junit.framework.TestCase;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.util.Util;

public class ObjectIdQueryTest extends TestCase {

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

    /**
     * Proper 'equals' and 'hashCode' implementations are important when mapping
     * results obtained in a QueryChain back to the query.
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
        ObjectIdQuery q1 = new ObjectIdQuery(new ObjectId("abc", "a", 1), true, ObjectIdQuery.CACHE_REFRESH);

        assertTrue(q1.isFetchAllowed());
        assertTrue(q1.isFetchMandatory());

        QueryMetadata md1 = q1.getMetaData(null);
        assertTrue(md1.isFetchingDataRows());

        ObjectIdQuery q2 = new ObjectIdQuery(new ObjectId("abc", "a", 1), false, ObjectIdQuery.CACHE);

        assertTrue(q2.isFetchAllowed());
        assertFalse(q2.isFetchMandatory());

        QueryMetadata md2 = q2.getMetaData(null);
        assertFalse(md2.isFetchingDataRows());

        ObjectIdQuery q3 = new ObjectIdQuery(new ObjectId("abc", "a", 1), false, ObjectIdQuery.CACHE_NOREFRESH);

        assertFalse(q3.isFetchAllowed());
        assertFalse(q3.isFetchMandatory());

        QueryMetadata md3 = q3.getMetaData(null);
        assertFalse(md3.isFetchingDataRows());
    }
}
