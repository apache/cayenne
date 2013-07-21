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

import java.sql.Time;
import java.util.Calendar;
import java.util.Date;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.CalendarEntity;
import org.apache.cayenne.testdo.testmap.DateTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Tests Date handling in Cayenne.
 */
@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DateTimeTypesTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("CALENDAR_TEST");
        dbHelper.deleteAll("DATE_TEST");
    }

    public void testCalendar() throws Exception {

        CalendarEntity test = context.newObject(CalendarEntity.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2002, 1, 1);

        test.setCalendarField(cal);
        context.commitChanges();

        SelectQuery q = new SelectQuery(CalendarEntity.class);
        CalendarEntity testRead = (CalendarEntity) context.performQuery(q).get(0);
        assertNotNull(testRead.getCalendarField());
        assertEquals(cal, testRead.getCalendarField());

        test.setCalendarField(null);
        context.commitChanges();
    }

    public void testDate() throws Exception {
        DateTestEntity test = context.newObject(DateTestEntity.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2002, 1, 1);
        Date nowDate = cal.getTime();
        test.setDateColumn(nowDate);
        context.commitChanges();

        SelectQuery q = new SelectQuery(DateTestEntity.class);
        DateTestEntity testRead = (DateTestEntity) context.performQuery(q).get(0);
        assertNotNull(testRead.getDateColumn());
        assertEquals(nowDate, testRead.getDateColumn());
        assertEquals(Date.class, testRead.getDateColumn().getClass());
    }

    public void testTime() throws Exception {
        DateTestEntity test = context.newObject(DateTestEntity.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1970, 0, 1, 1, 20, 30);
        Date nowTime = cal.getTime();
        test.setTimeColumn(nowTime);
        context.commitChanges();

        SelectQuery q = new SelectQuery(DateTestEntity.class);
        DateTestEntity testRead = (DateTestEntity) context.performQuery(q).get(0);
        assertNotNull(testRead.getTimeColumn());
        assertEquals(Date.class, testRead.getTimeColumn().getClass());

        // OpenBase fails to store seconds for the time
        // FrontBase returns time with 1 hour offset (I guess "TIME WITH TIMEZONE" may
        // need to be used as a default FB type?)
        // so this test is approximate...

        long delta = nowTime.getTime() - testRead.getTimeColumn().getTime();
        assertTrue("" + delta, Math.abs(delta) <= 1000 * 60 * 60);
    }

    public void testTimestamp() throws Exception {
        DateTestEntity test = context.newObject(DateTestEntity.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, 1, 1, 1, 20, 30);

        // most databases fail millisecond accuracy
        // cal.set(Calendar.MILLISECOND, 55);

        Date now = cal.getTime();
        test.setTimestampColumn(now);
        context.commitChanges();

        SelectQuery q = new SelectQuery(DateTestEntity.class);
        DateTestEntity testRead = (DateTestEntity) context.performQuery(q).get(0);
        assertNotNull(testRead.getTimestampColumn());
        assertEquals(now, testRead.getTimestampColumn());
    }

    public void testSQLTemplateTimestamp() throws Exception {
        DateTestEntity test = context.newObject(DateTestEntity.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, 1, 1, 1, 20, 30);

        // most databases fail millisecond accuracy
        // cal.set(Calendar.MILLISECOND, 55);

        Date now = cal.getTime();
        test.setTimestampColumn(now);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectDateTest");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Date columnValue = (Date) testRead.get("TIMESTAMP_COLUMN");
        assertNotNull(columnValue);
        assertEquals(now, columnValue);
    }

    public void testSQLTemplateDate() throws Exception {
        DateTestEntity test = (DateTestEntity) context.newObject("DateTestEntity");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, 1, 1, 1, 20, 30);

        // most databases fail millisecond accuracy
        // cal.set(Calendar.MILLISECOND, 55);

        java.sql.Date now = new java.sql.Date(cal.getTime().getTime());
        test.setDateColumn(now);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectDateTest");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Date columnValue = (Date) testRead.get("DATE_COLUMN");
        assertNotNull(columnValue);
        assertEquals(now.toString(), new java.sql.Date(columnValue.getTime()).toString());
    }

    public void testSQLTemplateTime() throws Exception {
        DateTestEntity test = (DateTestEntity) context.newObject("DateTestEntity");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, 1, 1, 1, 20, 30);

        // most databases fail millisecond accuracy
        // cal.set(Calendar.MILLISECOND, 55);

        Time now = new Time(cal.getTime().getTime());
        test.setTimeColumn(now);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectDateTest");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Date columnValue = (Date) testRead.get("TIME_COLUMN");
        assertNotNull(columnValue);
        assertEquals(now.toString(), new Time(columnValue.getTime()).toString());
    }
}
