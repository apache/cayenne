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

import org.apache.art.CompoundPkTestEntity;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.unit.CayenneCase;

public class CompoundPkChangeTest extends CayenneCase {

    private static final String key1v1 = "-key1-v1-";
    private static final String key2v1 = "-key2-v1-";
    private static final String key1v2 = "-key1-v2-";
    private static final String key2v2 = "-key2-v2-";
    private static final String key1v3 = "-key1-v3-";
    private static final String key2v3 = "-key2-v3-";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testCompoundPkChangeSingleElement() throws Exception {
        DataContext context = createDataContext();

        CompoundPkTestEntity dao = context
                .newObject(CompoundPkTestEntity.class);
        CompoundPkTestEntity refreshedDao = null;

        dao.setKey1(key1v1);
        dao.setKey2(key2v1);
        dao.setName("testing testing");

        context.commitChanges();
        assertEquals(key1v1, dao.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals(key2v1, dao.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY2_PK_COLUMN));

        refreshedDao = (CompoundPkTestEntity) context.refetchObject(dao.getObjectId());
        assertEquals(dao.getObjectId(), refreshedDao.getObjectId());

        dao.setKey2(key2v2);

        context.commitChanges();
        assertEquals(key1v1, dao.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals(key2v2, dao.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY2_PK_COLUMN));

        refreshedDao = (CompoundPkTestEntity) context.refetchObject(dao.getObjectId());
        assertEquals(dao.getObjectId(), refreshedDao.getObjectId());

        dao.setKey2(key2v3);

        context.commitChanges();
        assertEquals(key1v1, dao.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals(key2v3, dao.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY2_PK_COLUMN));

        refreshedDao = (CompoundPkTestEntity) context.refetchObject(dao.getObjectId());
        assertEquals(dao.getObjectId(), refreshedDao.getObjectId());

    }

    public void testCompoundPkChangeAllElements() throws Exception {
        DataContext context = createDataContext();

        CompoundPkTestEntity dao = context
                .newObject(CompoundPkTestEntity.class);
        CompoundPkTestEntity refreshedDao = null;

        dao.setKey1(key1v1);
        dao.setKey2(key2v1);
        dao.setName("testing testing");

        context.commitChanges();
        assertEquals(key1v1, dao.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals(key2v1, dao.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY2_PK_COLUMN));

        refreshedDao = (CompoundPkTestEntity) context.refetchObject(dao.getObjectId());
        assertEquals(dao.getObjectId(), refreshedDao.getObjectId());

        dao.setKey1(key1v2);
        dao.setKey2(key2v2);

        context.commitChanges();
        assertEquals(key1v2, dao.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals(key2v2, dao.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY2_PK_COLUMN));

        refreshedDao = (CompoundPkTestEntity) context.refetchObject(dao.getObjectId());
        assertEquals(dao.getObjectId(), refreshedDao.getObjectId());

        dao.setKey1(key1v3);
        dao.setKey2(key2v3);

        context.commitChanges();
        assertEquals(key1v3, dao.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals(key2v3, dao.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY2_PK_COLUMN));

        refreshedDao = (CompoundPkTestEntity) context.refetchObject(dao.getObjectId());
        assertEquals(dao.getObjectId(), refreshedDao.getObjectId());

    }
}
