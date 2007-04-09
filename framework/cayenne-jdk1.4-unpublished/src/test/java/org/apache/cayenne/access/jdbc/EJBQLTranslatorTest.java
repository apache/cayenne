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

import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLParser;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;

public class EJBQLTranslatorTest extends CayenneCase {

    public void testSelectFrom() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select a from Artist a",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        // column order is unpredictable, just need to ensure that they are all there
        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.indexOf("t0.ARTIST_ID") > 0);
        assertTrue(sql, sql.indexOf("t0.ARTIST_NAME") > 0);
        assertTrue(sql, sql.indexOf("t0.DATE_OF_BIRTH") > 0);
        assertTrue(sql, sql.endsWith(" FROM ARTIST AS t0"));
    }

    public void testSelectDistinct() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select distinct a from Artist a",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT DISTINCT "));
        assertTrue(sql, sql.endsWith(" FROM ARTIST AS t0"));
    }

    public void testSelectFromWhereEqual() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select a from Artist a where a.artistName = 'Dali'",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" FROM ARTIST AS t0 WHERE t0.ARTIST_NAME "
                + "#bindEqual('Dali' 'VARCHAR')"));
    }

    public void testSelectFromWhereOrEqual() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select a from Artist a where a.artistName = 'Dali' "
                        + "or a.artistName = 'Malevich'",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        EJBQLCompiledExpression select1 = parser.compile(
                "select a from Artist a where a.artistName = 'Picasso' "
                        + "or a.artistName = 'Malevich' "
                        + "or a.artistName = 'Dali'",
                getDomain().getEntityResolver());

        EJBQLTranslator tr1 = new EJBQLTranslator(select1);
        SQLTemplate query1 = tr1.translate();
        String sql1 = query1.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.indexOf(" FROM ARTIST AS t0 WHERE ") > 0);
        assertEquals(1, countDelimiters(sql, " OR ", sql.indexOf("WHERE ")));

        assertTrue(sql1, sql1.startsWith("SELECT "));
        assertTrue(sql1, sql.indexOf(" FROM ARTIST AS t0 WHERE ") > 0);
        assertEquals(2, countDelimiters(sql1, " OR ", sql.indexOf("WHERE ")));
    }

    public void testSelectFromWhereAndEqual() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select a from Artist a where a.artistName = 'Dali' "
                        + "and a.artistName = 'Malevich'",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        EJBQLCompiledExpression select1 = parser.compile(
                "select a from Artist a where a.artistName = 'Picasso' "
                        + "and a.artistName = 'Malevich' "
                        + "and a.artistName = 'Dali'",
                getDomain().getEntityResolver());

        EJBQLTranslator tr1 = new EJBQLTranslator(select1);
        SQLTemplate query1 = tr1.translate();
        String sql1 = query1.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.indexOf(" WHERE ") > 0);
        assertEquals(1, countDelimiters(sql, " AND ", sql.indexOf("WHERE ")));

        assertTrue(sql1, sql1.startsWith("SELECT "));
        assertTrue(sql1, sql1.indexOf(" WHERE ") > 0);
        assertEquals(2, countDelimiters(sql1, " AND ", sql1.indexOf("WHERE ")));
    }

    public void testSelectFromWhereNot() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select a from Artist a where not (a.artistName = 'Dali')",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE NOT "
                + "t0.ARTIST_NAME #bindEqual('Dali' 'VARCHAR')"));
    }

    public void testSelectFromWhereGreater() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select p from Painting p where p.estimatedPrice > 1.0",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t0.ESTIMATED_PRICE > #bind($id0 'DECIMAL')"));
    }

    public void testSelectFromWhereGreaterOrEqual() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select p from Painting p where p.estimatedPrice >= 2",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql
                .endsWith(" WHERE t0.ESTIMATED_PRICE >= #bind($id0 'INTEGER')"));
    }

    public void testSelectFromWhereLess() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select p from Painting p where p.estimatedPrice < 1.0",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t0.ESTIMATED_PRICE < #bind($id0 'DECIMAL')"));
    }

    public void testSelectFromWhereLessOrEqual() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select p from Painting p where p.estimatedPrice <= 1.0",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql
                .endsWith(" WHERE t0.ESTIMATED_PRICE <= #bind($id0 'DECIMAL')"));
    }

    public void testSelectFromWhereNotEqual() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select a from Artist a where a.artistName <> 'Dali'",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql
                .endsWith(" WHERE t0.ARTIST_NAME #bindNotEqual('Dali' 'VARCHAR')"));
    }

    public void testSelectFromWhereBetween() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select p from Painting p where p.estimatedPrice between 3 and 5",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t0.ESTIMATED_PRICE "
                + "BETWEEN #bind($id0 'INTEGER') AND #bind($id1 'INTEGER')"));
    }

    public void testSelectFromWhereNotBetween() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select p from Painting p where p.estimatedPrice not between 3 and 5",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t0.ESTIMATED_PRICE "
                + "NOT BETWEEN #bind($id0 'INTEGER') AND #bind($id1 'INTEGER')"));
    }

    public void testSelectFromWhereLike() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select p from Painting p where p.paintingTitle like 'Stuff'",
                getDomain().getEntityResolver());

        System.out.println(select.getExpression().toString());
        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t0.PAINTING_TITLE "
                + "LIKE #bind('Stuff' 'VARCHAR')"));
    }

    public void testSelectFromWhereNotLike() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select p from Painting p where p.paintingTitle NOT like 'Stuff'",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t0.PAINTING_TITLE "
                + "NOT LIKE #bind('Stuff' 'VARCHAR')"));
    }

    public void testSelectFromWhereRelationshipPropertyPath() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select p from Painting p where p.toArtist.artistName = 'AA2'",
                getDomain().getEntityResolver());

        System.out.println("Expression: " + select.getExpression());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        SQLTemplate query = tr.translate();
        String sql = query.getDefaultTemplate();

        System.out.println("SQL: " + sql);

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.endsWith(" WHERE t1.ARTIST_NAME #bindEqual('AA2' 'VARCHAR')"));
        // TODO: andrus, 3/25/2007 - implement joins support
        // assertEquals(" FROM PAINTING t0 JOIN ARTIST t1 ON (t0.ARTIST_ID =
        // t1.ARTIST_ID)", query.getParameters().get("from0"));
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
