package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@UseServerRuntime(ServerCase.COMPOUND_PROJECT)
public class DataContextEJBQLUpdateCompoundIT extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tCompoundPk;
    private TableHelper tCompoundFk;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("COMPOUND_FK_TEST");
        dbHelper.deleteAll("COMPOUND_PK_TEST");

        tCompoundPk = new TableHelper(dbHelper, "COMPOUND_PK_TEST");
        tCompoundPk.setColumns("KEY1", "KEY2");

        tCompoundFk = new TableHelper(dbHelper, "COMPOUND_FK_TEST");
        tCompoundFk.setColumns("PKEY", "F_KEY1", "F_KEY2");
    }

    private void createTwoCompoundPKTwoFK() throws Exception {
        tCompoundPk.insert("a1", "a2");
        tCompoundPk.insert("b1", "b2");
        tCompoundFk.insert(33001, "a1", "a2");
        tCompoundFk.insert(33002, "b1", "b2");
    }

    @Test
    public void testUpdateNoQualifierToOneCompoundPK() throws Exception {
        createTwoCompoundPKTwoFK();

        Map<String, String> key1 = new HashMap<String, String>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity object = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                key1);

        EJBQLQuery check = new EJBQLQuery(
                "select count(e) from CompoundFkTestEntity e WHERE e.toCompoundPk <> :param");
        check.setParameter("param", object);

        Object notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(new Long(1l), notUpdated);

        String ejbql = "UPDATE CompoundFkTestEntity e SET e.toCompoundPk = :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", object);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(new Long(0l), notUpdated);
    }

}
