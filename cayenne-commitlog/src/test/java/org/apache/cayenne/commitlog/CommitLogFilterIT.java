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
package org.apache.cayenne.commitlog;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.commitlog.db.Auditable1;
import org.apache.cayenne.commitlog.db.AuditableChild1;
import org.apache.cayenne.commitlog.db.AuditableChild1x;
import org.apache.cayenne.commitlog.model.*;
import org.apache.cayenne.commitlog.unit.AuditableServerCase;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.query.SelectById;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.sql.SQLException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CommitLogFilterIT extends AuditableServerCase {

    protected ObjectContext context;
    protected CommitLogListener mockListener;

    @Override
    protected ServerRuntimeBuilder configureCayenne() {
        this.mockListener = mock(CommitLogListener.class);
        return super.configureCayenne().addModule(CommitLogModule.extend().addListener(mockListener).module());
    }

    @Before
    public void before() {
        context = runtime.newContext();
    }

    @Test
    public void testPostCommit_Insert() {

        Auditable1 a1 = context.newObject(Auditable1.class);
        a1.setCharProperty1("yy");
        ObjectId preCommitId = a1.getObjectId();

        context.commitChanges();

        ArgumentCaptor<ChangeMap> changeMap = ArgumentCaptor.forClass(ChangeMap.class);
        verify(mockListener).onPostCommit(any(ObjectContext.class), changeMap.capture());

        assertNotNull(changeMap.getValue());
        assertEquals(2, changeMap.getValue().getChanges().size());
        assertEquals(1, changeMap.getValue().getUniqueChanges().size());

        ObjectChange c = changeMap.getValue().getUniqueChanges().iterator().next();
        assertNotNull(c);
        assertEquals(ObjectChangeType.INSERT, c.getType());
        assertEquals(1, c.getAttributeChanges().size());
        assertEquals("yy", c.getAttributeChanges().get(Auditable1.CHAR_PROPERTY1.getName()).getNewValue());

        assertNotEquals(preCommitId, a1.getObjectId());
        assertEquals(preCommitId, c.getPreCommitId());
        assertEquals(a1.getObjectId(), c.getPostCommitId());
    }

    @Test
    public void testPostCommit_Update() throws SQLException {

        auditable1.insert(1, "xx");

        Auditable1 a1 = SelectById.query(Auditable1.class, 1).selectOne(context);
        a1.setCharProperty1("yy");

        ObjectId preCommitId = a1.getObjectId();

        context.commitChanges();

        ArgumentCaptor<ChangeMap> changeMap = ArgumentCaptor.forClass(ChangeMap.class);
        verify(mockListener).onPostCommit(any(ObjectContext.class), changeMap.capture());

        assertNotNull(changeMap.getValue());
        assertEquals(1, changeMap.getValue().getUniqueChanges().size());

        ObjectChange c = changeMap.getValue().getChanges().get(ObjectId.of("Auditable1", Auditable1.ID_PK_COLUMN, 1));
        assertNotNull(c);
        assertEquals(ObjectChangeType.UPDATE, c.getType());
        assertEquals(1, c.getAttributeChanges().size());
        AttributeChange pc = c.getAttributeChanges().get(Auditable1.CHAR_PROPERTY1.getName());
        assertNotNull(pc);
        assertEquals("xx", pc.getOldValue());
        assertEquals("yy", pc.getNewValue());

        assertEquals(preCommitId, a1.getObjectId());
        assertEquals(preCommitId, c.getPreCommitId());
        assertEquals(preCommitId, c.getPostCommitId());
    }

    @Test
    public void testPostCommit_Delete() throws SQLException {
        auditable1.insert(1, "xx");
        auditableChild1.insert(1, 1, "cc1");
        auditableChild1.insert(2, 1, "cc2");

        Auditable1 a1 = SelectById.query(Auditable1.class, 1).selectOne(context);
        context.deleteObjects(a1.getChildren1());
        context.deleteObject(a1);
        context.commitChanges();

        ArgumentCaptor<ChangeMap> changeMap = ArgumentCaptor.forClass(ChangeMap.class);
        verify(mockListener).onPostCommit(any(ObjectContext.class), changeMap.capture());

        assertNotNull(changeMap.getValue());
        assertEquals(3, changeMap.getValue().getUniqueChanges().size());

        // check from the perspective of the master object
        ObjectChange masterChange = changeMap.getValue().getChanges().get(ObjectId.of("Auditable1", Auditable1.ID_PK_COLUMN, 1));
        assertNotNull(masterChange);
        assertEquals(ObjectChangeType.DELETE, masterChange.getType());

        assertEquals(1, masterChange.getAttributeChanges().size());
        assertEquals("xx", masterChange.getAttributeChanges().get(Auditable1.CHAR_PROPERTY1.getName()).getOldValue());
        assertNull(masterChange.getAttributeChanges().get(Auditable1.CHAR_PROPERTY1.getName()).getNewValue());

        assertEquals("1..N was explicitly unset as a part of delete. Expected to be recorded in changes",
                1, masterChange.getToManyRelationshipChanges().size());
        assertTrue("No N..1 relationships in the entity", masterChange.getToOneRelationshipChanges().isEmpty());

        // check from the perspective of the child object
        ObjectChange childChange = changeMap.getValue().getChanges().get(ObjectId.of("AuditableChild1", AuditableChild1.ID_PK_COLUMN, 2));
        assertNotNull(childChange);
        assertEquals(ObjectChangeType.DELETE, childChange.getType());

        assertEquals(1, childChange.getAttributeChanges().size());
        assertEquals("cc2", childChange.getAttributeChanges().get(AuditableChild1.CHAR_PROPERTY1.getName()).getOldValue());
        assertNull(childChange.getAttributeChanges().get(AuditableChild1.CHAR_PROPERTY1.getName()).getNewValue());

        assertTrue("No 1..N relationships in the entity", childChange.getToManyRelationshipChanges().isEmpty());
        assertEquals("N..1 was explicitly unset as a part of delete. Expected to be recorded in changes",
                1, childChange.getToOneRelationshipChanges().size());
    }

    @Test
    public void testPostCommit_Delete_ToOneNullify() throws SQLException {
        auditable1.insert(1, "xx");
        auditableChild1.insert(1, 1, "cc1");
        auditableChild1.insert(2, 1, "cc2");

        AuditableChild1 ac1 = SelectById.query(AuditableChild1.class, 2).selectOne(context);
        context.deleteObject(ac1);
        context.commitChanges();

        ArgumentCaptor<ChangeMap> changeMap = ArgumentCaptor.forClass(ChangeMap.class);
        verify(mockListener).onPostCommit(any(ObjectContext.class), changeMap.capture());

        assertNotNull(changeMap.getValue());
        assertEquals(2, changeMap.getValue().getUniqueChanges().size());

        ObjectChange change = changeMap.getValue().getChanges().get(ObjectId.of("AuditableChild1", AuditableChild1.ID_PK_COLUMN, 2));
        assertNotNull(change);
        assertEquals(ObjectChangeType.DELETE, change.getType());

        assertEquals(1, change.getAttributeChanges().size());
        assertEquals("cc2", change.getAttributeChanges().get(AuditableChild1.CHAR_PROPERTY1.getName()).getOldValue());
        assertNull(change.getAttributeChanges().get(AuditableChild1.CHAR_PROPERTY1.getName()).getNewValue());

        assertTrue("No 1..N relationships in the entity", change.getToManyRelationshipChanges().isEmpty());
        assertEquals("N..1 state was not captured", 1, change.getToOneRelationshipChanges().size());
        assertEquals(ObjectId.of("Auditable1", Auditable1.ID_PK_COLUMN, 1),
                change.getToOneRelationshipChanges().get(AuditableChild1.PARENT.getName()).getOldValue());
    }

    @Test
    public void testPostCommit_Delete_ToOne_OneWay() throws SQLException {
        auditable1.insert(1, "xx");
        auditableChild1x.insert(1, 1, "cc1");
        auditableChild1x.insert(2, 1, "cc2");

        AuditableChild1x ac1 = SelectById.query(AuditableChild1x.class, 2).selectOne(context);
        context.deleteObject(ac1);
        context.commitChanges();

        ArgumentCaptor<ChangeMap> changeMap = ArgumentCaptor.forClass(ChangeMap.class);
        verify(mockListener).onPostCommit(any(ObjectContext.class), changeMap.capture());

        assertNotNull(changeMap.getValue());
        assertEquals(1, changeMap.getValue().getUniqueChanges().size());

        ObjectChange change = changeMap.getValue().getChanges().get(ObjectId.of("AuditableChild1x", AuditableChild1x.ID_PK_COLUMN, 2));
        assertNotNull(change);
        assertEquals(ObjectChangeType.DELETE, change.getType());

        assertEquals(1, change.getAttributeChanges().size());
        assertEquals("cc2", change.getAttributeChanges().get(AuditableChild1x.CHAR_PROPERTY1.getName()).getOldValue());
        assertNull(change.getAttributeChanges().get(AuditableChild1x.CHAR_PROPERTY1.getName()).getNewValue());

        assertTrue("No 1..N relationships in the entity", change.getToManyRelationshipChanges().isEmpty());
        assertEquals("N..1 state was not captured", 1, change.getToOneRelationshipChanges().size());
        assertEquals(ObjectId.of("Auditable1", Auditable1.ID_PK_COLUMN, 1),
                change.getToOneRelationshipChanges().get(AuditableChild1x.PARENT.getName()).getOldValue());
    }


    @Test
    public void testPostCommit_UpdateToOne() throws SQLException {
        auditable1.insert(1, "xx");
        auditable1.insert(2, "yy");

        auditableChild1.insert(1, 1, "cc1");
        auditableChild1.insert(2, 2, "cc2");
        auditableChild1.insert(3, null, "cc3");

        AuditableChild1 ac1 = SelectById.query(AuditableChild1.class, 1).selectOne(context);
        AuditableChild1 ac2 = SelectById.query(AuditableChild1.class, 2).selectOne(context);
        AuditableChild1 ac3 = SelectById.query(AuditableChild1.class, 3).selectOne(context);

        Auditable1 a1 = SelectById.query(Auditable1.class, 1).selectOne(context);
        Auditable1 a2 = SelectById.query(Auditable1.class, 2).selectOne(context);

        a1.removeFromChildren1(ac1);
        a1.addToChildren1(ac2);
        a1.addToChildren1(ac3);

        context.commitChanges();

        ArgumentCaptor<ChangeMap> changeMap = ArgumentCaptor.forClass(ChangeMap.class);
        verify(mockListener).onPostCommit(any(ObjectContext.class), changeMap.capture());

        assertNotNull(changeMap.getValue());
        assertEquals(5, changeMap.getValue().getUniqueChanges().size());

        ObjectChange a1c = changeMap.getValue().getChanges().get(
            ObjectId.of("Auditable1", Auditable1.ID_PK_COLUMN, 1));
        assertNotNull(a1c);
        assertEquals(ObjectChangeType.UPDATE, a1c.getType());
        ToManyRelationshipChange a1c1 = a1c.getToManyRelationshipChanges().get(Auditable1.CHILDREN1.getName());
        assertEquals(2, a1c1.getAdded().size());
        assertEquals(1, a1c1.getRemoved().size());

        ObjectChange a2c = changeMap.getValue().getChanges().get(
            ObjectId.of("Auditable1", Auditable1.ID_PK_COLUMN, 2));
        assertNotNull(a2c);
        assertEquals(ObjectChangeType.UPDATE, a2c.getType());
        ToManyRelationshipChange a2c1 = a2c.getToManyRelationshipChanges().get(Auditable1.CHILDREN1.getName());
        assertEquals(0, a2c1.getAdded().size());
        assertEquals(1, a2c1.getRemoved().size());

        ObjectChange ac1c = changeMap.getValue().getChanges().get(
                ObjectId.of("AuditableChild1", AuditableChild1.ID_PK_COLUMN, 1));
        assertNotNull(ac1c);
        assertEquals(ObjectChangeType.UPDATE, ac1c.getType());
        ToOneRelationshipChange ac1c1 = ac1c.getToOneRelationshipChanges().get(AuditableChild1.PARENT.getName());
        assertEquals(a1.getObjectId(), ac1c1.getOldValue());
        assertNull(ac1c1.getNewValue());

        ObjectChange ac2c = changeMap.getValue().getChanges().get(
                ObjectId.of("AuditableChild1", AuditableChild1.ID_PK_COLUMN, 2));
        assertNotNull(ac2c);
        assertEquals(ObjectChangeType.UPDATE, ac2c.getType());
        ToOneRelationshipChange ac2c1 = ac2c.getToOneRelationshipChanges()
                .get(AuditableChild1.PARENT.getName());
        assertEquals(a2.getObjectId(), ac2c1.getOldValue());
        assertEquals(a1.getObjectId(), ac2c1.getNewValue());

        ObjectChange ac3c = changeMap.getValue().getChanges().get(
                ObjectId.of("AuditableChild1", AuditableChild1.ID_PK_COLUMN, 3));
        assertNotNull(ac3c);
        assertEquals(ObjectChangeType.UPDATE, ac3c.getType());
        ToOneRelationshipChange ac3c1 = ac3c.getToOneRelationshipChanges()
                .get(AuditableChild1.PARENT.getName());
        assertNull(ac3c1.getOldValue());
        assertEquals(a1.getObjectId(), ac3c1.getNewValue());
    }

    @Test
    public void testPostCommit_UpdateToMany() throws SQLException {
        auditable1.insert(1, "xx");
        auditableChild1.insert(1, 1, "cc1");
        auditableChild1.insert(2, null, "cc2");
        auditableChild1.insert(3, null, "cc3");

        AuditableChild1 ac1 = SelectById.query(AuditableChild1.class, 1).selectOne(context);
        AuditableChild1 ac2 = SelectById.query(AuditableChild1.class, 2).selectOne(context);
        AuditableChild1 ac3 = SelectById.query(AuditableChild1.class, 3).selectOne(context);

        Auditable1 a1 = SelectById.query(Auditable1.class, 1).selectOne(context);

        a1.removeFromChildren1(ac1);
        a1.addToChildren1(ac2);
        a1.addToChildren1(ac3);

        context.commitChanges();

        ArgumentCaptor<ChangeMap> changeMap = ArgumentCaptor.forClass(ChangeMap.class);
        verify(mockListener).onPostCommit(any(ObjectContext.class), changeMap.capture());

        assertNotNull(changeMap.getValue());
        assertEquals(4, changeMap.getValue().getUniqueChanges().size());

        ObjectChange a1c = changeMap.getValue().getChanges().get(ObjectId.of("Auditable1", Auditable1.ID_PK_COLUMN, 1));
        assertNotNull(a1c);
        assertEquals(ObjectChangeType.UPDATE, a1c.getType());
        assertEquals(0, a1c.getAttributeChanges().size());

        assertEquals(1, a1c.getToManyRelationshipChanges().size());

        ToManyRelationshipChange a1c1 = a1c.getToManyRelationshipChanges().get(Auditable1.CHILDREN1.getName());
        assertNotNull(a1c1);

        assertEquals(2, a1c1.getAdded().size());
        assertTrue(a1c1.getAdded().contains(ac2.getObjectId()));
        assertTrue(a1c1.getAdded().contains(ac3.getObjectId()));

        assertEquals(1, a1c1.getRemoved().size());
        assertTrue(a1c1.getRemoved().contains(ac1.getObjectId()));

    }
}
