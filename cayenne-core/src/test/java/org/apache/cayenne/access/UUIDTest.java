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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.UuidPkEntity;
import org.apache.cayenne.testdo.testmap.UuidTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class UUIDTest extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper uuidPkEntity;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("UUID_TEST");
        dbHelper.deleteAll("UUID_PK_ENTITY");

        uuidPkEntity = new TableHelper(dbHelper, "UUID_PK_ENTITY", "ID");
    }

    public void testUUID() throws Exception {

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

    public void testUUIDMeaningfulPkInsert() throws Exception {
        UUID id = UUID.randomUUID();

        UuidPkEntity o1 = context.newObject(UuidPkEntity.class);
        o1.setId(id);

        context.commitChanges();

        String fetched = uuidPkEntity.getString("ID");
        assertEquals(id, UUID.fromString(fetched));
    }

    public void testUUIDMeaningfulPkSelect() throws Exception {
        UUID id = UUID.randomUUID();

        uuidPkEntity.insert(id.toString());

        UuidPkEntity o1 = Cayenne.objectForPK(context, UuidPkEntity.class, id);

        assertNotNull(o1);
        assertEquals(id, o1.getId());
        assertEquals(id, o1.getObjectId().getIdSnapshot().get("ID"));
    }
}
