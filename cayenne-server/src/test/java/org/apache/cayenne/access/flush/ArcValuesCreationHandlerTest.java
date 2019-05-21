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

import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.flush.operation.DbRowOpType;
import org.apache.cayenne.access.flush.operation.DbRowOpVisitor;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.access.flush.operation.Values;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @since 4.2
 */
public class ArcValuesCreationHandlerTest {

    private ArcValuesCreationHandler handler;
    private DbRowOpFactory factory;
    private InsertDbRowOp dbRowOp;
    private Values values;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        factory = mock(DbRowOpFactory.class);
        handler = new ArcValuesCreationHandler(factory, DbRowOpType.INSERT);
        dbRowOp = mock(InsertDbRowOp.class);
        values = new Values(dbRowOp, false);

        ObjectDiff diff = mock(ObjectDiff.class);
        ClassDescriptor descriptor = mock(ClassDescriptor.class);
        ObjEntity entity = mock(ObjEntity.class);
        ObjRelationship relationship = mock(ObjRelationship.class);
        DbRelationship dbRelationship = mock(DbRelationship.class);
        ObjectStore store = mock(ObjectStore.class);
        Persistent object = mock(Persistent.class);

        when(relationship.getDbRelationships()).thenReturn(Collections.singletonList(dbRelationship));
        when(entity.getRelationship(anyString())).thenReturn(relationship);
        when(descriptor.getEntity()).thenReturn(entity);
        when(dbRowOp.accept(any(DbRowOpVisitor.class))).thenCallRealMethod();
        when(dbRowOp.getValues()).thenReturn(values);
        when(factory.getDiff()).thenReturn(diff);
        when(factory.getDescriptor()).thenReturn(descriptor);
        when(factory.getStore()).thenReturn(store);
        when(factory.getObject()).thenReturn(object);
        when(factory.getOrCreate(isNull(), any(ObjectId.class), any(DbRowOpType.class))).thenReturn(dbRowOp);
    }

    @Test
    public void processRelationshipPkPkMaster() {
        ObjectId srcId = ObjectId.of("test1", "id1", 1);
        ObjectId targetId = ObjectId.of("test2", "id2", 2);

        DbRelationship relationship = DbRelBuilder.of("id1", "id2")
                .withToDepPk().withDstPk().withSrcPk().build();

        handler.processRelationship(relationship, srcId, targetId, true);

        assertNotNull(handler);
        verify(factory).getOrCreate(isNull(), eq(targetId), eq(DbRowOpType.UPDATE));
        assertTrue(targetId.isReplacementIdAttached());
        assertEquals(1, targetId.getReplacementIdMap().size());
        assertEquals(1, targetId.getReplacementIdMap().get("id2"));
        assertFalse(srcId.isReplacementIdAttached());
    }

    @Test
    public void processRelationshipPkPkDependent() {
        ObjectId srcId = ObjectId.of("test1", "id1", 1);
        ObjectId targetId = ObjectId.of("test2", "id2", 2);

        DbRelationship relationship = DbRelBuilder.of("id1", "id2")
                .withDstPk().withSrcPk().build();

        handler.processRelationship(relationship, srcId, targetId, true);

        assertNotNull(handler);
        verify(factory).getOrCreate(isNull(), eq(srcId), eq(DbRowOpType.INSERT));
        assertTrue(srcId.isReplacementIdAttached());
        assertEquals(1, srcId.getReplacementIdMap().size());
        assertEquals(2, srcId.getReplacementIdMap().get("id1"));
        assertFalse(targetId.isReplacementIdAttached());
    }

    @Test
    public void processRelationshipPkFkMaster() {
        ObjectId srcId = ObjectId.of("test1", "pk", 1);
        ObjectId targetId = ObjectId.of("test2", "id2", 2);

        DbRelationship relationship = DbRelBuilder.of("pk", "fk")
                .withSrcPk().build();

        handler.processRelationship(relationship, srcId, targetId, true);

        assertNotNull(handler);
        verify(factory).getOrCreate(isNull(), eq(targetId), eq(DbRowOpType.UPDATE));
        assertFalse(srcId.isReplacementIdAttached());
        assertFalse(targetId.isReplacementIdAttached());

        verify(dbRowOp).getValues();
        Map<String, Object> snapshot = values.getSnapshot();
        assertEquals(1, snapshot.size());
        assertEquals(1, snapshot.get("fk"));
    }

    @Test
    public void processRelationshipFkPkDependent() {
        ObjectId srcId = ObjectId.of("test1", "id1", 1);
        ObjectId targetId = ObjectId.of("test2", "pk", 2);

        DbRelationship relationship = DbRelBuilder.of("fk", "pk")
                .withDstPk().build();

        handler.processRelationship(relationship, srcId, targetId, true);

        assertNotNull(handler);
        verify(factory).getOrCreate(isNull(), eq(srcId), eq(DbRowOpType.INSERT));
        assertFalse(srcId.isReplacementIdAttached());
        assertFalse(targetId.isReplacementIdAttached());

        verify(dbRowOp).getValues();
        Map<String, Object> snapshot = values.getSnapshot();
        assertEquals(1, snapshot.size());
        assertEquals(2, snapshot.get("fk"));
    }

    final static class DbRelBuilder {
        private String srcName;
        private String dstName;
        private boolean srcPk;
        private boolean dstPk;
        private boolean toDepPk;

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

        DbRelBuilder withToDepPk() {
            toDepPk = true;
            return this;
        }

        DbRelationship build() {
            DbRelationship relationship = mock(DbRelationship.class);
            when(relationship.isToDependentPK()).thenReturn(toDepPk);
            DbJoin join = mock(DbJoin.class);
            DbAttribute src = new DbAttribute(srcName);
            src.setPrimaryKey(srcPk);
            DbAttribute target = new DbAttribute(dstName);
            target.setPrimaryKey(dstPk);
            when(join.getSource()).thenReturn(src);
            when(join.getSourceName()).thenReturn(src.getName());
            when(join.getTarget()).thenReturn(target);
            when(join.getTargetName()).thenReturn(target.getName());
            when(relationship.getJoins()).thenReturn(Collections.singletonList(join));

            DbRelationship mockRel = mock(DbRelationship.class);
            when(relationship.getReverseRelationship()).thenReturn(mockRel);
            return relationship;
        }
    }
}
