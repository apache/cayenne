package org.apache.cayenne.access;

import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Bag;
import org.apache.cayenne.testdo.testmap.Ball;
import org.apache.cayenne.testdo.testmap.Box;
import org.apache.cayenne.testdo.testmap.BoxInfo;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

import java.util.List;

import static org.apache.cayenne.exp.ExpressionFactory.matchExp;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextDisjointByIdPrefetchTest extends ServerCase {
    @Inject
    protected DataContext context;

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

        tBag = new TableHelper(dbHelper, "BAG");
        tBag.setColumns("ID");

        tBox = new TableHelper(dbHelper, "BOX");
        tBox.setColumns("ID", "BAG_ID");

        tBoxInfo = new TableHelper(dbHelper, "BOX_INFO");
        tBoxInfo.setColumns("ID", "BOX_ID", "COLOR");

        tBall = new TableHelper(dbHelper, "BALL");
        tBall.setColumns("ID", "BOX_ID", "THING_WEIGHT", "THING_VOLUME");

        tThing = new TableHelper(dbHelper, "THING");
        tThing.setColumns("ID", "WEIGHT", "VOLUME");

        tBoxThing = new TableHelper(dbHelper, "BOX_THING");
        tBoxThing.setColumns("BOX_ID", "THING_WEIGHT", "THING_VOLUME");
    }

    private void createBagWithTwoBoxesDataSet() throws Exception {
        tBag.insert(1);
        tBox.insert(1, 1);
        tBox.insert(2, 1);
    }

    private void createThreeBagsWithPlentyOfBoxesDataSet() throws Exception {
        tBag.insert(1);
        tBag.insert(2);
        tBag.insert(3);

        tBox.insert(1, 1);
        tBox.insert(2, 1);
        tBox.insert(3, 1);
        tBox.insert(4, 1);
        tBox.insert(5, 1);

        tBox.insert(6, 2);
        tBox.insert(7, 2);

        tBox.insert(8, 3);
        tBox.insert(9, 3);
        tBox.insert(10, 3);
    }

    private void createBagWithTwoBoxesAndPlentyOfBallsDataSet() throws Exception {
        tBag.insert(1);
        tBox.insert(1, 1);
        tBoxInfo.insert(1, 1, "red");
        tBox.insert(2, 1);
        tBoxInfo.insert(2, 2, "green");

        tThing.insert(1, 10, 10);
        tBoxThing.insert(1, 10, 10);
        tBall.insert(1, 1, 10, 10);

        tThing.insert(2, 20, 20);
        tBoxThing.insert(1, 20, 20);
        tBall.insert(2, 1, 20, 20);

        tThing.insert(3, 30, 30);
        tBoxThing.insert(2, 30, 30);
        tBall.insert(3, 2, 30, 30);

        tThing.insert(4, 40, 40);
        tBoxThing.insert(2, 40, 40);
        tBall.insert(4, 2, 40, 40);

        tThing.insert(5, 10, 20);
        tBoxThing.insert(2, 10, 20);
        tBall.insert(5, 2, 10, 20);

        tThing.insert(6, 30, 40);
        tBoxThing.insert(2, 30, 40);
        tBall.insert(6, 2, 30, 40);
    }

    public void testBasic() throws Exception {
        createBagWithTwoBoxesDataSet();

        SelectQuery query = new SelectQuery(Bag.class);
        query.addPrefetch(Bag.BOXES_PROPERTY).setSemantics(
                PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);

        final List<Bag> result = context.performQuery(query);
        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertFalse(result.isEmpty());
                Bag b1 = result.get(0);
                List<?> toMany = (List<?>) b1.readPropertyDirectly(Bag.BOXES_PROPERTY);
                assertNotNull(toMany);
                assertFalse(((ValueHolder) toMany).isFault());
            }
        });
    }

    public void testFetchLimit() throws Exception {
        createThreeBagsWithPlentyOfBoxesDataSet();

        final SelectQuery query = new SelectQuery(Bag.class);
        query.addPrefetch(Bag.BOXES_PROPERTY).setSemantics(
                PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
        query.setFetchLimit(2);

        // There will be only 2 bags in a result. The first bag has 5 boxes and
        // the second has 2. So we are expecting exactly 9 snapshots in the data
        // row store after performing the query.
        context.performQuery(query);

        assertEquals(9, context.getObjectStore().getDataRowCache().size());
    }

    public void testToOneRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Box.class);
        query.addPrefetch(Box.BOX_INFO_PROPERTY)
                .setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
        final List<Box> result = context.performQuery(query);
        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {
            public void execute() {
                assertFalse(result.isEmpty());
                Box b1 = result.get(0);
                BoxInfo info = (BoxInfo) b1.readPropertyDirectly(Box.BOX_INFO_PROPERTY);
                assertNotNull(info);
                assertEquals("red", info.getColor());
            }
        });
    }

    public void testFlattenedRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Bag.class);
        query.addPrefetch(Bag.BALLS_PROPERTY)
                .setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
        final List<Bag> result = context.performQuery(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {
            public void execute() {
                assertFalse(result.isEmpty());
                Bag b1 = result.get(0);
                List<?> toMany = (List<?>) b1.readPropertyDirectly(Bag.BALLS_PROPERTY);
                assertNotNull(toMany);
                assertFalse(((ValueHolder) toMany).isFault());
                assertEquals(6, toMany.size());
            }
        });
    }

    public void testMultiColumnRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Ball.class);
        query.orQualifier(
                matchExp(Ball.THING_VOLUME_PROPERTY, 40).andExp(matchExp(Ball.THING_WEIGHT_PROPERTY, 30)));
        query.orQualifier(
                matchExp(Ball.THING_VOLUME_PROPERTY, 20).andExp(matchExp(Ball.THING_WEIGHT_PROPERTY, 10)));
        query.addPrefetch(Ball.THING_PROPERTY)
                .setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
        context.performQuery(query);

        assertEquals(4, context.getObjectStore().getDataRowCache().size());
    }

    public void testFlattenedMultiColumnRelationship() throws Exception {
        createBagWithTwoBoxesAndPlentyOfBallsDataSet();

        SelectQuery query = new SelectQuery(Box.class);
        query.addPrefetch(Box.THINGS_PROPERTY)
                .setSemantics(PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
        final List<Box> result = context.performQuery(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {
            public void execute() {
                assertFalse(result.isEmpty());
                Box b1 = result.get(0);
                List<?> toMany = (List<?>) b1.readPropertyDirectly(Box.THINGS_PROPERTY);
                assertNotNull(toMany);
                assertFalse(((ValueHolder) toMany).isFault());
                assertEquals(2, toMany.size());
            }
        });
    }
}
