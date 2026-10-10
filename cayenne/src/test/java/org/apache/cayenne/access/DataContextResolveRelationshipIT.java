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
import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DataContextResolveRelationshipIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;
    private TableHelper tArtist;
    private TableHelper tPainting;

    @BeforeEach
    public void setUp() {
        context = env.context();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
        tPainting = env.table("PAINTING", "PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID");
    }

    @Test
    public void toOneFromSnapshotCache() throws Exception {
        tArtist.insert(1, "a1");
        tPainting.insert(1, "p1", 1);

        Painting p = Cayenne.objectForPK(context, Painting.class, 1);

        // resolve the artist before resolving the relationship, so that the relationship is served from the cache
        Artist a = Cayenne.objectForPK(context, Artist.class, 1);
        long v = a.getSnapshotVersion();
        int writeCalls = a.getPropertyWrittenDirectly();
        assertEquals("a1", a.getArtistName());

        assertEquals(1, tArtist.update().set("ARTIST_NAME", "a2").where("ARTIST_ID", 1).execute());

        env.runWithQueriesBlocked(() -> {
            List<?> related = context.resolveRelationship(p.getObjectId(), Painting.TO_ARTIST.getName(), false);
            assertEquals(1, related.size());
            assertSame(a, related.getFirst());
        });

        assertEquals("a1", a.getArtistName());
        assertEquals(v, a.getSnapshotVersion());
        assertEquals(writeCalls, a.getPropertyWrittenDirectly(), "Cached to-one resolution refreshed the object");
    }

    @Test
    public void toOneNullFk() throws Exception {
        tPainting.insert(1, "p1", null);

        Painting p = Cayenne.objectForPK(context, Painting.class, 1);

        env.runWithQueriesBlocked(() -> {
            List<?> related = context.resolveRelationship(p.getObjectId(), Painting.TO_ARTIST.getName(), false);
            assertTrue(related.isEmpty());
        });
    }

    @Test
    public void toManyFetchedFromDbIsRefreshed() throws Exception {
        tArtist.insert(1, "a1");
        tPainting.insert(1, "p1", 1);

        Artist a = Cayenne.objectForPK(context, Artist.class, 1);
        Painting p = Cayenne.objectForPK(context, Painting.class, 1);
        assertEquals("p1", p.getPaintingTitle());

        assertEquals(1, tPainting.update().set("PAINTING_TITLE", "p2").where("PAINTING_ID", 1).execute());

        List<?> related = context.resolveRelationship(a.getObjectId(), Artist.PAINTING_ARRAY.getName(), false);
        assertEquals(1, related.size());
        assertSame(p, related.getFirst());
        assertEquals("p2", p.getPaintingTitle(), "To-many fetched from DB must refresh existing objects");
    }

    @Test
    public void toManyFromParentContext() throws Exception {
        tArtist.insert(1, "a1");
        tPainting.insert(1, "p1", 1);
        tPainting.insert(2, "p2", 1);

        Artist a = Cayenne.objectForPK(context, Artist.class, 1);
        assertEquals(2, a.getPaintingArray().size());

        ObjectContext child = env.runtime().newContext(context);
        Artist childA = child.localObject(a);

        env.runWithQueriesBlocked(() -> {
            List<? extends Persistent> related = child.getChannel().onRelationshipQuery(child, childA.getObjectId(), Artist.PAINTING_ARRAY.getName());
            assertEquals(2, related.size());
            for (Persistent p : related) {
                assertSame(child, p.getObjectContext());
            }
        });
    }

    @Test
    public void toOneFromParentContext() throws Exception {
        tArtist.insert(1, "a1");
        tPainting.insert(1, "p1", 1);

        Painting p = Cayenne.objectForPK(context, Painting.class, 1);
        Artist a = p.getToArtist();
        assertEquals("a1", a.getArtistName());

        ObjectContext child = env.runtime().newContext(context);
        Painting childP = child.localObject(p);

        env.runWithQueriesBlocked(() -> {
            List<? extends Persistent> related = child.getChannel().onRelationshipQuery(child, childP.getObjectId(),
                    Painting.TO_ARTIST.getName());
            assertEquals(1, related.size());
            assertSame(child, related.getFirst().getObjectContext());
            assertEquals(a.getObjectId(), related.getFirst().getObjectId());
        });
    }

    @Test
    public void newObjectInParentContext() {
        Artist a = context.newObject(Artist.class);
        a.setArtistName("a1");
        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("p1");
        a.addToPaintingArray(p);

        ObjectContext child = env.runtime().newContext(context);
        Artist childA = child.localObject(a);

        // a NEW object is unknown to the database, so its relationships must be resolved from the parent context
        env.runWithQueriesBlocked(() -> {
            List<? extends Persistent> related = child.getChannel().onRelationshipQuery(child, childA.getObjectId(),
                    Artist.PAINTING_ARRAY.getName());
            assertEquals(1, related.size());
            assertSame(child, related.getFirst().getObjectContext());
            assertEquals("p1", ((Painting) related.getFirst()).getPaintingTitle());
        });
    }

    // see https://issues.apache.org/jira/browse/CAY-3045
    @Test
    public void toOneFaultResolvedConcurrently() throws Exception {
        Artist a = context.newObject(Artist.class);
        a.setArtistName("a1");

        // using custom subclass of Painting to ensure the test is deterministic
        HandoffPainting p = new HandoffPainting();

        // the subclass is not mapped, so the entity has to be named explicitly
        p.setObjectId(ObjectId.of("Painting"));

        p.setPaintingTitle("p1");
        p.setToArtist(a);
        context.commitChanges();

        // turn the committed object's to-one back into a fault
        context.invalidateObjects(p);
        p.getPaintingTitle();
        assertTrue(p.readPropertyDirectly(Painting.TO_ARTIST.getName()) instanceof Fault);

        p.readerThread = Thread.currentThread();
        Thread resolver = new Thread(() -> {
            p.await(p.faultRead);
            p.getToArtist();
            p.resolved.countDown();
        }, "to-one-resolver");
        resolver.start();

        try {
            List<?> related = context.resolveRelationship(p.getObjectId(), Painting.TO_ARTIST.getName(), false);
            assertEquals(1, related.size());
            assertSame(a, related.getFirst());
        } finally {
            // unblock the other thread on a failure, so that it doesn't hang the test
            p.faultRead.countDown();
            resolver.join(TimeUnit.SECONDS.toMillis(10));
        }

        assertTrue(p.handedOff, "Reader thread never reached the to-one property");
        assertSame(a, p.getToArtist());
    }

    /**
     * Pauses the first read of the "toArtist" property on the designated reader thread, until another thread
     * signals that it has resolved the fault.
     */
    static class HandoffPainting extends Painting {

        final CountDownLatch faultRead = new CountDownLatch(1);
        final CountDownLatch resolved = new CountDownLatch(1);
        volatile Thread readerThread;
        volatile boolean handedOff;

        @Override
        public Object readPropertyDirectly(String propName) {
            Object value = super.readPropertyDirectly(propName);

            if (Thread.currentThread() == readerThread && !handedOff && TO_ARTIST.getName().equals(propName)) {
                handedOff = true;
                faultRead.countDown();
                await(resolved);
            }

            return value;
        }

        void await(CountDownLatch latch) {
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    fail("Timed out waiting for the other thread");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
