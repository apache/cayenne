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

package org.apache.cayenne;

import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.translator.TranslatedSQL;
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLTemplate;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CayenneSqlExceptionTest {

    @Test
    public void messageIncludesSql() {
        Query query = new SQLTemplate<>("SELECT * FROM ARTIST");
        TranslatedStatement statement = new TranslatedSQL("SELECT * FROM ARTIST", null, null);
        Throwable cause = new RuntimeException("boom");

        StatementContext context = context(query, statement);
        CayenneSqlException e = new CayenneSqlException("Query exception.", context, cause);

        assertSame(context, e.getStatementContext());
        assertSame(query, e.getQuery());
        assertSame(statement, e.getStatement());
        assertSame(cause, e.getCause());
        assertTrue(e.getMessage().contains("Query exception."), e.getMessage());
        assertTrue(e.getMessage().contains("SELECT * FROM ARTIST"), e.getMessage());
    }

    @Test
    public void queryNotIncludedInMessage() {
        Query query = new SQLTemplate<>("SELECT * FROM ARTIST");
        TranslatedStatement statement = new TranslatedSQL("SELECT * FROM ARTIST", null, null);

        CayenneSqlException e = new CayenneSqlException("Query exception.", context(query, statement),
                new RuntimeException());

        assertSame(query, e.getQuery());
        assertFalse(e.getMessage().contains(query.toString()), e.getMessage());
    }

    @Test
    public void sqlWithPercentDoesNotBreakFormatting() {
        // SQL may contain '%' (e.g. LIKE patterns); it must not be interpreted as a format specifier
        TranslatedStatement statement = new TranslatedSQL("SELECT * FROM ARTIST WHERE NAME LIKE '%foo%'", null, null);

        Query query = new SQLTemplate<>("SELECT * FROM ARTIST WHERE NAME LIKE '%foo%'");
        CayenneSqlException e = new CayenneSqlException("Query exception.", context(query, statement),
                new RuntimeException());

        assertTrue(e.getMessage().contains("LIKE '%foo%'"), e.getMessage());
    }

    @Test
    public void untranslatedRunHasNoStatement() {
        Query query = new SQLTemplate<>("SELECT * FROM ARTIST");
        StatementContext context = new StatementContext(query, mock(QueryMetadata.class));

        CayenneSqlException e = new CayenneSqlException("Query exception.", context, new RuntimeException());

        assertSame(query, e.getQuery());
        assertNull(e.getStatement());
        assertTrue(e.getMessage().contains("Query exception."), e.getMessage());
    }

    @Test
    public void nullRunIsAllowed() {
        CayenneSqlException e = new CayenneSqlException("Query exception.", null, new RuntimeException());

        assertNull(e.getStatementContext());
        assertNull(e.getQuery());
        assertNull(e.getStatement());
        assertTrue(e.getMessage().contains("Query exception."), e.getMessage());
    }

    private static StatementContext context(Query query, TranslatedStatement statement) {
        StatementContext context = new StatementContext(query, mock(QueryMetadata.class));
        context.setTranslated(statement);
        return context;
    }
}
