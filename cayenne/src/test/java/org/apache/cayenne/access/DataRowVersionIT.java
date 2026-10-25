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
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Snapshot versions are assigned per unit of work: all rows read by one SQL statement, and all snapshots of one
 * commit, share a version, and versions grow from one unit of work to the next.
 */
public class DataRowVersionIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;
    private TableHelper tArtist;
    private TableHelper tPainting;

    @BeforeEach
    public void setUp() throws Exception {
        context = env.context();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
        tPainting = env.table("PAINTING", "PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID");

        tArtist.insert(1, "a1").insert(2, "a2").insert(3, "a3");
        tPainting.insert(1, "p1", 1).insert(2, "p2", 1).insert(3, "p3", 2);
    }

    @Test
    public void rowsOfOneStatementShareVersion() {
        List<DataRow> rows = ObjectSelect.dataRowQuery(Artist.class).select(context);
        assertEquals(3, rows.size());

        long version = rows.getFirst().getVersion();
        assertNotEquals(Persistent.DEFAULT_VERSION, version);
        for (DataRow row : rows) {
            assertEquals(version, row.getVersion());
        }
    }

    @Test
    public void versionsGrowAcrossStatements() {
        long v1 = ObjectSelect.dataRowQuery(Artist.class).select(context).getFirst().getVersion();
        long v2 = ObjectSelect.dataRowQuery(Artist.class).select(context).getFirst().getVersion();
        assertTrue(v2 > v1, v1 + " -> " + v2);
    }

    @Test
    public void objectsOfOneStatementShareVersion() {
        List<Artist> artists = ObjectSelect.query(Artist.class).select(context);
        assertEquals(3, artists.size());

        long version = artists.getFirst().getSnapshotVersion();
        assertNotEquals(Persistent.DEFAULT_VERSION, version);
        for (Artist a : artists) {
            assertEquals(version, a.getSnapshotVersion());
            assertEquals(version, context.getObjectStore().getCachedSnapshot(a.getObjectId()).getVersion());
        }
    }

    @Test
    public void jointPrefetchSharesVersionWithRoot() {
        List<Painting> paintings = ObjectSelect.query(Painting.class)
                .prefetch(Painting.ARTIST.joint())
                .select(context);
        assertEquals(3, paintings.size());

        long version = paintings.getFirst().getSnapshotVersion();
        for (Painting p : paintings) {
            assertEquals(version, p.getSnapshotVersion());
            assertEquals(version, p.getArtist().getSnapshotVersion());
        }
    }

    @Test
    public void disjointPrefetchIsALaterStatement() {
        List<Painting> paintings = ObjectSelect.query(Painting.class)
                .prefetch(Painting.ARTIST.disjoint())
                .select(context);
        assertEquals(3, paintings.size());

        long rootVersion = paintings.getFirst().getSnapshotVersion();
        long prefetchVersion = paintings.getFirst().getArtist().getSnapshotVersion();
        assertTrue(prefetchVersion > rootVersion, rootVersion + " -> " + prefetchVersion);
        for (Painting p : paintings) {
            assertEquals(rootVersion, p.getSnapshotVersion());
            assertEquals(prefetchVersion, p.getArtist().getSnapshotVersion());
        }
    }

    @Test
    public void snapshotsOfOneCommitShareVersion() {
        Artist a = context.newObject(Artist.class);
        a.setArtistName("a4");
        Artist b = context.newObject(Artist.class);
        b.setArtistName("a5");
        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("p4");
        p.setArtist(a);
        context.commitChanges();

        long version = a.getSnapshotVersion();
        assertNotEquals(Persistent.DEFAULT_VERSION, version);
        for (Persistent o : List.of(a, b, p)) {
            assertEquals(version, o.getSnapshotVersion());
            assertEquals(version, context.getObjectStore().getCachedSnapshot(o.getObjectId()).getVersion());
        }

        a.setArtistName("a4+");
        p.setPaintingTitle("p4+");
        context.commitChanges();

        long version2 = a.getSnapshotVersion();
        assertTrue(version2 > version, version + " -> " + version2);
        assertEquals(version2, p.getSnapshotVersion());
        assertEquals(version, b.getSnapshotVersion(), "untouched object keeps its version");
    }

    @Test
    public void commitIsLaterThanPrecedingRead() {
        Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("a1")).selectOne(context);
        long readVersion = a.getSnapshotVersion();

        a.setArtistName("a1+");
        context.commitChanges();
        assertTrue(a.getSnapshotVersion() > readVersion, readVersion + " -> " + a.getSnapshotVersion());
    }

    @Test
    public void sameVersionRowIsNotReapplied() {
        Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("a1")).selectOne(context);
        DataRowStore cache = context.getObjectStore().getDataRowCache();
        DataRow cached = cache.getCachedSnapshot(a.getObjectId());

        // a row with the cached row's version is a row of the same statement: it carries the same data, so the
        // cache keeps what it has even if asked to refresh and even if the new row differs
        DataRow sameStatement = cached.merged(Map.of("ARTIST_NAME", "tampered"), cached.getVersion());

        cache.snapshotsUpdatedForObjects(List.of(a), List.of(sameStatement), true);
        assertSame(cached, cache.getCachedSnapshot(a.getObjectId()));
        assertEquals(cached.getVersion(), a.getSnapshotVersion());
    }

    @Test
    public void equalRowOfLaterStatementKeepsCachedRow() {
        Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("a1")).selectOne(context);
        DataRowStore cache = context.getObjectStore().getDataRowCache();
        DataRow cached = cache.getCachedSnapshot(a.getObjectId());

        // a later statement that read the same data leaves the cache alone: the cached row keeps its version, as it
        // tracks the last change of the data, not the last read
        ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("a1")).selectOne(context);
        assertSame(cached, cache.getCachedSnapshot(a.getObjectId()));
        assertTrue(a.getSnapshotVersion() > cached.getVersion(), "object reflects the later statement");
    }

    @Test
    public void changedRowOfLaterStatementReplacesCachedRow() throws Exception {
        Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("a1")).selectOne(context);
        DataRowStore cache = context.getObjectStore().getDataRowCache();
        DataRow cached = cache.getCachedSnapshot(a.getObjectId());

        tArtist.update().set("ARTIST_NAME", "a1+").where("ARTIST_ID", 1).execute();
        ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("a1+")).selectOne(context);

        DataRow replaced = cache.getCachedSnapshot(a.getObjectId());
        assertEquals("a1+", replaced.get("ARTIST_NAME"));
        assertTrue(replaced.getVersion() > cached.getVersion(), cached.getVersion() + " -> " + replaced.getVersion());
        assertEquals(replaced.getVersion(), a.getSnapshotVersion());
    }
}
