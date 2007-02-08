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

import org.apache.art.Artist;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.CayenneTestCase;

/**
 * This test case ensures that PK pre-generated for the entity manually before commit is
 * used during commit as well.
 * 
 * @author Andrus Adamchik
 */
// TODO: 1/16/2006 - the algorithm used to generate the PK may be included in
// DataObjectUtils to pull the PK on demand. A caveat - we need to analyze DataObject in
// question to see if a PK is numeric and not propagated.
public class PregeneratedPKTst extends CayenneTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testIntPk() throws Exception {

        DataContext context = createDataContext();
        Artist a = (Artist) context.createAndRegisterNewObject(Artist.class);
        a.setArtistName("XXX");

        updateId(a);

        Object pk = a.getObjectId().getReplacementIdMap().get(Artist.ARTIST_ID_PK_COLUMN);
        assertNotNull(pk);

        assertEquals(pk, new Integer(DataObjectUtils.intPKForObject(a)));

        context.commitChanges();

        Object pkAfterCommit = a.getObjectId().getIdSnapshot().get(
                Artist.ARTIST_ID_PK_COLUMN);
        assertEquals(pk, pkAfterCommit);
    }

    void updateId(Artist object) throws Exception {
        DbEntity entity = object.getDataContext().getEntityResolver().lookupDbEntity(
                object);
        DataNode node = object.getDataContext().getParentDataDomain().lookupDataNode(
                entity.getDataMap());

        Object pk = node
                .getAdapter()
                .getPkGenerator()
                .generatePkForDbEntity(node, entity); // throws Exception!!
        object.getObjectId().getReplacementIdMap().put(Artist.ARTIST_ID_PK_COLUMN, pk);
    }
}
