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

import org.junit.jupiter.api.Test;

import static org.apache.cayenne.log.FormattedSlf4jJdbcEventLogger.formatQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Assertions use text blocks so the resulting SQL layout is visible. The formatter emits trailing spaces on many
 * lines and indents continuations with a tab; since text blocks strip trailing whitespace, those are spelled out
 * explicitly as {@code \s} (space) and {@code \t} (tab).
 */
public class FormattedSlf4jJdbcEventLoggerTest {

    @Test
    public void formatQuery_breaksBeforeKeywords() {
        assertEquals("""
                SELECT *\s
                FROM ARTIST
                """,
                formatQuery("SELECT * FROM ARTIST"));
    }

    @Test
    public void formatQuery_fullSelect() {
        assertEquals("""
                SELECT t0.ID,
                \t t0.NAME\s
                FROM ARTIST t0\s
                WHERE t0.NAME = ?\s
                ORDER BY t0.NAME
                """,
                formatQuery("SELECT t0.ID, t0.NAME FROM ARTIST t0 WHERE t0.NAME = ? ORDER BY t0.NAME"));
    }

    @Test
    public void formatQuery_update() {
        assertEquals("""
                UPDATE ARTIST\s
                SET NAME = ?\s
                WHERE ID = ?
                """,
                formatQuery("UPDATE ARTIST SET NAME = ? WHERE ID = ?"));
    }

    @Test
    public void formatQuery_delete() {
        assertEquals("""
                DELETE\s
                FROM ARTIST\s
                WHERE ID = ?
                """,
                formatQuery("DELETE FROM ARTIST WHERE ID = ?"));
    }

    @Test
    public void formatQuery_insertBreaksOnParensAndCommas() {
        assertEquals("""
                INSERT INTO ARTIST (
                \tID,
                \t NAME)
                \t\s
                VALUES (
                \t?,
                \t ?)
                \t
                """,
                formatQuery("INSERT INTO ARTIST (ID, NAME) VALUES (?, ?)"));
    }

    @Test
    public void formatQuery_groupBy() {
        assertEquals("""
                SELECT COUNT(
                \t*)
                \t\s
                FROM ARTIST\s
                GROUP BY NAME
                """,
                formatQuery("SELECT COUNT(*) FROM ARTIST GROUP BY NAME"));
    }

    /**
     * The leading keyword of a statement has no preceding space, so it is not matched and not broken onto its own line.
     */
    @Test
    public void formatQuery_leadingKeywordNotBroken() {
        assertEquals("""
                SELECT *\s
                FROM ARTIST
                """,
                formatQuery("SELECT * FROM ARTIST"));
    }

    /**
     * Keywords, commas and parentheses that appear inside a quoted string literal must be left untouched.
     */
    @Test
    public void formatQuery_ignoresContentInsideStringLiterals() {
        assertEquals("""
                SELECT *\s
                FROM T\s
                WHERE NAME = 'a, b, (c)'
                """,
                formatQuery("SELECT * FROM T WHERE NAME = 'a, b, (c)'"));
        assertEquals("""
                SELECT *\s
                FROM T\s
                WHERE NAME = 'from the where group'
                """,
                formatQuery("SELECT * FROM T WHERE NAME = 'from the where group'"));
    }

    /**
     * A doubled single quote is an escaped apostrophe within a literal and must not toggle the "inside literal" state.
     */
    @Test
    public void formatQuery_handlesEscapedQuotesInsideLiterals() {
        assertEquals("""
                SELECT *\s
                FROM T\s
                WHERE NAME = 'O''Brien from x'
                """,
                formatQuery("SELECT * FROM T WHERE NAME = 'O''Brien from x'"));
    }

    @Test
    public void formatQuery_collapsesRepeatedSpaces() {
        assertEquals("""
                SELECT *\s
                FROM T
                """,
                formatQuery("SELECT   *   FROM   T"));
    }

    @Test
    public void formatQuery_noKeywords() {
        assertEquals("""
                no keywords here just text
                """,
                formatQuery("no keywords here just text"));
    }

    @Test
    public void formatQuery_empty() {
        // a bare newline is clearer as a plain literal than as a text block
        assertEquals("\n", formatQuery(""));
    }
}
