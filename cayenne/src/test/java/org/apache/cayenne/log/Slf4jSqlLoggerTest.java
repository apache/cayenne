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

    private static PSParameter<?> ps(String name, Object value) {
        return new PSParameter<>(value, 1, Types.INTEGER, 0, null, new DbAttribute(name));
    }

    @Test
    public void selectLineWithSingleBinding() {
        TranslatedSelect select = new TranslatedSelect(
                "SELECT t0.id FROM my_table t0 WHERE t0.user_id = ?",
                new PSParameter<?>[]{ps("user_id", 15)},
                new RSColumn[0], false, false);

        assertEquals("SELECT t0.id FROM my_table t0 WHERE t0.user_id = ? [bind:[user_id:15]] [selected:1]",
                logger.buildStatementLine(select, "selected:", 1));
    }

    @Test
    public void selectLineWithoutBindings() {
        TranslatedSelect select = new TranslatedSelect(
                "SELECT t0.id FROM my_table t0", new PSParameter<?>[0], new RSColumn[0], false, false);

        assertEquals("SELECT t0.id FROM my_table t0 [selected:0]",
                logger.buildStatementLine(select, "selected:", 0));
    }

    @Test
    public void batchLineTruncatesToFirstAndLastRow() {
        // 5 rows, 2 placeholders (id, name); with threshold 3 the middle 3 rows are elided
        PSBatchParameter id = new PSBatchParameter(
                new Object[]{3, 1, 5, 4, 2}, 1, Types.INTEGER, 0, new DbAttribute("id"));
        PSBatchParameter name = new PSBatchParameter(
                new Object[]{"n3", "n1", "n5", "n4", "n2"}, 2, Types.VARCHAR, 0, new DbAttribute("name"));

        TranslatedBatch batch = new TranslatedBatch(
                "INSERT INTO table1(id, name) VALUES(?, ?)", new PSBatchParameter[]{id, name});

        assertEquals("INSERT INTO table1(id, name) VALUES(?, ?) [bind:[id:3,name:'n3']..3..[id:2,name:'n2']] [updated:5]",
                logger.buildStatementLine(batch, "updated:", 5));
    }

    @Test
    public void singleRowBatchHasNoDoubleBrackets() {
        PSBatchParameter id = new PSBatchParameter(
                new Object[]{200}, 1, Types.INTEGER, 0, new DbAttribute("ARTIST_ID"));
        PSBatchParameter name = new PSBatchParameter(
                new Object[]{"Test"}, 2, Types.VARCHAR, 0, new DbAttribute("ARTIST_NAME"));

        TranslatedBatch batch = new TranslatedBatch(
                "INSERT INTO ARTIST(ARTIST_ID, ARTIST_NAME) VALUES(?, ?)", new PSBatchParameter[]{id, name});

        assertEquals("INSERT INTO ARTIST(ARTIST_ID, ARTIST_NAME) VALUES(?, ?) [bind:[ARTIST_ID:200,ARTIST_NAME:'Test']] [updated:1]",
                logger.buildStatementLine(batch, "updated:", 1));
    }

    @Test
    public void batchLineShowsAllRowsBelowThreshold() {
        PSBatchParameter id = new PSBatchParameter(
                new Object[]{3, 2}, 1, Types.INTEGER, 0, new DbAttribute("id"));

        TranslatedBatch batch = new TranslatedBatch(
                "INSERT INTO table1(id) VALUES(?)", new PSBatchParameter[]{id});

        assertEquals("INSERT INTO table1(id) VALUES(?) [bind:[id:3][id:2]] [updated:2]",
                logger.buildStatementLine(batch, "updated:", 2));
    }
}
