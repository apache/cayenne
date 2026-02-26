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
package org.apache.cayenne.access;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.uuid.UuidBinTestEntity;
import org.apache.cayenne.testdo.uuid.UuidPkEntity;
import org.apache.cayenne.testdo.uuid.UuidTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseCayenneRuntime(CayenneProjects.UUID_PROJECT)
public class UUIDIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper uuidPkEntity;
    private TableHelper uuidBinTable;

    @Before
    public void setUp() throws Exception {
        uuidPkEntity = new TableHelper(dbHelper, "UUID_PK_ENTITY", "ID");
        uuidBinTable = new TableHelper(dbHelper, "UUID_BIN_TEST");
    }

    @Test
    public void testUUID() throws Exception {

        UuidTestEntity test = context.newObject(UuidTestEntity.class);

        UUID id = UUID.randomUUID();
        test.setUuid(id);
        context.commitChanges();

        UuidTestEntity testRead = ObjectSelect.query(UuidTestEntity.class)
                .selectFirst(context);
        assertNotNull(testRead.getUuid());
        assertEquals(id, testRead.getUuid());

        test.setUuid(null);
        context.commitChanges();
    }

    @Test
    public void testUUIDMeaningfulPkInsert() throws Exception {
        UUID id = UUID.randomUUID();

        UuidPkEntity o1 = context.newObject(UuidPkEntity.class);
        o1.setId(id);

        context.commitChanges();

        String fetched = uuidPkEntity.getString("ID");
        assertEquals(id, UUID.fromString(fetched));
    }

    @Test
    public void testUUIDMeaningfulPkSelect() throws Exception {
        UUID id = UUID.randomUUID();

        uuidPkEntity.insert(id.toString());

        UuidPkEntity o1 = Cayenne.objectForPK(context, UuidPkEntity.class, id);

        assertNotNull(o1);
        assertEquals(id, o1.getId());
        assertEquals(id, o1.getObjectId().getIdSnapshot().get("ID"));
    }

    @Test
    public void testUUIDColumnSelect() throws Exception {
        UuidTestEntity test = context.newObject(UuidTestEntity.class);
        UUID id = UUID.randomUUID();
        test.setUuid(id);
        context.commitChanges();

        UUID readValue = ObjectSelect.query(UuidTestEntity.class)
                .column(UuidTestEntity.UUID).selectOne(context);

        assertEquals(id, readValue);

        UUID readValue2 = ObjectSelect.query(UuidTestEntity.class)
                .column(PropertyFactory.createBase(ExpressionFactory.dbPathExp("UUID"), UUID.class)).selectOne(context);

        assertEquals(id, readValue2);
    }

    @Test
    public void testUUIDBinary_InsertSelect() {
        UuidBinTestEntity test = context.newObject(UuidBinTestEntity.class);
        UUID expected = UUID.randomUUID();
        test.setUuid(expected);
        context.commitChanges();

        UuidBinTestEntity testRead = ObjectSelect.query(UuidBinTestEntity.class).selectFirst(context);
        assertNotNull(testRead.getUuid());
        assertEquals(expected, testRead.getUuid());
    }

    @Test
    public void testUUIDBinary_RawBytes() throws Exception {
        UuidBinTestEntity test = context.newObject(UuidBinTestEntity.class);
        UUID expected = UUID.randomUUID();
        test.setUuid(expected);
        context.commitChanges();

        byte[] raw = uuidBinTable.getBytes("UUID");
        assertNotNull(raw);
        assertEquals(16, raw.length);

        ByteBuffer bb = ByteBuffer.wrap(raw);
        UUID actual = new UUID(bb.getLong(), bb.getLong());
        assertEquals(expected, actual);
    }
}
