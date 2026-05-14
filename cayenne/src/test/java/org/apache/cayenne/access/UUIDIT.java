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

import java.util.UUID;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.uuid.UuidPkEntity;
import org.apache.cayenne.testdo.uuid.UuidTestEntity;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UUIDIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.UUID_PROJECT);

    private TableHelper uuidPkEntity;

    @BeforeEach
    public void setUp() throws Exception {
        uuidPkEntity = env.table("UUID_PK_ENTITY", "ID");
    }

    @Test
    public void uuid() throws Exception {

        UuidTestEntity test = env.context().newObject(UuidTestEntity.class);

        UUID id = UUID.randomUUID();
        test.setUuid(id);
        env.context().commitChanges();

        UuidTestEntity testRead = ObjectSelect.query(UuidTestEntity.class)
                .selectFirst(env.context());
        assertNotNull(testRead.getUuid());
        assertEquals(id, testRead.getUuid());

        test.setUuid(null);
        env.context().commitChanges();
    }

    @Test
    public void uuidMeaningfulPkInsert() throws Exception {
        UUID id = UUID.randomUUID();

        UuidPkEntity o1 = env.context().newObject(UuidPkEntity.class);
        o1.setId(id);

        env.context().commitChanges();

        String fetched = uuidPkEntity.getString("ID");
        assertEquals(id, UUID.fromString(fetched));
    }

    @Test
    public void uuidMeaningfulPkSelect() throws Exception {
        UUID id = UUID.randomUUID();

        uuidPkEntity.insert(id.toString());

        UuidPkEntity o1 = Cayenne.objectForPK(env.context(), UuidPkEntity.class, id);

        assertNotNull(o1);
        assertEquals(id, o1.getId());
        assertEquals(id, o1.getObjectId().getIdSnapshot().get("ID"));
    }

    @Test
    public void uuidColumnSelect() throws Exception {
        UuidTestEntity test = env.context().newObject(UuidTestEntity.class);
        UUID id = UUID.randomUUID();
        test.setUuid(id);
        env.context().commitChanges();

        UUID readValue = ObjectSelect.query(UuidTestEntity.class)
                .column(UuidTestEntity.UUID).selectOne(env.context());

        assertEquals(id, readValue);

        UUID readValue2 = ObjectSelect.query(UuidTestEntity.class)
                .column(PropertyFactory.createBase(ExpressionFactory.dbPathExp("UUID"), UUID.class)).selectOne(env.context());

        assertEquals(id, readValue2);
    }
}
