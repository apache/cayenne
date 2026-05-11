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


package org.apache.cayenne.query;

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AbstractQueryTest {

    @Test
    public void setRootEntityName() {
        AbstractQuery query = new MockAbstractQuery();
        assertNull(query.getRoot());
        query.setRoot("SomeEntity");
        assertSame("SomeEntity", query.getRoot());
    }

    @Test
    public void setRootObjEntity() {
        AbstractQuery query = new MockAbstractQuery();

        assertNull(query.getRoot());
        ObjEntity e = new ObjEntity("ABC");
        query.setRoot(e);
        assertSame(e, query.getRoot());
    }

    @Test
    public void setRootClass() {
        AbstractQuery query = new MockAbstractQuery();
        assertNull(query.getRoot());
        query.setRoot(Artist.class);
        assertSame(Artist.class, query.getRoot());
    }

    @Test
    public void setInvalidRoot() {
        AbstractQuery query = new MockAbstractQuery();
        assertNull(query.getRoot());
        assertThrows(IllegalArgumentException.class, () -> query.setRoot(1));
    }
}
