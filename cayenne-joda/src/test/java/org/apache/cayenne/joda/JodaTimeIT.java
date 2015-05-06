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

package org.apache.cayenne.joda;

import org.apache.cayenne.CayenneJodaModule;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.joda.db.DateTimeTestEntity;
import org.apache.cayenne.joda.db.LocalDateTestEntity;
import org.apache.cayenne.joda.db.LocalDateTimeTestEntity;
import org.apache.cayenne.joda.db.LocalTimeTestEntity;
import org.apache.cayenne.query.SelectQuery;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JodaTimeIT {

    private ServerRuntime runtime;

    @Before
    public void setUp() throws Exception {
        Module jodaModule = new CayenneJodaModule();
        this.runtime = new ServerRuntime("cayenne-joda.xml", jodaModule);
    }

    @Test
    public void testJodaDateTime() throws SQLException {
        ObjectContext context = runtime.newContext();

        DateTimeTestEntity dateTimeTestEntity = context.newObject(DateTimeTestEntity.class);
        DateTime dateTime = DateTime.now();
        dateTimeTestEntity.setTimestamp(dateTime);

        context.commitChanges();

        SelectQuery q = new SelectQuery(DateTimeTestEntity.class);
        DateTimeTestEntity testRead = (DateTimeTestEntity) context.performQuery(q).get(0);

        DateTime timestamp = testRead.getTimestamp();
        assertNotNull(timestamp);
        assertEquals(DateTime.class, timestamp.getClass());
        assertEquals(dateTime, timestamp);
    }

    @Test
    public void testJodaLocalDate() {
        ObjectContext context = runtime.newContext();

        LocalDateTestEntity localDateTestEntity = context.newObject(LocalDateTestEntity.class);
        LocalDate localDate = LocalDate.now();
        localDateTestEntity.setDate(localDate);

        context.commitChanges();

        SelectQuery q = new SelectQuery(LocalDateTestEntity.class);
        LocalDateTestEntity testRead = (LocalDateTestEntity) context.performQuery(q).get(0);

        LocalDate date = testRead.getDate();
        assertNotNull(date);
        assertEquals(LocalDate.class, date.getClass());
        assertEquals(localDate, date);
    }

    @Test
    public void testJodaLocalTime() {
        ObjectContext context = runtime.newContext();

        LocalTimeTestEntity localTimeTestEntity = context.newObject(LocalTimeTestEntity.class);
        LocalTime localTime = LocalTime.now();
        localTimeTestEntity.setTime(localTime);

        context.commitChanges();

        SelectQuery q = new SelectQuery(LocalTimeTestEntity.class);
        LocalTimeTestEntity testRead = (LocalTimeTestEntity) context.performQuery(q).get(0);

        LocalTime time = testRead.getTime();
        assertNotNull(time);
        assertEquals(LocalTime.class, time.getClass());
        assertEquals(localTime.getSecondOfMinute(), time.getSecondOfMinute());
        assertEquals(localTime.getMinuteOfHour(), time.getMinuteOfHour());
        assertEquals(localTime.getHourOfDay(), time.getHourOfDay());
    }

    @Test
    public void testJodaLocalDateTime() {
        ObjectContext context = runtime.newContext();

        LocalDateTimeTestEntity localDateTimeTestEntity = context.newObject(LocalDateTimeTestEntity.class);
        LocalDateTime localDateTime = LocalDateTime.now();
        localDateTimeTestEntity.setTimestamp(localDateTime);

        context.commitChanges();

        SelectQuery q = new SelectQuery(LocalDateTimeTestEntity.class);
        LocalDateTimeTestEntity testRead = (LocalDateTimeTestEntity) context.performQuery(q).get(0);

        LocalDateTime timestamp = testRead.getTimestamp();
        assertNotNull(timestamp);
        assertEquals(LocalDateTime.class, timestamp.getClass());
        assertEquals(localDateTime, timestamp);
    }

}
