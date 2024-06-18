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

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.things.Bag;
import org.apache.cayenne.testdo.things.Ball;
import org.apache.cayenne.testdo.things.Box;
import org.apache.cayenne.testdo.things.Thing;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.THINGS_PROJECT)
public class DataContextDisjointByIdPrefetch_ExtrasIT extends RuntimeCase {

    @Inject
    protected DataContext context;

    @Inject
    private CayenneRuntime runtime;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    protected TableHelper tBag;
    protected TableHelper tBall;
    protected TableHelper tBox;
    protected TableHelper tBoxInfo;
    protected TableHelper tBoxThing;
    protected TableHelper tThing;

    @Before
    public void setUp() throws Exception {
        tBag = new TableHelper(dbHelper, "BAG");
        tBag.setColumns("ID", "NAME");

        tBall = new TableHelper(dbHelper, "BALL");
        tBall.setColumns("ID", "BOX_ID", "THING_VOLUME", "THING_WEIGHT");

        tBox = new TableHelper(dbHelper, "BOX");
        tBox.setColumns("ID", "BAG_ID", "NAME");

        tBoxInfo = new TableHelper(dbHelper, "BOX_INFO");
        tBoxInfo.setColumns("ID" ,"BOX_ID", "COLOR");

        tBoxThing = new TableHelper(dbHelper, "BOX_THING");
        tBoxThing.setColumns("BOX_ID", "THING_VOLUME", "THING_WEIGHT");

        tThing = new TableHelper(dbHelper, "THING");
        tThing.setColumns("ID", "VOLUME", "WEIGHT");
    }

    private void createBagWithTwoBoxesAndPlentyOfBallsDataSet() throws Exception {

        // because of SQLServer need to enable identity inserts per transaction,
        // inserting these objects via Cayenne, and then flushing the cache
        // http://technet.microsoft.com/en-us/library/ms188059.aspx

        tBag.insert(1, "b1");
        tBox.insert(1, 1, "big");
        tBoxInfo.insert(1, 1, "red");
        tBox.insert(2, 1, "small");
        tBoxInfo.insert(2, 2, "green");
        tThing.insert(1, 10, 10);
        tBall.insert(1, 1, 10, 10);
        tThing.insert(2, 20, 20);
        tBall.insert(2, 1, 20, 20);
        tThing.insert(3, 30, 30);
        tBall.insert(3, 2, 30, 30);
        tThing.insert(4, 40, 40);
        tBall.insert(4, 2, 40, 40);
        tThing.insert(5, 20, 10);
        tBall.insert(5, 2, 20, 10);
        tThing.insert(6, 40, 30);
        tBall.insert(6, 2, 40, 30);

        tBoxThing.insert(1, 10, 10);
        tBoxThing.insert(1, 20, 20);
        tBoxThing.insert(2, 30, 30);
        tBoxThing.insert(1, 40, 40);
        tBoxThing.insert(1, 20, 10);
        tBoxThing.insert(1, 40, 30);

    }

    @Test
    public void testFlattenedRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        final List<Bag> result = ObjectSelect.query(Bag.class)
                .prefetch(Bag.BALLS.disjointById())
                .select(context);

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertFalse(result.isEmpty());
            Bag b1 = result.get(0);
            @SuppressWarnings("unchecked")
            List<Ball> balls = (List<Ball>) b1.readPropertyDirectly(Bag.BALLS.getName());
            assertNotNull(balls);
            assertFalse(((ValueHolder) balls).isFault());
            assertEquals(6, balls.size());

            List<Integer> volumes = new ArrayList<>();
            for (Ball b : balls) {
                assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
                volumes.add(b.getThingVolume());
            }
            assertThat(volumes, hasItems(10, 20, 30, 40, 20, 40));
        });
    }

    @Test
    public void testFlattenedMultiColumnRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        final List<Box> result = ObjectSelect.query(Box.class)
                .prefetch(Box.THINGS.disjointById())
                .select(context);

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertFalse(result.isEmpty());
            List<Integer> volumes = new ArrayList<>();
            for (Box box : result) {
                @SuppressWarnings("unchecked")
                List<Thing> things = (List<Thing>) box.readPropertyDirectly(Box.THINGS.getName());
                assertNotNull(things);
                assertFalse(((ValueHolder) things).isFault());
                for (Thing t : things) {
                    assertEquals(PersistenceState.COMMITTED, t.getPersistenceState());
                    volumes.add(t.getVolume());
                }
            }
            assertEquals(6, volumes.size());
            assertTrue(volumes.containsAll(Arrays.asList(10, 20, 30, 40)));
        });
    }

    @Test
    public void testLongFlattenedRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        final List<Bag> result = ObjectSelect.query(Bag.class)
                .prefetch(Box.THINGS.disjointById())
                .select(context);

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertFalse(result.isEmpty());
            Bag b1 = result.get(0);
            @SuppressWarnings("unchecked")
            List<Thing> things = (List<Thing>) b1.readPropertyDirectly(Bag.THINGS.getName());
            assertNotNull(things);
            assertFalse(((ValueHolder) things).isFault());
            assertEquals(6, things.size());

            List<Integer> volumes = new ArrayList<>();
            for (Thing t : things) {
                assertEquals(PersistenceState.COMMITTED, t.getPersistenceState());
                volumes.add(t.getVolume());
            }
            assertTrue(volumes.containsAll(Arrays.asList(10, 20, 20, 30, 40, 40)));
        });
    }

    @Test
    public void testMultiColumnRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        final List<Ball> balls = ObjectSelect.query(Ball.class)
                .or(Ball.THING_VOLUME.eq(40).andExp(Ball.THING_WEIGHT.eq(30)))
                .or(Ball.THING_VOLUME.eq(20).andExp(Ball.THING_WEIGHT.eq(10)))
                .prefetch(Ball.THING.disjointById())
                .select(context);

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertEquals(2, balls.size());
            for(Ball next : balls) {
                assertNotNull(balls.get(0).getThing());
                next.getThing().getVolume();
            }
        });
    }

    @Test
    public void testJointPrefetchInParent() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        final List<Box> result = ObjectSelect.query(Box.class)
                .prefetch(Box.BALLS.disjointById())
                .prefetch(Box.BALLS.dot(Ball.THING).disjointById())
                .select(context);

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertFalse(result.isEmpty());
            List<Integer> volumes = new ArrayList<>();
            for (Box box : result) {
                @SuppressWarnings("unchecked")
                List<Ball> balls = (List<Ball>) box.readPropertyDirectly(Box.BALLS.getName());
                assertNotNull(balls);
                assertFalse(((ValueHolder) balls).isFault());
                for (Ball ball : balls) {
                    Thing thing = (Thing) ball.readPropertyDirectly(Ball.THING.getName());
                    assertNotNull(thing);
                    assertEquals(PersistenceState.COMMITTED, thing.getPersistenceState());
                    volumes.add(thing.getVolume());
                }
            }
            assertEquals(6, volumes.size());
            assertTrue(volumes.containsAll(Arrays.asList(10, 20, 30, 40)));
        });
    }

    @Test
    public void testJointPrefetchInChild() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        final List<Bag> result = ObjectSelect.query(Bag.class)
                .prefetch(Bag.BOXES.disjointById())
                .prefetch(Bag.BOXES.dot(Box.BALLS).joint())
                .select(context);

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertFalse(result.isEmpty());

            Bag bag = result.get(0);
            @SuppressWarnings("unchecked")
            List<Box> boxes = (List<Box>) bag.readPropertyDirectly(Bag.BOXES.getName());
            assertNotNull(boxes);
            assertFalse(((ValueHolder) boxes).isFault());
            assertEquals(2, boxes.size());

            Box big = null;
            List<String> names = new ArrayList<>();
            for (Box box : boxes) {
                assertEquals(PersistenceState.COMMITTED, box.getPersistenceState());
                names.add(box.getName());
                if (box.getName().equals("big")) {
                    big = box;
                }
            }
            assertTrue(names.contains("big"));
            assertTrue(names.contains("small"));

            @SuppressWarnings("unchecked")
            List<Ball> balls = (List<Ball>) big.readPropertyDirectly(Box.BALLS.getName());
            assertNotNull(balls);
            assertFalse(((ValueHolder) balls).isFault());
            assertEquals(2, balls.size());
            List<Integer> volumes = new ArrayList<>();
            for (Ball ball : balls) {
                assertEquals(PersistenceState.COMMITTED, ball.getPersistenceState());
                volumes.add(ball.getThingVolume());
            }
            assertTrue(volumes.containsAll(Arrays.asList(10, 20)));
        });
    }
}
