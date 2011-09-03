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

package org.apache.cayenne;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.relationship.ToOneFK1;
import org.apache.cayenne.testdo.relationship.ToOneFK2;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Tests the behavior of one-to-one relationship where to-one is pointing to an FK.
 */
@UseServerRuntime(ServerCase.RELATIONSHIPS_PROJECT)
public class CDOOneToOneFKTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DataContext context1;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("TO_ONEFK1");
        dbHelper.deleteAll("TO_ONEFK2");
    }

    public void testReadRelationship() {
        ToOneFK2 src = context.newObject(ToOneFK2.class);
        ToOneFK1 target = context.newObject(ToOneFK1.class);
        src.setToOneToFK(target);
        context.commitChanges();

        context.invalidateObjects(src, target);

        ToOneFK2 src1 = (ToOneFK2) Cayenne.objectForPK(context, src.getObjectId());
        assertNotNull(src1.getToOneToFK());
        // resolve HOLLOW
        assertSame(src1, src1.getToOneToFK().getToPK());

        context.invalidateObjects(src1, src1.getToOneToFK());

        ToOneFK1 target2 = (ToOneFK1) Cayenne.objectForPK(context, target.getObjectId());
        assertNotNull(target2.getToPK());

        // resolve HOLLOW
        assertSame(target2, target2.getToPK().getToOneToFK());
    }

    public void test2Null() throws Exception {
        ToOneFK2 src = context.newObject(ToOneFK2.class);
        context.commitChanges();

        // test database data
        ObjectIdQuery refetch = new ObjectIdQuery(
                src.getObjectId(),
                false,
                ObjectIdQuery.CACHE_REFRESH);
        ToOneFK2 src2 = (ToOneFK2) Cayenne.objectForQuery(context1, refetch);

        // *** TESTING THIS ***
        assertNull(src2.getToOneToFK());
    }

    public void testReplaceNull1() throws Exception {
        ToOneFK2 src = context.newObject(ToOneFK2.class);
        context.commitChanges();

        // test database data
        ObjectIdQuery refetch = new ObjectIdQuery(
                src.getObjectId(),
                false,
                ObjectIdQuery.CACHE_REFRESH);
        ToOneFK2 src2 = (ToOneFK2) Cayenne.objectForQuery(context1, refetch);
        assertEquals(src.getObjectId(), src2.getObjectId());

        // *** TESTING THIS ***
        src2.setToOneToFK(null);
        assertNull(src2.getToOneToFK());
    }

    public void testReplaceNull2() throws Exception {
        ToOneFK2 src = context.newObject(ToOneFK2.class);
        context.commitChanges();

        ToOneFK1 target = context.newObject(ToOneFK1.class);

        // *** TESTING THIS ***
        src.setToOneToFK(target);

        // test before save
        assertSame(target, src.getToOneToFK());

        // do save
        context.commitChanges();

        // test database data
        ObjectIdQuery refetch = new ObjectIdQuery(
                src.getObjectId(),
                false,
                ObjectIdQuery.CACHE_REFRESH);
        ToOneFK2 src2 = (ToOneFK2) Cayenne.objectForQuery(context1, refetch);
        ToOneFK1 target2 = src2.getToOneToFK();
        assertNotNull(target2);
        assertEquals(src.getObjectId(), src2.getObjectId());
        assertEquals(target.getObjectId(), target2.getObjectId());
    }

    public void testNewAdd() throws Exception {
        ToOneFK2 src = context.newObject(ToOneFK2.class);
        ToOneFK1 target = context.newObject(ToOneFK1.class);

        // *** TESTING THIS ***
        src.setToOneToFK(target);

        // test before save
        assertSame(target, src.getToOneToFK());

        // do save
        context.commitChanges();

        // test database data
        ObjectIdQuery refetch = new ObjectIdQuery(
                src.getObjectId(),
                false,
                ObjectIdQuery.CACHE_REFRESH);
        ToOneFK2 src2 = (ToOneFK2) Cayenne.objectForQuery(context1, refetch);
        ToOneFK1 target2 = src2.getToOneToFK();
        assertNotNull(target2);
        assertEquals(src.getObjectId(), src2.getObjectId());
        assertEquals(target.getObjectId(), target2.getObjectId());
    }

    public void testTakeObjectSnapshotDependentFault() throws Exception {
        ToOneFK2 src = context.newObject(ToOneFK2.class);
        ToOneFK1 target = context.newObject(ToOneFK1.class);
        src.setToOneToFK(target);
        context.commitChanges();

        ObjectIdQuery refetch = new ObjectIdQuery(
                src.getObjectId(),
                false,
                ObjectIdQuery.CACHE_REFRESH);
        ToOneFK2 src2 = (ToOneFK2) Cayenne.objectForQuery(context1, refetch);

        assertTrue(src2.readPropertyDirectly("toOneToFK") instanceof Fault);

        // test that taking a snapshot does not trigger a fault, and generally works well
        context.currentSnapshot(src2);
        assertTrue(src2.readPropertyDirectly("toOneToFK") instanceof Fault);
    }

    public void testDelete() throws Exception {
        ToOneFK2 src = context.newObject(ToOneFK2.class);
        ToOneFK1 target = context.newObject(ToOneFK1.class);
        src.setToOneToFK(target);
        context.commitChanges();

        src.setToOneToFK(null);
        context.deleteObjects(target);
        context.commitChanges();

        // test database data

        ObjectIdQuery refetch = new ObjectIdQuery(
                src.getObjectId(),
                false,
                ObjectIdQuery.CACHE_REFRESH);
        ToOneFK2 src2 = (ToOneFK2) Cayenne.objectForQuery(context1, refetch);
        assertNull(src.getToOneToFK());
        assertEquals(src.getObjectId(), src2.getObjectId());
    }
}
