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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link ObjectSelect#byId(Object)} and {@link ObjectSelect#byIds(Object...)} against an entity with a compound
 * PK. The single-column PK cases are in {@link ObjectSelect_ByIdIT}.
 */
public class ObjectSelect_ByIdCompoundPkIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.COMPOUND_PROJECT);

    private static final Map<String, ?> PK_1 = Map.of("KEY1", "PK1", "KEY2", "PK2");
    private static final Map<String, ?> PK_2 = Map.of("KEY1", "PK3", "KEY2", "PK4");

    @BeforeEach
    public void seed() throws SQLException {
        TableHelper pk = env.table("COMPOUND_PK_TEST", "KEY1", "KEY2", "NAME");
        pk.insert("PK1", "PK2", "BBB");
        pk.insert("PK3", "PK4", "CCC");
    }

    @Test
    public void byId_Map() {
        CompoundPkTestEntity o = ObjectSelect.query(CompoundPkTestEntity.class).byId(PK_2).selectOne(env.context());
        assertNotNull(o);
        assertEquals("CCC", o.getName());
    }

    @Test
    public void byId_ObjectId() {
        ObjectId id = ObjectId.of("CompoundPkTestEntity", PK_2);
        CompoundPkTestEntity o = ObjectSelect.query(CompoundPkTestEntity.class).byId(id).selectOne(env.context());
        assertNotNull(o);
        assertEquals("CCC", o.getName());
    }

    @Test
    public void byId_Scalar() {
        assertThrows(CayenneRuntimeException.class,
                () -> ObjectSelect.query(CompoundPkTestEntity.class).byId("PK1").selectOne(env.context()));
    }

    @Test
    public void byIds_MapsAndObjectIds() {
        ObjectId id2 = ObjectId.of("CompoundPkTestEntity", PK_2);
        List<CompoundPkTestEntity> list = ObjectSelect.query(CompoundPkTestEntity.class)
                .byIds(PK_1, id2)
                .orderBy(CompoundPkTestEntity.NAME.asc()).select(env.context());
        assertEquals(2, list.size());
        assertEquals("BBB", list.get(0).getName());
        assertEquals("CCC", list.get(1).getName());
    }
}
