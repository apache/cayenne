package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.meaningful_pk.ClientMeaningfulPk;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(ClientCase.MEANINGFUL_PK_PROJECT)
public class CayenneContextMeaningfulPKIT extends ClientCase {

    @Inject
    private CayenneContext clientContext;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tMeaningfulPK;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MEANINGFUL_PK");

        tMeaningfulPK = new TableHelper(dbHelper, "MEANINGFUL_PK");
        tMeaningfulPK.setColumns("PK");
    }

    private void deleteAndCreateTwoMeaningfulPKsDataSet() throws Exception {
        tMeaningfulPK.deleteAll();
        tMeaningfulPK.insert("A");
        tMeaningfulPK.insert("B");
    }

    @Test
    public void testMeaningfulPK() throws Exception {
        deleteAndCreateTwoMeaningfulPKsDataSet();

        SelectQuery query = new SelectQuery(ClientMeaningfulPk.class);
        query.addOrdering(ClientMeaningfulPk.PK_PROPERTY, SortOrder.DESCENDING);

        List<?> results = clientContext.performQuery(query);
        assertEquals(2, results.size());
    }

}
