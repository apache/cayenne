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

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.query.SQLExec;
import org.apache.cayenne.query.SQLSelect;
import org.apache.cayenne.testdo.datetime.LocalDateTestEntity;
import org.apache.cayenne.testdo.datetime.LocalDateTimeTestEntity;
import org.apache.cayenne.testdo.datetime.LocalTimeTestEntity;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Binds java.time values as untyped {@code #bind($x)} parameters in raw SQL. With no explicit JDBC type the
 * directive guesses one from the Java class of the value, so these tests cover the java.time entries in
 * {@link org.apache.cayenne.dba.TypesMapping}.
 */
public class DateTimeTypesSQLTemplateIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.DATE_TIME_PROJECT);

    private DataContext context;

    @BeforeEach
    public void before() throws SQLException {
        context = env.context();
        env.table("LOCAL_DATE_TEST").deleteAll();
        env.table("LOCAL_DATETIME_TEST").deleteAll();
        env.table("LOCAL_TIME_TEST").deleteAll();
    }

    @Test
    public void localDate_UntypedBind_Select() {
        LocalDate localDate = LocalDate.of(2024, 3, 15);
        LocalDateTestEntity entity = context.newObject(LocalDateTestEntity.class);
        entity.setDate(localDate);
        context.commitChanges();

        int count = SQLSelect
                .scalarQuery("SELECT COUNT(*) FROM LOCAL_DATE_TEST WHERE DateField = #bind($d)", Integer.class)
                .param("d", localDate)
                .selectOne(context);
        assertEquals(1, count);

        count = SQLSelect
                .scalarQuery("SELECT COUNT(*) FROM LOCAL_DATE_TEST WHERE DateField = #bind($d)", Integer.class)
                .param("d", localDate.plusDays(1))
                .selectOne(context);
        assertEquals(0, count);
    }

    @Test
    public void localTime_UntypedBind_Select() {
        LocalTime localTime = LocalTime.of(13, 45, 30);
        LocalTimeTestEntity entity = context.newObject(LocalTimeTestEntity.class);
        entity.setTime(localTime);
        context.commitChanges();

        int count = SQLSelect
                .scalarQuery("SELECT COUNT(*) FROM LOCAL_TIME_TEST WHERE TimeField = #bind($t)", Integer.class)
                .param("t", localTime)
                .selectOne(context);
        assertEquals(1, count);
    }

    @Test
    public void localDateTime_UntypedBind_Select() {
        LocalDateTime localDateTime = LocalDateTime.of(2024, 3, 15, 13, 45, 30);
        LocalDateTimeTestEntity entity = context.newObject(LocalDateTimeTestEntity.class);
        entity.setTimestamp(localDateTime);
        context.commitChanges();

        int count = SQLSelect
                .scalarQuery("SELECT COUNT(*) FROM LOCAL_DATETIME_TEST WHERE TimestampField = #bind($ts)", Integer.class)
                .param("ts", localDateTime)
                .selectOne(context);
        assertEquals(1, count);

        count = SQLSelect
                .scalarQuery("SELECT COUNT(*) FROM LOCAL_DATETIME_TEST WHERE TimestampField < #bind($ts)", Integer.class)
                .param("ts", localDateTime.plus(1, ChronoUnit.HOURS))
                .selectOne(context);
        assertEquals(1, count);
    }

    @Test
    public void localDateTime_UntypedBind_Insert() {
        LocalDateTime localDateTime = LocalDateTime.of(2024, 3, 15, 13, 45, 30);

        SQLExec.query("INSERT INTO LOCAL_DATETIME_TEST (ID, TimestampField) VALUES (#bind($id), #bind($ts))")
                .params("id", 1)
                .params("ts", localDateTime)
                .execute(context);

        LocalDateTime read = SQLSelect
                .scalarQuery("SELECT TimestampField FROM LOCAL_DATETIME_TEST WHERE ID = 1", LocalDateTime.class)
                .selectOne(context);
        assertEquals(localDateTime, read);
    }
}
