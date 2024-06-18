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
package org.apache.cayenne;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.COMPOUND_PROJECT)
public class CompoundPkChangeIT extends RuntimeCase {

    private static final String key1v1 = "-key1-v1-";
    private static final String key2v1 = "-key2-v1-";
    private static final String key1v2 = "-key1-v2-";
    private static final String key2v2 = "-key2-v2-";
    private static final String key1v3 = "-key1-v3-";
    private static final String key2v3 = "-key2-v3-";

    @Inject
    private DataContext context;

    @Test
    public void testCompoundPkChangeSingleElement() throws Exception {

        CompoundPkTestEntity object = context.newObject(CompoundPkTestEntity.class);
        CompoundPkTestEntity refreshedObject = null;

        object.setKey1(key1v1);
        object.setKey2(key2v1);
        object.setName("testing testing");

        context.commitChanges();
        assertEquals(key1v1, object.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals(key2v1, object.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY2_PK_COLUMN));

        ObjectIdQuery refetch = new ObjectIdQuery(
                object.getObjectId(),
                false,
                ObjectIdQuery.CACHE_REFRESH);
        refreshedObject = (CompoundPkTestEntity) Cayenne.objectForQuery(context, refetch);
        assertEquals(object.getObjectId(), refreshedObject.getObjectId());

        object.setKey2(key2v2);

        context.commitChanges();
        assertEquals(key1v1, object.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals(key2v2, object.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY2_PK_COLUMN));

        ObjectIdQuery refetch1 = new ObjectIdQuery(
                object.getObjectId(),
                false,
                ObjectIdQuery.CACHE_REFRESH);
        refreshedObject = (CompoundPkTestEntity) Cayenne
                .objectForQuery(context, refetch1);
        assertEquals(object.getObjectId(), refreshedObject.getObjectId());

        object.setKey2(key2v3);

        context.commitChanges();
        assertEquals(key1v1, object.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals(key2v3, object.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY2_PK_COLUMN));

        ObjectIdQuery refetch2 = new ObjectIdQuery(
                object.getObjectId(),
                false,
                ObjectIdQuery.CACHE_REFRESH);
        refreshedObject = (CompoundPkTestEntity) Cayenne
                .objectForQuery(context, refetch2);
        assertEquals(object.getObjectId(), refreshedObject.getObjectId());
    }

    @Test
    public void testCompoundPkChangeAllElements() throws Exception {

        CompoundPkTestEntity object = context.newObject(CompoundPkTestEntity.class);
        CompoundPkTestEntity refreshedObject = null;

        object.setKey1(key1v1);
        object.setKey2(key2v1);
        object.setName("testing testing");

        context.commitChanges();
        assertEquals(key1v1, object.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals(key2v1, object.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY2_PK_COLUMN));

        ObjectIdQuery refetch = new ObjectIdQuery(
                object.getObjectId(),
                false,
                ObjectIdQuery.CACHE_REFRESH);
        refreshedObject = (CompoundPkTestEntity) Cayenne.objectForQuery(context, refetch);
        assertEquals(object.getObjectId(), refreshedObject.getObjectId());

        object.setKey1(key1v2);
        object.setKey2(key2v2);

        context.commitChanges();
        assertEquals(key1v2, object.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals(key2v2, object.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY2_PK_COLUMN));

        ObjectIdQuery refetch1 = new ObjectIdQuery(
                object.getObjectId(),
                false,
                ObjectIdQuery.CACHE_REFRESH);
        refreshedObject = (CompoundPkTestEntity) Cayenne
                .objectForQuery(context, refetch1);
        assertEquals(object.getObjectId(), refreshedObject.getObjectId());

        object.setKey1(key1v3);
        object.setKey2(key2v3);

        context.commitChanges();
        assertEquals(key1v3, object.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals(key2v3, object.getObjectId().getIdSnapshot().get(
                CompoundPkTestEntity.KEY2_PK_COLUMN));

        ObjectIdQuery refetch2 = new ObjectIdQuery(
                object.getObjectId(),
                false,
                ObjectIdQuery.CACHE_REFRESH);
        refreshedObject = (CompoundPkTestEntity) Cayenne
                .objectForQuery(context, refetch2);
        assertEquals(object.getObjectId(), refreshedObject.getObjectId());
    }
}
