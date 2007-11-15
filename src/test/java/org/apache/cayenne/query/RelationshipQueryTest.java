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
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.util.Util;

/**
 * @author Andrus Adamchik
 */
public class RelationshipQueryTest extends TestCase {

    public void testConstructorObjectId() {

        ObjectId oid = new ObjectId("MockDataObject", "a", "b");
        RelationshipQuery query = new RelationshipQuery(oid, "relX");
        assertSame(oid, query.getObjectId());
        assertSame("relX", query.getRelationshipName());
    }

    public void testSerializability() throws Exception {
        ObjectId oid = new ObjectId("test", "a", "b");
        RelationshipQuery query = new RelationshipQuery(oid, "relX");

        RelationshipQuery q1 = (RelationshipQuery) Util.cloneViaSerialization(query);
        assertNotNull(q1);
        assertEquals(oid, q1.getObjectId());
        assertEquals("relX", q1.getRelationshipName());
    }

    public void testSerializabilityWithHessian() throws Exception {
        ObjectId oid = new ObjectId("test", "a", "b");
        RelationshipQuery query = new RelationshipQuery(oid, "relX");

        RelationshipQuery q1 = (RelationshipQuery) HessianUtil
                .cloneViaClientServerSerialization(query,new EntityResolver());
        assertNotNull(q1);
        assertEquals(oid, q1.getObjectId());
        assertEquals("relX", q1.getRelationshipName());
    }
}
