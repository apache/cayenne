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

package org.apache.cayenne.log;

import org.apache.cayenne.access.jdbc.PSBatchParameter;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.translator.TranslatedBatch;
import org.apache.cayenne.access.translator.TranslatedSelect;
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.map.DbAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Slf4jSqlLoggerTest {

    private Slf4jSqlLogger logger;

    @BeforeEach
    public void setUp() {
        RuntimeProperties props = mock(RuntimeProperties.class);
        when(props.getInt(eq(Constants.JDBC_LOG_BATCH_ROW_THRESHOLD_PROPERTY), anyInt())).thenReturn(3);
        logger = new Slf4jSqlLogger(props);
    }

    private static Slf4jSqlLogger loggerWithThreshold(int threshold) {
        RuntimeProperties props = mock(RuntimeProperties.class);
        when(props.getInt(eq(Constants.JDBC_LOG_BATCH_ROW_THRESHOLD_PROPERTY), anyInt())).thenReturn(threshold);
        return new Slf4jSqlLogger(props);
    }

    private static String line(Slf4jSqlLogger logger, TranslatedStatement statement, String label, int count) {
        StringBuilder buffer = new StringBuilder();
        logger.appendStatementLine(buffer, statement, label, count);
        return buffer.toString();
    }

    private static String errorLine(Slf4jSqlLogger logger, TranslatedStatement statement, Throwable error, long durationMillis) {
        StringBuilder buffer = new StringBuilder();
        logger.appendErrorLine(buffer, statement, error, durationMillis);
        return buffer.toString();
    }

    // a single-placeholder batch whose id values are 0..rows-1, so each logged row is identifiable by its value
    private static TranslatedBatch idBatch(int rows) {
        Object[] values = new Object[rows];
        for (int i = 0; i < rows; i++) {
            values[i] = i;
        }
        PSBatchParameter id = new PSBatchParameter(values, 1, Types.INTEGER, 0, new DbAttribute("id"));
        return new TranslatedBatch("INSERT INTO t(id) VALUES(?)", new PSBatchParameter[]{id});
    }

    @Test
    public void selectLineWithSingleBinding() {
        TranslatedSelect select = new TranslatedSelect(
                "SELECT t0.id FROM my_table t0 WHERE t0.user_id = ?",
                new PSParameter<?>[]{new PSParameter<>(15, 1, Types.INTEGER, 0, null, new DbAttribute("user_id"))},
                new RSColumn[0], false, false);

        assertEquals(
                "SELECT t0.id FROM my_table t0 WHERE t0.user_id = ? | bind:[user_id:15] selected:1",
                line(logger, select, "selected:", 1));
    }

    @Test
    public void errorLineCarriesBindingsMessageAndDuration() {
        TranslatedSelect select = new TranslatedSelect(
                "SELECT t0.id FROM my_table t0 WHERE t0.user_id = ?",
                new PSParameter<?>[]{new PSParameter<>(15, 1, Types.INTEGER, 0, null, new DbAttribute("user_id"))},
                new RSColumn[0], false, false);

        assertEquals(
                "SELECT t0.id FROM my_table t0 WHERE t0.user_id = ? | bind:[user_id:15] time_ms:1000 error: bad column",
                errorLine(logger, select, new RuntimeException("bad column"), 1000));
    }

    @Test
    public void selectLineWithoutBindings() {
        TranslatedSelect select = new TranslatedSelect(
                "SELECT t0.id FROM my_table t0", new PSParameter<?>[0], new RSColumn[0], false, false);

        assertEquals(
                "SELECT t0.id FROM my_table t0 | selected:0",
                line(logger, select, "selected:", 0));
    }

    @Test
    public void batchLineSplitsHeadAndTailAroundElision() {
        // 5 rows, 2 placeholders (id, name); odd threshold 3 -> head 2, tail 1, middle 2 rows elided
        PSBatchParameter id = new PSBatchParameter(
                new Object[]{3, 1, 5, 4, 2}, 1, Types.INTEGER, 0, new DbAttribute("id"));
        PSBatchParameter name = new PSBatchParameter(
                new Object[]{"n3", "n1", "n5", "n4", "n2"}, 2, Types.VARCHAR, 0, new DbAttribute("name"));

        TranslatedBatch batch = new TranslatedBatch(
                "INSERT INTO table1(id, name) VALUES(?, ?)", new PSBatchParameter[]{id, name});

        assertEquals(
                "INSERT INTO table1(id, name) VALUES(?, ?) | bind:[id:3,name:'n3'][id:1,name:'n1']..2..[id:2,name:'n2'] updated:5",
                line(logger, batch, "updated:", 5));
    }

    @Test
    public void singleRowBatchHasNoDoubleBrackets() {
        PSBatchParameter id = new PSBatchParameter(
                new Object[]{200}, 1, Types.INTEGER, 0, new DbAttribute("ARTIST_ID"));
        PSBatchParameter name = new PSBatchParameter(
                new Object[]{"Test"}, 2, Types.VARCHAR, 0, new DbAttribute("ARTIST_NAME"));

        TranslatedBatch batch = new TranslatedBatch(
                "INSERT INTO ARTIST(ARTIST_ID, ARTIST_NAME) VALUES(?, ?)", new PSBatchParameter[]{id, name});

        assertEquals(
                "INSERT INTO ARTIST(ARTIST_ID, ARTIST_NAME) VALUES(?, ?) | bind:[ARTIST_ID:200,ARTIST_NAME:'Test'] updated:1",
                line(logger, batch, "updated:", 1));
    }

    @Test
    public void batchLineShowsAllRowsBelowThreshold() {
        PSBatchParameter id = new PSBatchParameter(
                new Object[]{3, 2}, 1, Types.INTEGER, 0, new DbAttribute("id"));

        TranslatedBatch batch = new TranslatedBatch("INSERT INTO table1(id) VALUES(?)", new PSBatchParameter[]{id});
        assertEquals("INSERT INTO table1(id) VALUES(?) | bind:[id:3][id:2] updated:2",
                line(logger, batch, "updated:", 2));
    }

    @Test
    public void batchLineEvenThresholdSplitsEvenly() {
        // even threshold 4 -> head 2, tail 2; 10 rows -> middle 6 elided
        assertEquals("INSERT INTO t(id) VALUES(?) | bind:[id:0][id:1]..6..[id:8][id:9] updated:10",
                line(loggerWithThreshold(4), idBatch(10), "updated:", 10));
    }

    @Test
    public void batchLineShowsAllRowsAtThreshold() {
        // exactly threshold rows -> nothing elided
        assertEquals("INSERT INTO t(id) VALUES(?) | bind:[id:0][id:1][id:2][id:3] updated:4",
                line(loggerWithThreshold(4), idBatch(4), "updated:", 4));
    }

    @Test
    public void batchLineElidesExactlyOneAboveThreshold() {
        // one row above threshold 4 -> head 2, tail 2, a single row elided
        assertEquals("INSERT INTO t(id) VALUES(?) | bind:[id:0][id:1]..1..[id:3][id:4] updated:5",
                line(loggerWithThreshold(4), idBatch(5), "updated:", 5));
    }

    @Test
    public void batchLineThresholdOfOneOrTwoClampsToTwo() {
        // threshold 1 and 2 both clamp to 2 -> head 1, tail 1
        assertEquals("INSERT INTO t(id) VALUES(?) | bind:[id:0]..3..[id:4] updated:5",
                line(loggerWithThreshold(1), idBatch(5), "updated:", 5));
        assertEquals("INSERT INTO t(id) VALUES(?) | bind:[id:0]..3..[id:4] updated:5",
                line(loggerWithThreshold(2), idBatch(5), "updated:", 5));
    }

    @Test
    public void batchLineNonPositiveThresholdDisablesTruncation() {
        // threshold 0 or negative -> no truncation, every row logged
        assertEquals("INSERT INTO t(id) VALUES(?) | bind:[id:0][id:1][id:2][id:3][id:4] updated:5",
                line(loggerWithThreshold(0), idBatch(5), "updated:", 5));
        assertEquals("INSERT INTO t(id) VALUES(?) | bind:[id:0][id:1][id:2][id:3][id:4] updated:5",
                line(loggerWithThreshold(-1), idBatch(5), "updated:", 5));
    }
}
