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
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checks the consistency of objects resolved concurrently on a shared DataContext.
 * <p>
 * A painting row encodes a number in both PAINTING_TITLE and ESTIMATED_PRICE, and points to an artist derived from
 * the number. A writer keeps bumping the number while readers select paintings with different prefetches, so every
 * statement sees a different row state. The object must never mix two statements: the title and the price must
 * encode the same number, a resolved to-one target must be the artist of that number, and the snapshot version of an
 * object must never go back.
 */
public class DataContextConcurrentResolveIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private static final int PAINTINGS = 10;
    private static final int READERS = 6;

    private DataContext context;
    private TableHelper tArtist;
    private TableHelper tPainting;

    @BeforeEach
    public void setUp() {
        context = env.context();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
        tPainting = env.table("PAINTING")
                .setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "ESTIMATED_PRICE")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.BIGINT, Types.DECIMAL);
    }

    private static int artistFor(int n) {
        return 1 + n % 2;
    }

    private static String titleFor(int painting, int n) {
        return "p" + painting + "-" + n;
    }

    private static int numberFrom(String title) {
        return Integer.parseInt(title.substring(title.indexOf('-') + 1));
    }

    @Test
    public void concurrentSelectsWithDifferentPrefetches() throws Exception {
        tArtist.insert(1, "a1");
        tArtist.insert(2, "a2");
        for (int i = 1; i <= PAINTINGS; i++) {
            tPainting.insert(i, titleFor(i, 0), artistFor(0), 0);
        }

        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
        Map<ObjectId, Long> lastVersions = new ConcurrentHashMap<>();
        AtomicBoolean stop = new AtomicBoolean();
        CountDownLatch start = new CountDownLatch(1);

        List<Runnable> readers = List.of(
                () -> ObjectSelect.query(Painting.class).prefetch(Painting.TO_ARTIST.disjoint()).select(context),
                () -> ObjectSelect.query(Painting.class).prefetch(Painting.TO_ARTIST.joint()).select(context),
                () -> ObjectSelect.query(Painting.class).select(context),
                () -> ObjectSelect.query(Artist.class).prefetch(Artist.PAINTING_ARRAY.disjoint()).select(context));

        ExecutorService executor = Executors.newFixedThreadPool(READERS + 1);
        try {
            List<Future<?>> tasks = new ArrayList<>();

            // the writer changes all three encoded columns of a row in one statement
            tasks.add(executor.submit(() -> {
                start.await();
                for (int n = 1; !stop.get(); n++) {
                    for (int i = 1; i <= PAINTINGS; i++) {
                        tPainting.update()
                                .set("PAINTING_TITLE", titleFor(i, n))
                                .set("ESTIMATED_PRICE", new BigDecimal(n), Types.DECIMAL)
                                .set("ARTIST_ID", artistFor(n))
                                .where("PAINTING_ID", i)
                                .execute();
                    }
                }
                return null;
            }));

            for (int r = 0; r < READERS; r++) {
                Runnable select = readers.get(r % readers.size());
                tasks.add(executor.submit(() -> {
                    start.await();
                    while (!stop.get()) {
                        select.run();
                        for (Painting p : ObjectSelect.query(Painting.class).select(context)) {
                            checkConsistent(p, lastVersions);
                        }
                    }
                    return null;
                }));
            }

            start.countDown();
            Thread.sleep(2000);
            stop.set(true);

            for (Future<?> task : tasks) {
                try {
                    task.get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    failures.add(e.getCause() != null ? e.getCause() : e);
                }
            }
        } finally {
            stop.set(true);
            executor.shutdownNow();
        }

        if (!failures.isEmpty()) {
            Throwable first = failures.peek();
            fail(failures.size() + " failure(s), the first one: " + first, first);
        }

        // after the dust settles every object reflects the last statement that read it, and nothing is torn
        for (Painting p : ObjectSelect.query(Painting.class).prefetch(Painting.TO_ARTIST.disjoint()).select(context)) {
            checkConsistent(p, lastVersions);
            assertTrue(p.readPropertyDirectly(Painting.TO_ARTIST.getName()) instanceof Artist);
        }
    }

    /**
     * Checks an object under its lock, as the readers of this test are the only ones that care about seeing a complete
     * write.
     */
    private void checkConsistent(Painting p, Map<ObjectId, Long> lastVersions) {
        DataContextObjectStore store = context.getObjectStore();
        ObjectId id = p.getObjectId();

        store.withObjectLock(id, () -> {
            assertSame(p, store.getObject(id), "Two objects registered for " + id);
            assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());

            int n = numberFrom((String) p.readPropertyDirectly(Painting.PAINTING_TITLE.getName()));
            BigDecimal price = (BigDecimal) p.readPropertyDirectly(Painting.ESTIMATED_PRICE.getName());
            assertEquals(n, price.intValue(), "Title and price of " + id + " come from different statements");

            long version = p.getSnapshotVersion();
            Long lastVersion = lastVersions.put(id, version);
            if (lastVersion != null) {
                assertTrue(version >= lastVersion, "Snapshot version of " + id + " went back from " + lastVersion
                        + " to " + version);
            }

            Object artist = p.readPropertyDirectly(Painting.TO_ARTIST.getName());
            if (artist instanceof Artist a) {
                assertEquals(artistFor(n), Cayenne.intPKForObject(a),
                        "To-one of " + id + " comes from an older statement than its attributes");
            }
            return null;
        });
    }

    @Test
    public void olderSnapshotDoesNotOverwriteNewerObject() throws Exception {
        tArtist.insert(1, "a1");
        tPainting.insert(1, titleFor(1, 0), 1, 0);

        Painting p = Cayenne.objectForPK(context, Painting.class, 1);
        long version = p.getSnapshotVersion();

        DataRow dbRow = ObjectSelect.dataRowQuery(Painting.class).selectOne(context);
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor("Painting");

        // a snapshot of an earlier statement arriving late is ignored
        context.objectsFromDataRows(descriptor, List.of(copy(dbRow, "stale", version - 1)));
        assertEquals(titleFor(1, 0), p.getPaintingTitle());
        assertEquals(version, p.getSnapshotVersion());

        // a snapshot of the same statement is a no-op too, while a newer one is merged
        context.objectsFromDataRows(descriptor, List.of(copy(dbRow, "same", version)));
        assertEquals("same", p.getPaintingTitle());
        assertEquals(version, p.getSnapshotVersion());

        long newer = SnapshotVersion.next();
        context.objectsFromDataRows(descriptor, List.of(copy(dbRow, "newer", newer)));
        assertEquals("newer", p.getPaintingTitle());
        assertEquals(newer, p.getSnapshotVersion());
    }

    private static DataRow copy(DataRow row, String title, long version) {
        return row.merged(Map.of("PAINTING_TITLE", title), version);
    }

    @Test
    public void findOrCreateObjectIsAtomic() throws Exception {
        ObjectId id = ObjectId.of("Artist", "ARTIST_ID", 1);

        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Persistent>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    Persistent object = context.findOrCreateObject(id);

                    // the object is fully initialized by the time it is visible
                    assertEquals(PersistenceState.HOLLOW, object.getPersistenceState());
                    assertSame(context, object.getObjectContext());
                    return object;
                }));
            }
            start.countDown();

            Persistent first = results.getFirst().get(10, TimeUnit.SECONDS);
            for (Future<Persistent> result : results) {
                assertSame(first, result.get(10, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void childContextObjectTakesParentVersion() throws Exception {
        tArtist.insert(1, "a1");

        Artist a = Cayenne.objectForPK(context, Artist.class, 1);

        ObjectContext child = env.runtime().newContext(context);
        Artist childA = Cayenne.objectForPK(child, Artist.class, 1);

        assertNotSame(a, childA);
        assertEquals("a1", childA.getArtistName());
        assertEquals(a.getSnapshotVersion(), childA.getSnapshotVersion());
    }
}
