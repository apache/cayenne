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
package org.apache.cayenne.access.jdbc;

import java.util.Collections;

import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLParser;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;

public class EJBQLSelectTranslatorTest extends CayenneCase {

    private SQLTemplate translateSelect(String ejbql) {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(ejbql, getDomain()
                .getEntityResolver());

        EJBQLTranslationContext tr = new EJBQLTranslationContext(
                select,
                Collections.EMPTY_MAP);
        select.getExpression().visit(new EJBQLSelectTranslator(tr));
        return tr.getQuery();
    }

    public void testSelectFrom() {
        SQLTemplate query = translateSelect("select a from Artist a");
        String sql = query.getDefaultTemplate();

        // column order is unpredictable, just need to ensure that they are all there
        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.indexOf("t0.ARTIST_ID") > 0);
        assertTrue(sql, sql.indexOf("t0.ARTIST_NAME") > 0);
        assertTrue(sql, sql.indexOf("t0.DATE_OF_BIRTH") > 0);
        assertTrue(sql, sql.endsWith(" FROM ARTIST AS t0${marker0}"));
    }

    public void testSelectMultipleJoinsToTheSameTable() throws Exception {
        SQLTemplate query = translateSelect("SELECT a "
                + "FROM Artist a JOIN a.paintingArray b JOIN a.paintingArray c "
                + "WHERE b.paintingTitle = 'P1' AND c.paintingTitle = 'P2'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(
                sql,
                sql.indexOf("INNER JOIN PAINTING AS t1 ON (t0.ARTIST_ID = t1.ARTIST_ID)") > 0);
        assertTrue(
                sql,
                sql.indexOf("INNER JOIN PAINTING AS t2 ON (t0.ARTIST_ID = t2.ARTIST_ID)") > 0);

    }

    public void testSelectDistinct() {
        SQLTemplate query = translateSelect("select distinct a from Artist a");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT DISTINCT "));
        assertTrue(sql, sql.endsWith(" FROM ARTIST AS t0${marker0}"));
    }

    public void testSelectFromWhereEqual() {
        SQLTemplate query = translateSelect("select a from Artist a where a.artistName = 'Dali'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" FROM ARTIST AS t0${marker0} WHERE t0.ARTIST_NAME "
                + "#bindEqual('Dali' 'VARCHAR')"));
    }

    public void testSelectFromWhereOrEqual() {
        SQLTemplate query = translateSelect("select a from Artist a where a.artistName = 'Dali' "
                + "or a.artistName = 'Malevich'");
        String sql = query.getDefaultTemplate();

        SQLTemplate query1 = translateSelect("select a from Artist a where a.artistName = 'Picasso' "
                + "or a.artistName = 'Malevich' "
                + "or a.artistName = 'Dali'");
        String sql1 = query1.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.indexOf(" FROM ARTIST AS t0${marker0} WHERE ") > 0);
        assertEquals(1, countDelimiters(sql, " OR ", sql.indexOf("WHERE ")));

        assertTrue(sql1, sql1.startsWith("SELECT "));
        assertTrue(sql1, sql.indexOf(" FROM ARTIST AS t0${marker0} WHERE ") > 0);
        assertEquals(2, countDelimiters(sql1, " OR ", sql.indexOf("WHERE ")));
    }

    public void testSelectFromWhereAndEqual() {

        SQLTemplate query = translateSelect("select a from Artist a where a.artistName = 'Dali' "
                + "and a.artistName = 'Malevich'");
        String sql = query.getDefaultTemplate();

        SQLTemplate query1 = translateSelect("select a from Artist a where a.artistName = 'Picasso' "
                + "and a.artistName = 'Malevich' "
                + "and a.artistName = 'Dali'");
        String sql1 = query1.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.indexOf(" WHERE ") > 0);
        assertEquals(1, countDelimiters(sql, " AND ", sql.indexOf("WHERE ")));

        assertTrue(sql1, sql1.startsWith("SELECT "));
        assertTrue(sql1, sql1.indexOf(" WHERE ") > 0);
        assertEquals(2, countDelimiters(sql1, " AND ", sql1.indexOf("WHERE ")));
    }

    public void testSelectFromWhereNot() {
        SQLTemplate query = translateSelect("select a from Artist a where not (a.artistName = 'Dali')");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE NOT "
                + "t0.ARTIST_NAME #bindEqual('Dali' 'VARCHAR')"));
    }

    public void testSelectFromWhereGreater() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice > 1.0");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t0.ESTIMATED_PRICE > #bind($id1 'DECIMAL')"));
    }

    public void testSelectFromWhereGreaterOrEqual() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice >= 2");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql
                .endsWith(" WHERE t0.ESTIMATED_PRICE >= #bind($id1 'INTEGER')"));
    }

    public void testSelectFromWhereLess() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice < 1.0");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t0.ESTIMATED_PRICE < #bind($id1 'DECIMAL')"));
    }

    public void testSelectFromWhereLessOrEqual() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice <= 1.0");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql
                .endsWith(" WHERE t0.ESTIMATED_PRICE <= #bind($id1 'DECIMAL')"));
    }

    public void testSelectFromWhereNotEqual() {
        SQLTemplate query = translateSelect("select a from Artist a where a.artistName <> 'Dali'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql
                .endsWith(" WHERE t0.ARTIST_NAME #bindNotEqual('Dali' 'VARCHAR')"));
    }

    public void testSelectFromWhereBetween() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice between 3 and 5");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t0.ESTIMATED_PRICE "
                + "BETWEEN #bind($id1 'INTEGER') AND #bind($id2 'INTEGER')"));
    }

    public void testSelectFromWhereNotBetween() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice not between 3 and 5");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t0.ESTIMATED_PRICE "
                + "NOT BETWEEN #bind($id1 'INTEGER') AND #bind($id2 'INTEGER')"));
    }

    public void testSelectFromWhereLike() {
        SQLTemplate query = translateSelect("select p from Painting p where p.paintingTitle like 'Stuff'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t0.PAINTING_TITLE "
                + "LIKE #bind('Stuff' 'VARCHAR')"));
    }

    public void testSelectFromWhereNotLike() {
        SQLTemplate query = translateSelect("select p from Painting p where p.paintingTitle NOT like 'Stuff'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t0.PAINTING_TITLE "
                + "NOT LIKE #bind('Stuff' 'VARCHAR')"));
    }

    public void testSelectPositionalParameters() {
        SQLTemplate query = translateSelect("select a from Artist a where a.artistName = ?1 or a.artistName = ?2");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(
                sql,
                sql
                        .endsWith("t0.ARTIST_NAME #bindEqual($id1) OR t0.ARTIST_NAME #bindEqual($id2)"));
    }

    private int countDelimiters(String string, String delim, int fromIndex) {
        int i = 0;
        while ((fromIndex = string.indexOf(delim, fromIndex)) >= 0) {
            fromIndex += delim.length();
            i++;
        }

        return i;
    }
}
