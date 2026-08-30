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

package org.apache.cayenne.exp.property;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CompoundFkTestEntity;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityPropertyIdIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.COMPOUND_PROJECT);

    private static final Map<String, ?> PK_1 = Map.of("KEY1", "PK1", "KEY2", "PK2");
    private static final Map<String, ?> PK_2 = Map.of("KEY1", "PK3", "KEY2", "PK4");

    @BeforeEach
    public void seed() throws SQLException {
        TableHelper pk = env.table("COMPOUND_PK_TEST", "KEY1", "KEY2", "NAME");
        pk.insert("PK1", "PK2", "BBB");
        pk.insert("PK3", "PK4", "CCC");

        TableHelper fk = env.table("COMPOUND_FK_TEST", "PKEY", "F_KEY1", "F_KEY2", "NAME");
        fk.insert(1, "PK1", "PK2", "FK1");
        fk.insert(2, "PK3", "PK4", "FK2");
    }

    // --- "self" property: an empty path ---

    @Test
    public void selfEqIdMap() {
        CompoundPkTestEntity o = ObjectSelect.query(CompoundPkTestEntity.class)
                .where(CompoundPkTestEntity.SELF.eqIdMap(PK_2)).selectOne(env.context());
        assertNotNull(o);
        assertEquals("CCC", o.getName());
    }

    @Test
    public void selfEqIdMapMatchesExplicitDbExpression() {
        CompoundPkTestEntity viaProperty = ObjectSelect.query(CompoundPkTestEntity.class)
                .where(CompoundPkTestEntity.SELF.eqIdMap(PK_2)).selectOne(env.context());

        // the same match, spelled out as raw db paths
        CompoundPkTestEntity viaExpression = ObjectSelect.query(CompoundPkTestEntity.class)
                .where(ExpressionFactory.matchAllDbExp(PK_2, Expression.EQUAL_TO)).selectOne(env.context());

        assertSame(viaExpression, viaProperty);
    }

    @Test
    public void selfNeqIdMap() {
        List<CompoundPkTestEntity> list = ObjectSelect.query(CompoundPkTestEntity.class)
                .where(CompoundPkTestEntity.SELF.neqIdMap(PK_1)).select(env.context());
        assertEquals(1, list.size());
        assertEquals("CCC", list.get(0).getName());
    }

    @Test
    public void selfIdMapsIn() {
        List<CompoundPkTestEntity> list = ObjectSelect.query(CompoundPkTestEntity.class)
                .where(CompoundPkTestEntity.SELF.idMapsIn(PK_1, PK_2))
                .orderBy(CompoundPkTestEntity.NAME.asc()).select(env.context());
        assertEquals(2, list.size());
        assertEquals("BBB", list.get(0).getName());
        assertEquals("CCC", list.get(1).getName());
    }

    @Test
    public void selfObjectIdsIn() {
        ObjectId id1 = ObjectId.of("CompoundPkTestEntity", PK_1);
        ObjectId id2 = ObjectId.of("CompoundPkTestEntity", PK_2);

        List<CompoundPkTestEntity> list = ObjectSelect.query(CompoundPkTestEntity.class)
                .where(CompoundPkTestEntity.SELF.objectIdsIn(id1, id2))
                .orderBy(CompoundPkTestEntity.NAME.asc()).select(env.context());
        assertEquals(2, list.size());
        assertEquals("BBB", list.get(0).getName());
        assertEquals("CCC", list.get(1).getName());
    }

    // --- to-one relationship property: a non-empty path ---

    @Test
    public void toOneEqIdMap() {
        CompoundFkTestEntity o = ObjectSelect.query(CompoundFkTestEntity.class)
                .where(CompoundFkTestEntity.TO_COMPOUND_PK.eqIdMap(PK_2)).selectOne(env.context());
        assertNotNull(o);
        assertEquals("FK2", o.getName());
    }

    @Test
    public void toOneIdMapsIn() {
        List<CompoundFkTestEntity> list = ObjectSelect.query(CompoundFkTestEntity.class)
                .where(CompoundFkTestEntity.TO_COMPOUND_PK.idMapsIn(PK_1, PK_2))
                .orderBy(CompoundFkTestEntity.NAME.asc()).select(env.context());
        assertEquals(2, list.size());
        assertEquals("FK1", list.get(0).getName());
    }

    @Test
    public void toOneObjectIdsIn() {
        ObjectId id1 = ObjectId.of("CompoundPkTestEntity", PK_1);
        List<CompoundFkTestEntity> list = ObjectSelect.query(CompoundFkTestEntity.class)
                .where(CompoundFkTestEntity.TO_COMPOUND_PK.objectIdsIn(id1)).select(env.context());
        assertEquals(1, list.size());
        assertEquals("FK1", list.get(0).getName());
    }

    // --- edge cases ---

    @Test
    public void emptyIdCollectionMatchesNothing() {
        List<CompoundPkTestEntity> list = ObjectSelect.query(CompoundPkTestEntity.class)
                .where(CompoundPkTestEntity.SELF.idMapsInCollection(List.of())).select(env.context());
        assertTrue(list.isEmpty());
    }

    @Test
    public void emptyIdMapFails() {
        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class,
                () -> CompoundPkTestEntity.SELF.eqIdMap(Map.of()));
        assertTrue(e.getMessage().contains("Null or empty id map"), e.getMessage());
    }
}
