package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CompoundFkTestEntity;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(ServerCase.COMPOUND_PROJECT)
public class DataContextSQLTemplateCompoundIT extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tCompoundPkTest;
    protected TableHelper tCompoundFkTest;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("COMPOUND_FK_TEST");
        dbHelper.deleteAll("COMPOUND_PK_TEST");

        tCompoundPkTest = new TableHelper(dbHelper, "COMPOUND_PK_TEST");
        tCompoundPkTest.setColumns("KEY1", "KEY2");

        tCompoundFkTest = new TableHelper(dbHelper, "COMPOUND_FK_TEST");
        tCompoundFkTest.setColumns("PKEY", "F_KEY1", "F_KEY2");
    }

    protected void createTwoCompoundPKsAndCompoundFKsDataSet() throws Exception {
        tCompoundPkTest.insert("a1", "a2");
        tCompoundPkTest.insert("b1", "b2");

        tCompoundFkTest.insert(6, "a1", "a2");
        tCompoundFkTest.insert(7, "b1", "b2");
    }

    @Test
    public void testBindObjectEqualCompound() throws Exception {
        createTwoCompoundPKsAndCompoundFKsDataSet();

        Map<String, String> pk = new HashMap<String, String>();
        pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "a1");
        pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "a2");

        CompoundPkTestEntity a = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                pk);

        String template = "SELECT * FROM COMPOUND_FK_TEST t0"
                + " WHERE #bindObjectEqual($a [ 't0.F_KEY1', 't0.F_KEY2' ] [ 'KEY1', 'KEY2' ] ) ORDER BY PKEY";
        SQLTemplate query = new SQLTemplate(CompoundFkTestEntity.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List<CompoundFkTestEntity> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        CompoundFkTestEntity p = objects.get(0);
        assertEquals(6, Cayenne.intPKForObject(p));
    }

    @Test
    public void testBindObjectNotEqualCompound() throws Exception {
        createTwoCompoundPKsAndCompoundFKsDataSet();

        Map<String, String> pk = new HashMap<String, String>();
        pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "a1");
        pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "a2");

        CompoundPkTestEntity a = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                pk);

        String template = "SELECT * FROM COMPOUND_FK_TEST t0"
                + " WHERE #bindObjectNotEqual($a [ 't0.F_KEY1', 't0.F_KEY2' ] [ 'KEY1', 'KEY2' ] ) ORDER BY PKEY";
        SQLTemplate query = new SQLTemplate(CompoundFkTestEntity.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List<CompoundFkTestEntity> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        CompoundFkTestEntity p = objects.get(0);
        assertEquals(7, Cayenne.intPKForObject(p));
    }
}
