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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Tests objects registration in DataContext, transferring objects between contexts and
 * such.
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextObjectTrackingIT extends RuntimeCase {

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected CayenneRuntime runtime;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "ARTIST_ID",
                "PAINTING_TITLE",
                "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER,
                Types.BIGINT,
                Types.VARCHAR,
                Types.DECIMAL);
    }

    protected void createArtistsDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tArtist.insert(33004, "artist4");
    }

    protected void createMixedDataSet() throws Exception {
        tArtist.insert(33003, "artist3");
        tPainting.insert(33003, 33003, "P_artist3", 3000);
    }

    @Test
    public void testUnregisterObject() {

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", 1);
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        Persistent obj = context.objectFromDataRow(Artist.class, row);
        ObjectId oid = obj.getObjectId();

        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertSame(context, obj.getObjectContext());
        assertSame(obj, context.getGraphManager().getNode(oid));

        context.unregisterObjects(Collections.singletonList(obj));

        assertEquals(PersistenceState.TRANSIENT, obj.getPersistenceState());
        assertNull(obj.getObjectContext());
        assertEquals(oid, obj.getObjectId());
        assertNull(context.getGraphManager().getNode(oid));
        assertNull(context.getObjectStore().getCachedSnapshot(oid));
    }

    @Test
    public void testInvalidateObjects_Vararg() {

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", 1);
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        Persistent obj = context.objectFromDataRow(Artist.class, row);
        ObjectId oid = obj.getObjectId();

        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertSame(context, obj.getObjectContext());
        assertSame(obj, context.getGraphManager().getNode(oid));

        context.invalidateObjects(obj);

        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());
        assertSame(context, obj.getObjectContext());
        assertSame(oid, obj.getObjectId());
        assertNull(context.getObjectStore().getCachedSnapshot(oid));
        assertNotNull(context.getGraphManager().getNode(oid));
    }
}
