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
package org.apache.cayenne.access;

import static org.apache.cayenne.exp.ExpressionFactory.matchExp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Bag;
import org.apache.cayenne.testdo.testmap.Ball;
import org.apache.cayenne.testdo.testmap.Box;
import org.apache.cayenne.testdo.testmap.BoxInfo;
import org.apache.cayenne.testdo.testmap.Thing;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextDisjointByIdPrefetch_ExtrasTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    private ServerRuntime runtime;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    protected TableHelper tBag;
    protected TableHelper tBox;
    protected TableHelper tBoxInfo;
    protected TableHelper tBall;
    protected TableHelper tThing;
    protected TableHelper tBoxThing;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("BALL");
        dbHelper.deleteAll("BOX_THING");
        dbHelper.deleteAll("THING");
        dbHelper.deleteAll("BOX_INFO");
        dbHelper.deleteAll("BOX");
        dbHelper.deleteAll("BAG");

        tBoxThing = new TableHelper(dbHelper, "BOX_THING");
        tBoxThing.setColumns("BOX_ID", "THING_WEIGHT", "THING_VOLUME");
    }

    private void createBagWithTwoBoxesAndPlentyOfBallsDataSet() throws Exception {

        // because of SQLServer need to enable identity inserts per transaction,
        // inserting these objects via Cayenne, and then flushing the cache
        // http://technet.microsoft.com/en-us/library/ms188059.aspx

        Collection<Object> invalidate = new ArrayList<Object>();
        ObjectContext context = runtime.getContext();

        Bag b1 = context.newObject(Bag.class);
        invalidate.add(b1);
        b1.setName("b1");

        Box bx1 = context.newObject(Box.class);
        invalidate.add(bx1);
        bx1.setName("big");
        bx1.setBag(b1);

        BoxInfo bi1 = context.newObject(BoxInfo.class);
        invalidate.add(bi1);
        bi1.setColor("red");
        bi1.setBox(bx1);

        Box bx2 = context.newObject(Box.class);
        invalidate.add(bx2);
        bx2.setName("small");
        bx2.setBag(b1);

        BoxInfo bi2 = context.newObject(BoxInfo.class);
        invalidate.add(bi2);
        bi2.setColor("green");
        bi2.setBox(bx2);

        Thing t1 = context.newObject(Thing.class);
        invalidate.add(t1);
        t1.setVolume(10);
        t1.setWeight(10);

        Ball bl1 = context.newObject(Ball.class);
        invalidate.add(bl1);
        bl1.setBox(bx1);
        bl1.setThingVolume(10);
        bl1.setThingWeight(10);

        Thing t2 = context.newObject(Thing.class);
        invalidate.add(t2);
        t2.setVolume(20);
        t2.setWeight(20);

        Ball bl2 = context.newObject(Ball.class);
        invalidate.add(bl2);
        bl2.setBox(bx1);
        bl2.setThingVolume(20);
        bl2.setThingWeight(20);

        Thing t3 = context.newObject(Thing.class);
        invalidate.add(t3);
        t3.setVolume(30);
        t3.setWeight(30);

        Ball bl3 = context.newObject(Ball.class);
        invalidate.add(bl3);
        bl3.setBox(bx2);
        bl3.setThingVolume(30);
        bl3.setThingWeight(30);

        Thing t4 = context.newObject(Thing.class);
        invalidate.add(t4);
        t4.setVolume(40);
        t4.setWeight(40);

        Ball bl4 = context.newObject(Ball.class);
        invalidate.add(bl4);
        bl4.setBox(bx2);
        bl4.setThingVolume(40);
        bl4.setThingWeight(40);

        Thing t5 = context.newObject(Thing.class);
        invalidate.add(t5);
        t5.setVolume(20);
        t5.setWeight(10);

        Ball bl5 = context.newObject(Ball.class);
        invalidate.add(bl5);
        bl5.setBox(bx2);
        bl5.setThingVolume(20);
        bl5.setThingWeight(10);

        Thing t6 = context.newObject(Thing.class);
        invalidate.add(t6);
        t6.setVolume(40);
        t6.setWeight(30);

        Ball bl6 = context.newObject(Ball.class);
        invalidate.add(bl6);
        bl6.setBox(bx2);
        bl6.setThingVolume(40);
        bl6.setThingWeight(30);

        context.commitChanges();

        tBoxThing.insert(Cayenne.intPKForObject(bx1), t1.getWeight(), t1.getVolume());
        tBoxThing.insert(Cayenne.intPKForObject(bx1), t2.getWeight(), t2.getVolume());
        tBoxThing.insert(Cayenne.intPKForObject(bx2), t3.getWeight(), t3.getVolume());
        tBoxThing.insert(Cayenne.intPKForObject(bx1), t4.getWeight(), t4.getVolume());
        tBoxThing.insert(Cayenne.intPKForObject(bx1), t5.getWeight(), t5.getVolume());
        tBoxThing.insert(Cayenne.intPKForObject(bx1), t6.getWeight(), t6.getVolume());

        context.invalidateObjects(invalidate);
    }

    public void testFlattenedRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Bag.class);
        query.addPrefetch(Bag.BALLS_PROPERTY).setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
        final List<Bag> result = context.performQuery(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertFalse(result.isEmpty());
                Bag b1 = result.get(0);
                List<Ball> balls = (List<Ball>) b1.readPropertyDirectly(Bag.BALLS_PROPERTY);
                assertNotNull(balls);
                assertFalse(((ValueHolder) balls).isFault());
                assertEquals(6, balls.size());

                List<Integer> volumes = new ArrayList<Integer>();
                for (Ball b : balls) {
                    assertEquals(PersistenceState.COMMITTED, b.getPersistenceState());
                    volumes.add(b.getThingVolume());
                }
                assertTrue(volumes.containsAll(Arrays.asList(10, 20, 30, 40, 20, 40)));
            }
        });
    }

    public void testFlattenedMultiColumnRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Box.class);
        query.addPrefetch(Box.THINGS_PROPERTY).setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
        final List<Box> result = context.performQuery(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertFalse(result.isEmpty());
                List<Integer> volumes = new ArrayList<Integer>();
                for (Box box : result) {
                    List<Thing> things = (List<Thing>) box.readPropertyDirectly(Box.THINGS_PROPERTY);
                    assertNotNull(things);
                    assertFalse(((ValueHolder) things).isFault());
                    for (Thing t : things) {
                        assertEquals(PersistenceState.COMMITTED, t.getPersistenceState());
                        volumes.add(t.getVolume());
                    }
                }
                assertEquals(6, volumes.size());
                assertTrue(volumes.containsAll(Arrays.asList(10, 20, 30, 40)));
            }
        });
    }

    public void testLongFlattenedRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Bag.class);
        query.addPrefetch(Bag.THINGS_PROPERTY).setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
        final List<Bag> result = context.performQuery(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertFalse(result.isEmpty());
                Bag b1 = result.get(0);
                List<Thing> things = (List<Thing>) b1.readPropertyDirectly(Bag.THINGS_PROPERTY);
                assertNotNull(things);
                assertFalse(((ValueHolder) things).isFault());
                assertEquals(6, things.size());

                List<Integer> volumes = new ArrayList<Integer>();
                for (Thing t : things) {
                    assertEquals(PersistenceState.COMMITTED, t.getPersistenceState());
                    volumes.add(t.getVolume());
                }
                assertTrue(volumes.containsAll(Arrays.asList(10, 20, 20, 30, 40, 40)));
            }
        });
    }

    public void testMultiColumnRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Ball.class);
        query.orQualifier(matchExp(Ball.THING_VOLUME_PROPERTY, 40).andExp(matchExp(Ball.THING_WEIGHT_PROPERTY, 30)));
        query.orQualifier(matchExp(Ball.THING_VOLUME_PROPERTY, 20).andExp(matchExp(Ball.THING_WEIGHT_PROPERTY, 10)));

        query.addPrefetch(Ball.THING_PROPERTY).setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);

        final List<Ball> balls = context.performQuery(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                assertEquals(2, balls.size());

                balls.get(0).getThing().getVolume();
                balls.get(1).getThing().getVolume();
            }
        });
    }

    public void testJointPrefetchInParent() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Box.class);
        query.addPrefetch(Box.BALLS_PROPERTY).setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
        query.addPrefetch(Box.BALLS_PROPERTY + "." + Ball.THING_PROPERTY).setSemantics(
                PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
        final List<Box> result = context.performQuery(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertFalse(result.isEmpty());
                List<Integer> volumes = new ArrayList<Integer>();
                for (Box box : result) {
                    List<Ball> balls = (List<Ball>) box.readPropertyDirectly(Box.BALLS_PROPERTY);
                    assertNotNull(balls);
                    assertFalse(((ValueHolder) balls).isFault());
                    for (Ball ball : balls) {
                        Thing thing = (Thing) ball.readPropertyDirectly(Ball.THING_PROPERTY);
                        assertNotNull(thing);
                        assertEquals(PersistenceState.COMMITTED, thing.getPersistenceState());
                        volumes.add(thing.getVolume());
                    }
                }
                assertEquals(6, volumes.size());
                assertTrue(volumes.containsAll(Arrays.asList(10, 20, 30, 40)));
            }
        });
    }

    public void testJointPrefetchInChild() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Bag.class);
        query.addPrefetch(Bag.BOXES_PROPERTY).setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
        query.addPrefetch(Bag.BOXES_PROPERTY + "." + Box.BALLS_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
        final List<Bag> result = context.performQuery(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertFalse(result.isEmpty());

                Bag bag = result.get(0);
                List<Box> boxes = (List<Box>) bag.readPropertyDirectly(Bag.BOXES_PROPERTY);
                assertNotNull(boxes);
                assertFalse(((ValueHolder) boxes).isFault());
                assertEquals(2, boxes.size());

                Box big = null;
                List<String> names = new ArrayList<String>();
                for (Box box : boxes) {
                    assertEquals(PersistenceState.COMMITTED, box.getPersistenceState());
                    names.add(box.getName());
                    if (box.getName().equals("big")) {
                        big = box;
                    }
                }
                assertTrue(names.contains("big"));
                assertTrue(names.contains("small"));

                List<Ball> balls = (List<Ball>) big.readPropertyDirectly(Box.BALLS_PROPERTY);
                assertNotNull(balls);
                assertFalse(((ValueHolder) balls).isFault());
                assertEquals(2, balls.size());
                List<Integer> volumes = new ArrayList<Integer>();
                for (Ball ball : balls) {
                    assertEquals(PersistenceState.COMMITTED, ball.getPersistenceState());
                    volumes.add(ball.getThingVolume());
                }
                assertTrue(volumes.containsAll(Arrays.asList(10, 20)));
            }
        });
    }
}
