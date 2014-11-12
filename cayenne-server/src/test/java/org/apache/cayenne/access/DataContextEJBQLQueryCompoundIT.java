package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CompoundFkTestEntity;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(ServerCase.COMPOUND_PROJECT)
public class DataContextEJBQLQueryCompoundIT extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private UnitDbAdapter accessStackAdapter;

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
    public void testSelectFromWhereMatchOnMultiColumnObject() throws Exception {
        createTwoCompoundPKTwoFK();

        Map<String, String> key1 = new HashMap<String, String>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity a = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                key1);

        String ejbql = "select e from CompoundFkTestEntity e WHERE e.toCompoundPk = :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", a);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        CompoundFkTestEntity o1 = (CompoundFkTestEntity) ps.get(0);
        assertEquals(33002, Cayenne.intPKForObject(o1));
    }

    @Test
    public void testSelectFromWhereMatchOnMultiColumnObjectReverse() throws Exception {
        if (!accessStackAdapter.supportsReverseComparison()) {
            return;
        }

        createTwoCompoundPKTwoFK();

        Map<String, String> key1 = new HashMap<String, String>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity a = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                key1);

        String ejbql = "select e from CompoundFkTestEntity e WHERE :param = e.toCompoundPk";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", a);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        CompoundFkTestEntity o1 = (CompoundFkTestEntity) ps.get(0);
        assertEquals(33002, Cayenne.intPKForObject(o1));
    }

    @Test
    public void testSelectFromWhereNoMatchOnMultiColumnObject() throws Exception {
        createTwoCompoundPKTwoFK();

        Map<String, String> key1 = new HashMap<String, String>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity a = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                key1);

        String ejbql = "select e from CompoundFkTestEntity e WHERE e.toCompoundPk <> :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", a);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        CompoundFkTestEntity o1 = (CompoundFkTestEntity) ps.get(0);
        assertEquals(33001, Cayenne.intPKForObject(o1));
    }

}
