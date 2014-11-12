package org.apache.cayenne.access;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.date_time.DateTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(ServerCase.DATE_TIME_PROJECT)
public class DataContextEJBQLDateTimeFunctionalExpressionsIT extends ServerCase {

    @Inject
    protected DBHelper dbHelper;

    @Inject
    private ObjectContext context;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("DATE_TEST");
    }

    @Test
    public void testCURRENT_DATE() {

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);

        DateTestEntity o1 = context.newObject(DateTestEntity.class);
        cal.set(year - 3, 1, 1);
        o1.setDateColumn(cal.getTime());

        DateTestEntity o2 = context.newObject(DateTestEntity.class);
        cal.set(year + 3, 1, 1);
        o2.setDateColumn(cal.getTime());

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM DateTestEntity d WHERE d.dateColumn > CURRENT_DATE");
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o2));
    }

    @Test
    public void testCURRENT_TIME() {

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);

        DateTestEntity o1 = context.newObject(DateTestEntity.class);
        cal.set(year, 1, 1, 0, 0, 0);
        o1.setTimeColumn(cal.getTime());

        DateTestEntity o2 = context.newObject(DateTestEntity.class);
        cal.set(year, 1, 1, 23, 59, 59);
        o2.setTimeColumn(cal.getTime());

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM DateTestEntity d WHERE d.timeColumn < CURRENT_TIME");
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o1));
    }

    @Test
    public void testCURRENT_TIMESTAMP() {

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int date = cal.get(Calendar.DATE);

        DateTestEntity o1 = context.newObject(DateTestEntity.class);
        cal.set(year, month, date, 0, 0, 0);
        o1.setTimestampColumn(cal.getTime());

        DateTestEntity o2 = context.newObject(DateTestEntity.class);
        cal.set(year, month, date, 23, 59, 59);
        o2.setTimestampColumn(cal.getTime());

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM DateTestEntity d WHERE d.timestampColumn < CURRENT_TIMESTAMP");
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o1));
    }
}
