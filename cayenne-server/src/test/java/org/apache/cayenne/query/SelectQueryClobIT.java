package org.apache.cayenne.query;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.lob.ClobTestEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(ServerCase.LOB_PROJECT)
public class SelectQueryClobIT extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("CLOB_TEST_RELATION");

        if (accessStackAdapter.supportsLobs()) {
            dbHelper.deleteAll("CLOB_TEST");
        }
    }

    protected void createClobDataSet() throws Exception {
        TableHelper tClobTest = new TableHelper(dbHelper, "CLOB_TEST");
        tClobTest.setColumns("CLOB_TEST_ID", "CLOB_COL");

        tClobTest.deleteAll();

        tClobTest.insert(1, "clob1");
        tClobTest.insert(2, "clob2");
    }

    /**
     * Test how "like ignore case" works when using uppercase parameter.
     */
    @Test
    public void testSelectLikeIgnoreCaseClob() throws Exception {
        if (accessStackAdapter.supportsLobs()) {
            createClobDataSet();
            SelectQuery<ClobTestEntity> query = new SelectQuery<ClobTestEntity>(ClobTestEntity.class);
            Expression qual = ExpressionFactory.likeIgnoreCaseExp("clobCol", "clob%");
            query.setQualifier(qual);
            List<?> objects = context.performQuery(query);
            assertEquals(2, objects.size());
        }
    }

    @Test
    public void testSelectFetchLimit_Offset_DistinctClob() throws Exception {
        if (accessStackAdapter.supportsLobs()) {
            createClobDataSet();

            // see CAY-1539... CLOB column causes suppression of DISTINCT in
            // SQL, and hence the offset processing is done in memory
            SelectQuery<ClobTestEntity> query = new SelectQuery<ClobTestEntity>(ClobTestEntity.class);
            query.addOrdering("db:" + ClobTestEntity.CLOB_TEST_ID_PK_COLUMN, SortOrder.ASCENDING);
            query.setFetchLimit(1);
            query.setFetchOffset(1);
            query.setDistinct(true);

            List<ClobTestEntity> objects = context.performQuery(query);
            assertEquals(1, objects.size());
            assertEquals(2, Cayenne.intPKForObject(objects.get(0)));
        }
    }

    @Test
    public void testSelectEqualsClob() throws Exception {
        if (accessStackAdapter.supportsLobComparisons()) {
            createClobDataSet();
            SelectQuery<ClobTestEntity> query = new SelectQuery<ClobTestEntity>(ClobTestEntity.class);
            Expression qual = ExpressionFactory.matchExp("clobCol", "clob1");
            query.setQualifier(qual);
            List<?> objects = context.performQuery(query);
            assertEquals(1, objects.size());
        }
    }

    @Test
    public void testSelectNotEqualsClob() throws Exception {
        if (accessStackAdapter.supportsLobComparisons()) {
            createClobDataSet();
            SelectQuery query = new SelectQuery(ClobTestEntity.class);
            Expression qual = ExpressionFactory.noMatchExp("clobCol", "clob1");
            query.setQualifier(qual);
            List<?> objects = context.performQuery(query);
            assertEquals(1, objects.size());
        }
    }
}