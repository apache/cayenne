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
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.SQLQuery;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.query.resolved.ResolvedSQL;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CayenneSqlExceptionTest {

    @Test
    public void messageIncludesSql() {
        SQLQuery<?> query = SQLQuery.query("SELECT * FROM ARTIST");
        ResolvedSQL resolved = query.resolve(new EntityResolver());
        TranslatedStatement statement = new TranslatedSQL(resolved, "SELECT * FROM ARTIST", null, null);
        Throwable cause = new RuntimeException("boom");

        StatementContext context = context(resolved, statement);
        CayenneSqlException e = new CayenneSqlException("Query exception.", context, cause);

        assertSame(context, e.getStatementContext());
        assertSame(query, e.getQuery());
        assertSame(statement, e.getTranslatedStatement());
        assertSame(cause, e.getCause());
        assertTrue(e.getMessage().contains("Query exception."), e.getMessage());
        assertTrue(e.getMessage().contains("SELECT * FROM ARTIST"), e.getMessage());
    }

    @Test
    public void queryNotIncludedInMessage() {
        SQLQuery<?> query = SQLQuery.query("SELECT * FROM ARTIST");
        ResolvedSQL resolved = query.resolve(new EntityResolver());
        TranslatedStatement statement = new TranslatedSQL(resolved, "SELECT * FROM ARTIST", null, null);

        CayenneSqlException e = new CayenneSqlException("Query exception.", context(resolved, statement),
                new RuntimeException());

        assertSame(query, e.getQuery());
        assertFalse(e.getMessage().contains(query.toString()), e.getMessage());
    }

    @Test
    public void sqlWithPercentDoesNotBreakFormatting() {
        // SQL may contain '%' (e.g. LIKE patterns); it must not be interpreted as a format specifier
        SQLQuery<?> query = SQLQuery.query("SELECT * FROM ARTIST WHERE NAME LIKE '%foo%'");
        ResolvedSQL resolved = query.resolve(new EntityResolver());
        TranslatedStatement statement = new TranslatedSQL(resolved, "SELECT * FROM ARTIST WHERE NAME LIKE '%foo%'",
                null, null);
        CayenneSqlException e = new CayenneSqlException("Query exception.", context(resolved, statement),
                new RuntimeException());

        assertTrue(e.getMessage().contains("LIKE '%foo%'"), e.getMessage());
    }

    @Test
    public void untranslatedRunHasNoStatement() {
        SQLQuery<?> query = SQLQuery.query("SELECT * FROM ARTIST");
        StatementContext context = new StatementContext(query.resolve(new EntityResolver()), mock(EntityResolver.class));

        CayenneSqlException e = new CayenneSqlException("Query exception.", context, new RuntimeException());

        assertSame(query, e.getQuery());
        assertNull(e.getTranslatedStatement());
        assertTrue(e.getMessage().contains("Query exception."), e.getMessage());
    }

    @Test
    public void nullRunIsAllowed() {
        CayenneSqlException e = new CayenneSqlException("Query exception.", null, new RuntimeException());

        assertNull(e.getStatementContext());
        assertNull(e.getQuery());
        assertNull(e.getTranslatedStatement());
        assertTrue(e.getMessage().contains("Query exception."), e.getMessage());
    }

    private static StatementContext context(ResolvedQuery query, TranslatedStatement statement) {
        StatementContext context = new StatementContext(query, mock(EntityResolver.class));
        context.setTranslatedStatement(statement);
        return context;
    }
}
