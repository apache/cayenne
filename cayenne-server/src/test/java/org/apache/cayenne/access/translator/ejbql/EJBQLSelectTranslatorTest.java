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
package org.apache.cayenne.access.translator.ejbql;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.access.translator.ejbql.EJBQLSelectTranslator;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslationContext;
import org.apache.cayenne.access.translator.ejbql.JdbcEJBQLTranslatorFactory;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLParser;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class EJBQLSelectTranslatorTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private DbAdapter adapter;

    private SQLTemplate translateSelect(String ejbql) {
        return translateSelect(ejbql, Collections.EMPTY_MAP);
    }

    private SQLTemplate translateSelect(String ejbql, final Map<Integer, Object> queryParameters) {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(ejbql, runtime.getDataDomain().getEntityResolver());
        EJBQLQuery query = new EJBQLQuery(ejbql) {

            @Override
            public Map<Integer, Object> getPositionalParameters() {
                return queryParameters;
            }
        };

        EJBQLTranslationContext tr = new EJBQLTranslationContext(runtime.getDataDomain().getEntityResolver(), query,
                select, new JdbcEJBQLTranslatorFactory(), adapter.getQuotingStrategy());
        select.getExpression().visit(new EJBQLSelectTranslator(tr));
        return tr.getQuery();
    }

    public void testSelectFrom() {
        SQLTemplate query = translateSelect("select a from Artist a");
        String sql = query.getDefaultTemplate();

        // column order is unpredictable, just need to ensure that they are all
        // there
        assertTrue(sql, sql.startsWith("SELECT"));
        assertTrue(sql, sql.indexOf("t0.ARTIST_ID") > 0);
        assertTrue(sql, sql.indexOf("t0.ARTIST_NAME") > 0);
        assertTrue(sql, sql.indexOf("t0.DATE_OF_BIRTH") > 0);
        assertTrue(sql, sql.endsWith(" FROM ARTIST t0"));
    }

    public void testSelectMultipleJoinsToTheSameTable() throws Exception {
        SQLTemplate query = translateSelect("SELECT a "
                + "FROM Artist a JOIN a.paintingArray b JOIN a.paintingArray c "
                + "WHERE b.paintingTitle = 'P1' AND c.paintingTitle = 'P2'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT"));

        assertTrue(sql, sql.indexOf("INNER JOIN PAINTING t1 ON (t0.ARTIST_ID = t1.ARTIST_ID)") > 0);
        assertTrue(sql, sql.indexOf("INNER JOIN PAINTING t2 ON (t0.ARTIST_ID = t2.ARTIST_ID)") > 0);
    }

    public void testSelectImplicitColumnJoins() throws Exception {
        SQLTemplate query = translateSelect("SELECT a.paintingArray.toGallery.galleryName "
                + "FROM Artist a JOIN a.paintingArray b");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT"));

        // check that overlapping implicit and explicit joins did not result in
        // duplicates

        assertTrue(sql, sql.contains("INNER JOIN GALLERY"));
        assertTrue(sql, sql.contains("INNER JOIN PAINTING"));

        int i1 = sql.indexOf("INNER JOIN PAINTING");
        assertTrue(sql, i1 >= 0);

        // TODO: andrus 1/6/2008 - this fails
        // int i2 = sql.indexOf("INNER JOIN PAINTING", i1 + 1);
        // assertTrue(sql, i2 < 0);
    }

    public void testSelectDistinct() {
        SQLTemplate query = translateSelect("select distinct a from Artist a");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT DISTINCT "));
    }

    public void testSelectFromWhereEqual() {
        SQLTemplate query = translateSelect("select a from Artist a where a.artistName = 'Dali'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT"));

        assertTrue(sql, sql.endsWith(" FROM ARTIST t0 WHERE t0.ARTIST_NAME =" + " #bind('Dali' 'VARCHAR')"));
    }

    public void testSelectFromWhereOrEqual() {
        SQLTemplate query = translateSelect("select a from Artist a where a.artistName = 'Dali' "
                + "or a.artistName = 'Malevich'");
        String sql = query.getDefaultTemplate();

        SQLTemplate query1 = translateSelect("select a from Artist a where a.artistName = 'Picasso' "
                + "or a.artistName = 'Malevich' " + "or a.artistName = 'Dali'");
        String sql1 = query1.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT"));
        assertTrue(sql, sql.indexOf(" FROM ARTIST t0 WHERE ") > 0);
        assertEquals(1, countDelimiters(sql, " OR ", sql.indexOf("WHERE ")));

        assertTrue(sql1, sql1.startsWith("SELECT"));
        assertTrue(sql1, sql.indexOf(" FROM ARTIST t0 WHERE ") > 0);
        assertEquals(2, countDelimiters(sql1, " OR ", sql.indexOf("WHERE ")));
    }

    public void testSelectFromWhereAndEqual() {

        SQLTemplate query = translateSelect("select a from Artist a where a.artistName = 'Dali' "
                + "and a.artistName = 'Malevich'");
        String sql = query.getDefaultTemplate();

        SQLTemplate query1 = translateSelect("select a from Artist a where a.artistName = 'Picasso' "
                + "and a.artistName = 'Malevich' " + "and a.artistName = 'Dali'");
        String sql1 = query1.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT"));
        assertTrue(sql, sql.indexOf("WHERE ") > 0);
        assertEquals(1, countDelimiters(sql, " AND ", sql.indexOf("WHERE ")));

        assertTrue(sql1, sql1.startsWith("SELECT"));
        assertTrue(sql1, sql1.indexOf("WHERE ") > 0);
        assertEquals(2, countDelimiters(sql1, " AND ", sql1.indexOf("WHERE ")));
    }

    public void testSelectFromWhereNot() {
        SQLTemplate query = translateSelect("select a from Artist a where not (a.artistName = 'Dali')");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT"));
        assertTrue(sql, sql.endsWith("WHERE NOT " + "t0.ARTIST_NAME = #bind('Dali' 'VARCHAR')"));
    }

    public void testSelectFromWhereGreater() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice > 1.0");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT"));
        assertTrue(sql, sql.endsWith("WHERE t0.ESTIMATED_PRICE > #bind($id0 'DECIMAL')"));
    }

    public void testSelectFromWhereGreaterOrEqual() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice >= 2");
        String sql = query.getDefaultTemplate();
        assertTrue(sql, sql.endsWith("WHERE t0.ESTIMATED_PRICE >= #bind($id0 'INTEGER')"));
    }

    public void testSelectFromWhereLess() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice < 1.0");
        String sql = query.getDefaultTemplate();
        assertTrue(sql, sql.endsWith("WHERE t0.ESTIMATED_PRICE < #bind($id0 'DECIMAL')"));
    }

    public void testSelectFromWhereLessOrEqual() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice <= 1.0");
        String sql = query.getDefaultTemplate();
        assertTrue(sql, sql.endsWith("WHERE t0.ESTIMATED_PRICE <= #bind($id0 'DECIMAL')"));
    }

    public void testSelectFromWhereNotEqual() {
        SQLTemplate query = translateSelect("select a from Artist a where a.artistName <> 'Dali'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.endsWith("WHERE t0.ARTIST_NAME <> #bind('Dali' 'VARCHAR')"));
    }

    public void testSelectFromWhereBetween() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice between 3 and 5");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.endsWith("WHERE t0.ESTIMATED_PRICE "
                + "BETWEEN #bind($id0 'INTEGER') AND #bind($id1 'INTEGER')"));
    }

    public void testSelectFromWhereNotBetween() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice not between 3 and 5");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.endsWith("WHERE t0.ESTIMATED_PRICE "
                + "NOT BETWEEN #bind($id0 'INTEGER') AND #bind($id1 'INTEGER')"));
    }

    public void testSelectFromWhereLike() {
        SQLTemplate query = translateSelect("select p from Painting p where p.paintingTitle like 'Stuff'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.endsWith("WHERE t0.PAINTING_TITLE " + "LIKE #bind('Stuff' 'VARCHAR')"));
    }

    public void testSelectFromWhereNotLike() {
        SQLTemplate query = translateSelect("select p from Painting p where p.paintingTitle NOT like 'Stuff'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.endsWith("WHERE t0.PAINTING_TITLE " + "NOT LIKE #bind('Stuff' 'VARCHAR')"));
    }

    public void testSelectPositionalParameters() {
        Map<Integer, Object> params = new HashMap<Integer, Object>();
        params.put(new Integer(1), "X");
        params.put(new Integer(2), "Y");
        SQLTemplate query = translateSelect("select a from Artist a where a.artistName = ?1 or a.artistName = ?2",
                params);
        String sql = query.getDefaultTemplate();
        assertTrue(sql, sql.endsWith("t0.ARTIST_NAME = #bind($id0) OR t0.ARTIST_NAME = #bind($id1)"));
    }

    public void testMax() {
        SQLTemplate query = translateSelect("select max(p.estimatedPrice) from Painting p");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT " + "#result('MAX(t0.ESTIMATED_PRICE)' 'java.math.BigDecimal' 'sc0') "
                + "FROM PAINTING t0"));
    }

    public void testDistinctSum() {
        SQLTemplate query = translateSelect("select sum( distinct p.estimatedPrice) from Painting p");
        String sql = query.getDefaultTemplate();

        assertTrue(sql,
                sql.startsWith("SELECT #result('SUM(DISTINCT t0.ESTIMATED_PRICE)' 'java.math.BigDecimal' 'sc0') "
                        + "FROM PAINTING t0"));
    }

    public void testColumnPaths() {
        SQLTemplate query = translateSelect("select p.estimatedPrice, p.toArtist.artistName from Painting p");
        String sql = query.getDefaultTemplate();

        assertTrue(sql, sql.startsWith("SELECT "
                + "#result('t0.ESTIMATED_PRICE' 'java.math.BigDecimal' 'sc0' 'sc0' 3), "
                + "#result('t1.ARTIST_NAME' 'java.lang.String' 'sc1' 'sc1' 1) FROM"));
    }

    private int countDelimiters(String string, String delim, int fromIndex) {
        int i = 0;
        while ((fromIndex = string.indexOf(delim, fromIndex)) >= 0) {
            fromIndex += delim.length();
            i++;
        }

        return i;
    }

    // if parameter value is null (in this test x := null) we will generate
    // "IS NULL"
    public void testEqualsNullParameter() {
        String ejbql = "select p from Painting p WHERE p.toArtist=:x";
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(ejbql, runtime.getDataDomain().getEntityResolver());
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("x", null);

        EJBQLTranslationContext tr = new EJBQLTranslationContext(runtime.getDataDomain().getEntityResolver(), query,
                select, new JdbcEJBQLTranslatorFactory(), adapter.getQuotingStrategy());
        select.getExpression().visit(new EJBQLSelectTranslator(tr));
        String sql = tr.getQuery().getDefaultTemplate();
        assertTrue(sql, sql.endsWith("t0.ARTIST_ID IS NULL"));
    }

    // if parameter value is null and more than one parameter in query
    public void testEqualsNullAndNotNullParameter() {
        String ejbql = "select p from Painting p WHERE p.toArtist=:x OR p.toArtist.artistName=:b";
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(ejbql, runtime.getDataDomain().getEntityResolver());
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("x", null);
        query.setParameter("b", "Y");

        EJBQLTranslationContext tr = new EJBQLTranslationContext(runtime.getDataDomain().getEntityResolver(), query,
                select, new JdbcEJBQLTranslatorFactory(), adapter.getQuotingStrategy());
        select.getExpression().visit(new EJBQLSelectTranslator(tr));
        String sql = tr.getQuery().getDefaultTemplate();
        assertTrue(sql, sql.endsWith("t0.ARTIST_ID IS NULL OR t1.ARTIST_NAME = #bind($id0)"));
    }
}
