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

package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ObjectStoreDiffRetainingIT extends RuntimeCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH")
                .setColumnTypes(Types.BIGINT, Types.CHAR, Types.DATE);

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "ARTIST_ID",
                "ESTIMATED_PRICE",
                "GALLERY_ID",
                "PAINTING_ID",
                "PAINTING_TITLE").setColumnTypes(
                 Types.BIGINT,
                 Types.DECIMAL,
                 Types.INTEGER,
                 Types.INTEGER,
                 Types.VARCHAR);
    }

    protected void createMixedDataSet() throws Exception {
        tArtist.insert(2000, "artist with one painting", null);
        tPainting.insert(2000, null, null, 3000, "p1");
    }

    @Test
    public void testSnapshotRetainedOnPropertyModification() throws Exception {
        createMixedDataSet();

        Artist a = Cayenne.objectForPK(context, Artist.class, 2000);
        ObjectStore objectStore = context.getObjectStore();

        assertNull(objectStore.getChangesByObjectId().get(a.getObjectId()));

        a.setArtistName("some other name");
        assertNotNull(objectStore.getChangesByObjectId().get(a.getObjectId()));
    }

    @Test
    public void testSnapshotRetainedOnRelAndPropertyModification() throws Exception {
        createMixedDataSet();

        Artist a = Cayenne.objectForPK(context, Artist.class, 2000);
        ObjectStore objectStore = context.getObjectStore();

        assertNull(objectStore.getChangesByObjectId().get(a.getObjectId()));

        // we are trying to reproduce the bug CAY-213 - relationship modification puts
        // object in a modified state, so later when object is really modified, its
        // snapshot is not retained... in testing this I am leaving some flexibility for
        // the framework to retain a snapshot when it deems appropriate...

        a.addToPaintingArray(context.newObject(Painting.class));
        a.setArtistName("some other name");
        assertNotNull("Snapshot wasn't retained - CAY-213", objectStore
                .getChangesByObjectId()
                .get(a.getObjectId()));
    }
}
