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
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * This test case ensures that PK pre-generated for the entity manually before commit is
 * used during commit as well.
 */
// TODO: 1/16/2006 - the algorithm used to generate the PK may be included in
// DataObjectUtils to pull the PK on demand. A caveat - we need to analyze DataObject in
// question to see if a PK is numeric and not propagated.
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class PregeneratedPKIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Test
    public void testLongPk() throws Exception {
        Artist a = context.newObject(Artist.class);
        a.setArtistName("XXX");

        updateId(context, a.getObjectId());

        Object pk = a.getObjectId().getReplacementIdMap().get(Artist.ARTIST_ID_PK_COLUMN);
        assertNotNull(pk);

        assertEquals(pk, Cayenne.longPKForObject(a));

        context.commitChanges();

        Object pkAfterCommit = a.getObjectId().getIdSnapshot().get(
                Artist.ARTIST_ID_PK_COLUMN);
        assertEquals(pk, pkAfterCommit);
    }

    void updateId(DataContext context, ObjectId id) throws Exception {
        DbEntity entity = context.getEntityResolver().getDbEntity("ARTIST");
        DataNode node = context.getParentDataDomain().lookupDataNode(entity.getDataMap());

        Object pk = node.getAdapter().getPkGenerator().generatePk(
                node,
                entity.getPrimaryKeys().iterator().next());
        id.getReplacementIdMap().put(Artist.ARTIST_ID_PK_COLUMN, pk);
    }
}
