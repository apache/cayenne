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
package org.apache.cayenne.access;

import java.util.UUID;

import org.apache.art.UuidTestEntity;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class UUIDTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testUUID() throws Exception {

        ObjectContext context = createDataContext();
        UuidTestEntity test = context.newObject(UuidTestEntity.class);

        UUID id = UUID.randomUUID();
        test.setUuid(id);
        context.commitChanges();

        SelectQuery q = new SelectQuery(UuidTestEntity.class);
        UuidTestEntity testRead = (UuidTestEntity) context.performQuery(q).get(0);
        assertNotNull(testRead.getUuid());
        assertEquals(id, testRead.getUuid());

        test.setUuid(null);
        context.commitChanges();
    }
}
