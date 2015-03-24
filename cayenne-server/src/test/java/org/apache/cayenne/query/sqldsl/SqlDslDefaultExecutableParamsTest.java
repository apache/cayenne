package org.apache.cayenne.query.sqldsl; /*****************************************************************
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

import de.jexp.jequel.expression.IColumn;
import de.jexp.jequel.expression.Table;
import de.jexp.jequel.expression.types.NUMERIC;
import de.jexp.jequel.sql.Sql;
import junit.framework.TestCase;

import java.sql.Date;

import static de.jexp.jequel.sql.Expressions.param;

public class SqlDslDefaultExecutableParamsTest extends TestCase {

    public static final ARTIST ARTIST = new ARTIST();
    public static class ARTIST extends Table<ARTIST> {
        public NUMERIC ARTIST_ID = integer().primaryKey();
        public IColumn<String> ARTIST_NAME = character(254).mandatory();
        public IColumn<Date> DATE_OF_BIRTH = date();

        {
            initFields();
        }
    }

    public void testExtractParams() throws Exception {
        Sql sql = Sql.Select(ARTIST).where(ARTIST.ARTIST_ID.eq(param(201L))).toSql();
        SqlDslDefaultExecutableParams params = SqlDslDefaultExecutableParams.extractParams(sql);

        assertEquals(1, params.getParamCount());
    }
}