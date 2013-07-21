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

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.testdo.testmap.Artist;

public class AbstractQueryTest extends TestCase {

    public void testSetRootEntityName() {
        AbstractQuery query = new MockAbstractQuery();
        assertNull(query.getRoot());
        query.setRoot("SomeEntity");
        assertSame("SomeEntity", query.getRoot());
    }

    public void testSetRootObjEntity() {
        AbstractQuery query = new MockAbstractQuery();

        assertNull(query.getRoot());
        ObjEntity e = new ObjEntity("ABC");
        query.setRoot(e);
        assertSame(e, query.getRoot());
    }

    public void testSetRootClass() {
        AbstractQuery query = new MockAbstractQuery();
        assertNull(query.getRoot());
        query.setRoot(Artist.class);
        assertSame(Artist.class, query.getRoot());
    }

    public void testSetInvalidRoot() {
        AbstractQuery query = new MockAbstractQuery();
        assertNull(query.getRoot());
        try {
            query.setRoot(new Integer(1));
            fail("Should not be able to set the root to an Integer");
        }
        catch (IllegalArgumentException e) {
        }
    }
}
