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
package org.apache.cayenne.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.types.BooleanType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.IntegerType;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.map.DbAttribute;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class CompactSl4jJdbcEventLoggerTest {

    @Before
    public void before() {
        TestAppender.events.clear();
    }

    @Test
    public void logWithCompact_Union() {

        CompactSl4jJdbcEventLogger compactSl4jJdbcEventLogger = new CompactSl4jJdbcEventLogger(new DefaultRuntimeProperties(Collections.emptyMap()));
        DbAttributeBinding[] bindings = createBindings();
        final List<LoggingEvent> log = TestAppender.events;
        assertEquals(log.size(), 0);

        compactSl4jJdbcEventLogger.logQuery(
                "SELECT t0.NAME AS ec0_0, t0.F_KEY1 AS ec0_1, t0.F_KEY2 AS ec0_2," +
                        " t0.PKEY AS ec0_3 FROM COMPOUND_FK_TEST t0 INNER JOIN COMPOUND_PK_TEST " +
                        "t1 ON (t0.F_KEY1 = t1.KEY1 AND t0.F_KEY2 = t1.KEY2) WHERE t1.NAME LIKE ?", createBindings());
        assertEquals(log.size(), 1);
        LoggingEvent firstLogEntry = log.get(0);
        assertThat(firstLogEntry.getLevel(), is(Level.INFO));
        assertThat(firstLogEntry.getMessage(), is("SELECT (4 columns) FROM COMPOUND_FK_TEST t0 " +
                "INNER JOIN COMPOUND_PK_TEST t1 ON (t0.F_KEY1 = t1.KEY1 AND t0.F_KEY2 = t1.KEY2) " +
                "WHERE t1.NAME LIKE ? [bind: 1->t0.NAME: {'', 52, 'true'}, 2->t0.F_KEY1: 'true'] "));
        assertThat(firstLogEntry.getLoggerName(), is("org.apache.cayenne.log.JdbcEventLogger"));

        compactSl4jJdbcEventLogger.logQuery(
                "SELECT t0.NAME AS ec0_0, t0.F_KEY1 AS ec0_1, " +
                        "t0.PKEY AS ec0_3 FROM COMPOUND_FK_TEST t0 INNER JOIN COMPOUND_PK_TEST " +
                        "t1 ON (t0.F_KEY1 = t1.KEY1 AND t0.F_KEY2 = t1.KEY2) WHERE t1.NAME LIKE ?" +
                        "UNION ALL " +
                        "SELECT t0.NAME AS ec0_0, t0.F_KEY1 AS ec0_1," +
                        " t0.PKEY AS ec0_3 FROM COMPOUND_FK_TEST t0 INNER JOIN COMPOUND_PK_TEST " +
                        "t1 ON (t0.F_KEY1 = t1.KEY1 AND t0.F_KEY2 = t1.KEY2) WHERE t1.NAME LIKE ?" +
                        "union all " +
                        "SELECT t0.NAME AS ec0_0, t0.F_KEY1 AS ec0_1, t0.F_KEY2 AS ec0_2," +
                        " t0.PKEY AS ec0_3 FROM COMPOUND_FK_TEST t0 INNER JOIN COMPOUND_PK_TEST " +
                        "t1 ON (t0.F_KEY1 = t1.KEY1 AND t0.F_KEY2 = t1.KEY2) WHERE t1.NAME LIKE ?", bindings);
        assertEquals(log.size(), 2);
         firstLogEntry = log.get(1);
        assertThat(firstLogEntry.getLevel(), is(Level.INFO));
        assertThat(firstLogEntry.getMessage(), is("SELECT t0.NAME AS ec0_0, t0.F_KEY1 AS ec0_1, t0.PKEY AS ec0_3 FROM COMPOUND_FK_TEST t0 " +
                "INNER JOIN COMPOUND_PK_TEST t1 ON (t0.F_KEY1 = t1.KEY1 AND t0.F_KEY2 = t1.KEY2) " +
                "WHERE t1.NAME LIKE ? UNION ALL SELECT t0.NAME AS ec0_0, t0.F_KEY1 AS ec0_1, t0.PKEY AS ec0_3 " +
                "FROM COMPOUND_FK_TEST t0 INNER JOIN COMPOUND_PK_TEST t1 ON (t0.F_KEY1 = t1.KEY1 AND t0.F_KEY2 = t1.KEY2) " +
                "WHERE t1.NAME LIKE ? UNION all SELECT (4 columns) FROM COMPOUND_FK_TEST t0 INNER JOIN COMPOUND_PK_TEST t1 ON (t0.F_KEY1 = t1.KEY1 AND t0.F_KEY2 = t1.KEY2) " +
                "WHERE t1.NAME LIKE ? [bind: 1->t0.NAME: {'', 52, 'true'}, 2->t0.F_KEY1: 'true'] "));
        assertThat(firstLogEntry.getLoggerName(), is("org.apache.cayenne.log.JdbcEventLogger"));

    }

    private DbAttributeBinding [] createBindings() {
        return new DbAttributeBinding[] { createBinding("t0.NAME", 1, "", new CharType(false, false)),
                                            createBinding("t0.NAME", 2, 52, new IntegerType()),
                                            createBinding("t0.NAME", 3, true, new BooleanType()),
                                            createBinding("t0.F_KEY1", 4, true, new BooleanType())};
    }

    private DbAttributeBinding createBinding(String name, int position, Object object, ExtendedType type){

        DbAttributeBinding dbAttributeBinding = new DbAttributeBinding(new DbAttribute(name));
        dbAttributeBinding.setValue(object);
        dbAttributeBinding.setStatementPosition(position);
        if (type != null) {
            dbAttributeBinding.setExtendedType(type);
        }

        return dbAttributeBinding;
    }
}
