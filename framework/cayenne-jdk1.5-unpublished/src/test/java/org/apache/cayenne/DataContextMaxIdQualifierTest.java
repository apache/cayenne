package org.apache.cayenne;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Bag;
import org.apache.cayenne.testdo.testmap.Box;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextMaxIdQualifierTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    @Inject
    protected ServerRuntime runtime;

    protected TableHelper tBag;
    protected TableHelper tBox;

    @Override
    protected void setUpAfterInjection() throws Exception {

        runtime.getDataDomain().setMaxIdQualifierSite(100);

        dbHelper.deleteAll("BALL");
        dbHelper.deleteAll("BOX_THING");
        dbHelper.deleteAll("THING");
        dbHelper.deleteAll("BOX_INFO");
        dbHelper.deleteAll("BOX");
        dbHelper.deleteAll("BAG");

        tBag = new TableHelper(dbHelper, "BAG");
        tBag.setColumns("ID", "NAME");

        tBox = new TableHelper(dbHelper, "BOX");
        tBox.setColumns("ID", "BAG_ID", "NAME");
    }

    public void testDisjointByIdPrefetch() throws Exception {
        for (int i = 0; i < 1000; i++) {
            tBag.insert(i + 1, "bag" + (i + 1));
            tBox.insert(i + 1, i + 1, "box" + (i + 1));
        }

        final SelectQuery query = new SelectQuery(Bag.class);
        query.addPrefetch(Bag.BOXES_PROPERTY).setSemantics(
                PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);

        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                context.performQuery(query);
            }
        });

        assertEquals(11, queriesCount);
    }

    public void testIncrementalFaultList() throws Exception {
        tBag.insert(1, "bag1");
        for (int i = 0; i < 1000; i++) {
            tBox.insert(i + 1, 1, "box" + (i + 1));
        }

        final SelectQuery query = new SelectQuery(Box.class);
        query.setPageSize(100);
        int queriesCount = queryInterceptor.runWithQueryCounter(new UnitTestClosure() {

            public void execute() {
                final List<Box> boxes = context.performQuery(query);
                List<Box> tempList = new ArrayList<Box>();
                tempList.addAll(boxes);
            }
        });

        assertEquals(11, queriesCount);
    }
}
