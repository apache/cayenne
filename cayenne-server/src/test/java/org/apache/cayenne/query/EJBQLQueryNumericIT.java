package org.apache.cayenne.query;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.numeric_types.BigIntegerEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(ServerCase.NUMERIC_TYPES_PROJECT)
public class EJBQLQueryNumericIT extends ServerCase {

    @Inject
    protected DBHelper dbHelper;

    @Inject
    private ObjectContext context;

    private TableHelper tBigIntegerEntity;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("BIGINTEGER_ENTITY");

        tBigIntegerEntity = new TableHelper(dbHelper, "BIGINTEGER_ENTITY");
        tBigIntegerEntity.setColumns("ID", "BIG_INTEGER_FIELD");
    }

    protected void createBigIntegerEntitiesDataSet() throws Exception {
        tBigIntegerEntity.insert(44001, new Long(744073709551715l));
    }

    @Test
    public void testLongParameter() throws Exception {
        createBigIntegerEntitiesDataSet();
        String ejbql = "SELECT bie FROM BigIntegerEntity bie WHERE bie.bigIntegerField > ?1";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter(1,744073709551615l);
        List<BigIntegerEntity> result = context.performQuery(query);
        assertEquals(1, result.size());
    }

    @Test
    public void testLongLiteral() throws Exception {
        createBigIntegerEntitiesDataSet();
        String ejbql = "SELECT bie FROM BigIntegerEntity bie WHERE bie.bigIntegerField > 744073709551615";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<BigIntegerEntity> result = context.performQuery(query);
        assertEquals(1, result.size());
    }
}
