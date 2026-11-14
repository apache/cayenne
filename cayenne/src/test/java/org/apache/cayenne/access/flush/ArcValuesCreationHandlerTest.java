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

package org.apache.cayenne.access.flush;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOpType;
import org.apache.cayenne.access.flush.operation.DeleteDbRowOp;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.access.flush.operation.UpdateDbRowOp;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.unit.CompiledTables;
import org.apache.cayenne.unit.TestObjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArcValuesCreationHandlerTest {

    private RecordingFactory factory;
    private ArcValuesCreationHandler handler;

    @BeforeEach
    public void setup() {
        factory = new RecordingFactory();
        handler = new ArcValuesCreationHandler(factory, DbRowOpType.INSERT);
    }

    @Test
    public void processRelationshipPkPkMaster() {
        ObjectId srcId = ObjectId.of("test1", "id1", 1);
        ObjectId targetId = ObjectId.of("test2", "id2", 2);

        CompiledJoin join = DbRelBuilder.of("id1", "id2")
                .withDstPk().withSrcPk().build();

        handler.processRelationship(join, srcId, targetId, true);

        assertRow(UpdateDbRowOp.class, "test2", targetId);
        assertTrue(targetId.isReplacementIdAttached());
        assertEquals(1, targetId.getReplacementIdMap().size());
        assertEquals(1, targetId.getReplacementIdMap().get("id2"));
        assertFalse(srcId.isReplacementIdAttached());
    }

    @Test
    public void processRelationshipPkPkDependent() {
        ObjectId srcId = ObjectId.of("test1", "id1", 1);
        ObjectId targetId = ObjectId.of("test2", "id2", 2);

        CompiledJoin join = DbRelBuilder.of("id1", "id2")
                .withForeignKey().withDstPk().withSrcPk().build();

        handler.processRelationship(join, srcId, targetId, true);

        assertRow(InsertDbRowOp.class, "test1", srcId);
        assertTrue(srcId.isReplacementIdAttached());
        assertEquals(1, srcId.getReplacementIdMap().size());
        assertEquals(2, srcId.getReplacementIdMap().get("id1"));
        assertFalse(targetId.isReplacementIdAttached());
    }

    @Test
    public void processRelationshipPkFkMaster() {
        ObjectId srcId = ObjectId.of("test1", "pk", 1);
        ObjectId targetId = ObjectId.of("test2", "id2", 2);

        CompiledJoin join = DbRelBuilder.of("pk", "fk")
                .withSrcPk().build();

        handler.processRelationship(join, srcId, targetId, true);

        UpdateDbRowOp row = assertRow(UpdateDbRowOp.class, "test2", targetId);
        assertFalse(srcId.isReplacementIdAttached());
        assertFalse(targetId.isReplacementIdAttached());

        Map<String, Object> snapshot = row.getValues().getSnapshot();
        assertEquals(1, snapshot.size());
        assertEquals(1, snapshot.get("fk"));
    }

    @Test
    public void processRelationshipFkPkDependent() {
        ObjectId srcId = ObjectId.of("test1", "id1", 1);
        ObjectId targetId = ObjectId.of("test2", "pk", 2);

        CompiledJoin join = DbRelBuilder.of("fk", "pk")
                .withDstPk().build();

        handler.processRelationship(join, srcId, targetId, true);

        InsertDbRowOp row = assertRow(InsertDbRowOp.class, "test1", srcId);
        assertFalse(srcId.isReplacementIdAttached());
        assertFalse(targetId.isReplacementIdAttached());

        Map<String, Object> snapshot = row.getValues().getSnapshot();
        assertEquals(2, snapshot.get("fk"));
    }

    private <T extends DbRowOp> T assertRow(Class<T> type, String table, ObjectId id) {
        assertEquals(1, factory.rows.size(), factory.rows.toString());
        DbRowOp row = factory.rows.get(id);
        assertInstanceOf(type, row);
        assertEquals(table, row.getEntity().name());
        return type.cast(row);
    }

    /**
     * Creates the rows like the real factory, but for a fixed object, without a context or a diff being processed.
     */
    static class RecordingFactory extends DbRowOpFactory {

        private static final Persistent OBJECT = TestObjects.modifiedObject(ObjectId.of("test1", "id1", 1));

        final Map<ObjectId, DbRowOp> rows = new HashMap<>();

        RecordingFactory() {
            super(null, null, new HashSet<>(), null);
        }

        @SuppressWarnings("unchecked")
        @Override
        <E extends DbRowOp> E getOrCreate(CompiledTable table, ObjectId id, DbRowOpType type) {
            return (E) rows.computeIfAbsent(id, i -> switch (type) {
                case INSERT -> new InsertDbRowOp(OBJECT, table, i);
                case UPDATE -> new UpdateDbRowOp(OBJECT, table, i);
                case DELETE -> new DeleteDbRowOp(OBJECT, table, i);
            });
        }
    }

    /**
     * Builds a to-one join from the "test1" table to the "test2" table over a single pair of columns.
     */
    static final class DbRelBuilder {
        private String srcName;
        private String dstName;
        private boolean srcPk;
        private boolean dstPk;
        private boolean foreignKey;

        static DbRelBuilder of(String srcName, String dstName) {
            DbRelBuilder builder = new DbRelBuilder();
            builder.srcName = srcName;
            builder.dstName = dstName;
            return builder;
        }

        DbRelBuilder withSrcPk() {
            srcPk = true;
            return this;
        }

        DbRelBuilder withDstPk() {
            dstPk = true;
            return this;
        }

        DbRelBuilder withForeignKey() {
            foreignKey = true;
            return this;
        }

        CompiledJoin build() {
            DbEntity src = table("test1", srcName, srcPk);
            DbEntity target = table("test2", dstName, dstPk);

            DbRelationship relationship = new DbRelationship("rel");
            src.addRelationship(relationship);
            relationship.setTargetEntityName(target.getName());
            relationship.setToMany(false);
            relationship.setForeignKey(foreignKey);
            relationship.addJoin(new DbJoin(relationship, srcName, dstName));

            DataMap map = new DataMap("M");
            map.addDbEntity(src);
            map.addDbEntity(target);
            return CompiledTables.compile(map).table("test1").join("rel");
        }

        private static DbEntity table(String name, String column, boolean pk) {
            DbAttribute attribute = new DbAttribute(column);
            attribute.setPrimaryKey(pk);
            DbEntity table = new DbEntity(name);
            table.addAttribute(attribute);
            return table;
        }
    }
}
