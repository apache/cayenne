/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.types;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.query.MappedSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.legacy_datetime.CalendarEntity;
import org.apache.cayenne.testdo.legacy_datetime.DateTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Date handling in Cayenne.
 */
public class LegacyDateTimeTypesIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.LEGACY_DATE_TIME_PROJECT);

    @Test
    public void calendar() {

        CalendarEntity test = env.context().newObject(CalendarEntity.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2002, Calendar.FEBRUARY, 1);

        test.setCalendarField(cal);
        env.context().commitChanges();

        CalendarEntity testRead = ObjectSelect.query(CalendarEntity.class)
                .selectFirst(env.context());
        assertNotNull(testRead.getCalendarField());
        assertEquals(cal, testRead.getCalendarField());

        test.setCalendarField(null);
        env.context().commitChanges();
    }

    @Test
    public void date() {
        DateTestEntity test = env.context().newObject(DateTestEntity.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2002, Calendar.FEBRUARY, 1);
        Date nowDate = cal.getTime();
        test.setDateColumn(nowDate);
        env.context().commitChanges();

        DateTestEntity testRead = ObjectSelect.query(DateTestEntity.class)
                .selectFirst(env.context());
        assertNotNull(testRead.getDateColumn());
        assertEquals(nowDate, testRead.getDateColumn());
        assertEquals(Date.class, testRead.getDateColumn().getClass());
    }

    @Test
    public void time() {
        DateTestEntity test = env.context().newObject(DateTestEntity.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1970, Calendar.JANUARY, 1, 1, 20, 30);
        Date nowTime = cal.getTime();
        test.setTimeColumn(nowTime);
        env.context().commitChanges();

        DateTestEntity testRead = ObjectSelect.query(DateTestEntity.class)
                .selectFirst(env.context());
        assertNotNull(testRead.getTimeColumn());
        assertEquals(Date.class, testRead.getTimeColumn().getClass());

        // OpenBase fails to store seconds for the time
        // FrontBase returns time with 1 hour offset (I guess "TIME WITH TIMEZONE" may
        // need to be used as a default FB type?)
        // so this test is approximate...

        long delta = nowTime.getTime() - testRead.getTimeColumn().getTime();
        assertTrue(Math.abs(delta) <= 1000 * 60 * 60, "" + delta);
    }

    @Test
    public void timestamp() {
        DateTestEntity test = env.context().newObject(DateTestEntity.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, Calendar.FEBRUARY, 1, 1, 20, 30);

        // most databases fail millisecond accuracy
        // cal.set(Calendar.MILLISECOND, 55);

        Date now = cal.getTime();
        test.setTimestampColumn(now);
        env.context().commitChanges();

        DateTestEntity testRead = ObjectSelect.query(DateTestEntity.class)
                .selectFirst(env.context());
        assertNotNull(testRead.getTimestampColumn());
        assertEquals(now, testRead.getTimestampColumn());
    }

    @Test
    public void sQLTemplateTimestamp() {
        DateTestEntity test = env.context().newObject(DateTestEntity.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, Calendar.FEBRUARY, 1, 1, 20, 30);

        // most databases fail millisecond accuracy
        // cal.set(Calendar.MILLISECOND, 55);

        Date now = cal.getTime();
        test.setTimestampColumn(now);
        env.context().commitChanges();

        DataRow testRead = (DataRow) env.context().performQuery(MappedSelect.query("SelectDateTest")).get(0);
        Date columnValue = (Date) testRead.get("TIMESTAMP_COLUMN");
        assertNotNull(columnValue);
        assertEquals(now, columnValue);
    }

    @Test
    public void sQLTemplateDate() {
        DateTestEntity test = (DateTestEntity) env.context().newObject("DateTestEntity");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, Calendar.FEBRUARY, 1, 1, 20, 30);

        // most databases fail millisecond accuracy
        // cal.set(Calendar.MILLISECOND, 55);

        java.sql.Date now = new java.sql.Date(cal.getTime().getTime());
        test.setDateColumn(now);
        env.context().commitChanges();

        DataRow testRead = (DataRow) env.context().performQuery(MappedSelect.query("SelectDateTest")).get(0);
        Date columnValue = (Date) testRead.get("DATE_COLUMN");
        assertNotNull(columnValue);
        assertEquals(now.toString(), new java.sql.Date(columnValue.getTime()).toString());
    }

    @Test
    public void sQLTemplateTime() {
        DateTestEntity test = (DateTestEntity) env.context().newObject("DateTestEntity");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, Calendar.FEBRUARY, 1, 1, 20, 30);

        // most databases fail millisecond accuracy
        // cal.set(Calendar.MILLISECOND, 55);

        Time now = new Time(cal.getTime().getTime());
        test.setTimeColumn(now);
        env.context().commitChanges();

        DataRow testRead = (DataRow) env.context().performQuery(MappedSelect.query("SelectDateTest")).get(0);
        Date columnValue = (Date) testRead.get("TIME_COLUMN");
        assertNotNull(columnValue, testRead.toString());
        assertNotNull(columnValue);
        assertEquals(now.toString(), new Time(columnValue.getTime()).toString());
    }
}
