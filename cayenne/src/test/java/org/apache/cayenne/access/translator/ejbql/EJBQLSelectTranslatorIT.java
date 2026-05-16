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
package org.apache.cayenne.access.translator.ejbql;

import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLParser;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EJBQLSelectTranslatorIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private SQLTemplate translateSelect(String ejbql) {
        return translateSelect(ejbql, Collections.EMPTY_MAP);
    }

    private SQLTemplate translateSelect(String ejbql, final Map<Integer, Object> queryParameters) {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(ejbql, env.runtime().getDataDomain().getEntityResolver());
        EJBQLQuery query = new EJBQLQuery(ejbql) {

            @Override
            public Map<Integer, Object> getPositionalParameters() {
                return queryParameters;
            }
        };

        EJBQLTranslationContext tr = new EJBQLTranslationContext(env.runtime().getDataDomain().getEntityResolver(), query,
                select, new JdbcEJBQLTranslatorFactory(), env.dataNode().getAdapter().getQuotingStrategy());
        select.getExpression().visit(new EJBQLSelectTranslator(tr));
        return tr.getQuery();
    }

    @Test
    public void selectFrom() {
        SQLTemplate query = translateSelect("select a from Artist a");
        String sql = query.getDefaultTemplate();

        // column order is unpredictable, just need to ensure that they are all
        // there
        assertTrue(sql.startsWith("SELECT"), sql);
        assertTrue(sql.indexOf("t0.ARTIST_ID") > 0, sql);
        assertTrue(sql.indexOf("t0.ARTIST_NAME") > 0, sql);
        assertTrue(sql.indexOf("t0.DATE_OF_BIRTH") > 0, sql);
        assertTrue(sql.endsWith(" FROM ARTIST t0"), sql);
    }

    @Test
    public void selectMultipleJoinsToTheSameTable() throws Exception {
        SQLTemplate query = translateSelect("SELECT a "
                + "FROM Artist a JOIN a.paintingArray b JOIN a.paintingArray c "
                + "WHERE b.paintingTitle = 'P1' AND c.paintingTitle = 'P2'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.startsWith("SELECT"), sql);

        assertTrue(sql.indexOf("INNER JOIN PAINTING t1 ON (t0.ARTIST_ID = t1.ARTIST_ID)") > 0, sql);
        assertTrue(sql.indexOf("INNER JOIN PAINTING t2 ON (t0.ARTIST_ID = t2.ARTIST_ID)") > 0, sql);
    }

    @Test
    public void selectImplicitColumnJoins() throws Exception {
        SQLTemplate query = translateSelect("SELECT a.paintingArray.toGallery.galleryName "
                + "FROM Artist a JOIN a.paintingArray b");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.startsWith("SELECT"), sql);

        // check that overlapping implicit and explicit joins did not result in
        // duplicates

        assertTrue(sql.contains("INNER JOIN GALLERY"), sql);
        assertTrue(sql.contains("INNER JOIN PAINTING"), sql);

        int i1 = sql.indexOf("INNER JOIN PAINTING");
        assertTrue(i1 >= 0, sql);

        // TODO: andrus 1/6/2008 - this fails
        // int i2 = sql.indexOf("INNER JOIN PAINTING", i1 + 1);
        // assertTrue(i2 < 0, sql);
    }

    @Test
    public void selectDistinct() {
        SQLTemplate query = translateSelect("select distinct a from Artist a");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.startsWith("SELECT DISTINCT "), sql);
    }

    @Test
    public void selectFromWhereEqual() {
        SQLTemplate query = translateSelect("select a from Artist a where a.artistName = 'Dali'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.startsWith("SELECT"), sql);

        assertTrue(sql.endsWith(" FROM ARTIST t0 WHERE t0.ARTIST_NAME =" + " #bind('Dali' 'VARCHAR')"), sql);
    }

    @Test
    public void selectFromWhereOrEqual() {
        SQLTemplate query = translateSelect("select a from Artist a where a.artistName = 'Dali' "
                + "or a.artistName = 'Malevich'");
        String sql = query.getDefaultTemplate();

        SQLTemplate query1 = translateSelect("select a from Artist a where a.artistName = 'Picasso' "
                + "or a.artistName = 'Malevich' " + "or a.artistName = 'Dali'");
        String sql1 = query1.getDefaultTemplate();

        assertTrue(sql.startsWith("SELECT"), sql);
        assertTrue(sql.indexOf(" FROM ARTIST t0 WHERE ") > 0, sql);
        assertEquals(1, countDelimiters(sql, " OR ", sql.indexOf("WHERE ")));

        assertTrue(sql1.startsWith("SELECT"), sql1);
        assertTrue(sql.indexOf(" FROM ARTIST t0 WHERE ") > 0, sql1);
        assertEquals(2, countDelimiters(sql1, " OR ", sql.indexOf("WHERE ")));
    }

    @Test
    public void selectFromWhereAndEqual() {

        SQLTemplate query = translateSelect("select a from Artist a where a.artistName = 'Dali' "
                + "and a.artistName = 'Malevich'");
        String sql = query.getDefaultTemplate();

        SQLTemplate query1 = translateSelect("select a from Artist a where a.artistName = 'Picasso' "
                + "and a.artistName = 'Malevich' " + "and a.artistName = 'Dali'");
        String sql1 = query1.getDefaultTemplate();

        assertTrue(sql.startsWith("SELECT"), sql);
        assertTrue(sql.indexOf("WHERE ") > 0, sql);
        assertEquals(1, countDelimiters(sql, " AND ", sql.indexOf("WHERE ")));

        assertTrue(sql1.startsWith("SELECT"), sql1);
        assertTrue(sql1.indexOf("WHERE ") > 0, sql1);
        assertEquals(2, countDelimiters(sql1, " AND ", sql1.indexOf("WHERE ")));
    }

    @Test
    public void selectFromWhereNot() {
        SQLTemplate query = translateSelect("select a from Artist a where not (a.artistName = 'Dali')");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.startsWith("SELECT"), sql);
        assertTrue(sql.endsWith("WHERE NOT " + "t0.ARTIST_NAME = #bind('Dali' 'VARCHAR')"), sql);
    }

    @Test
    public void selectFromWhereGreater() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice > 1.0");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.startsWith("SELECT"), sql);
        assertTrue(sql.endsWith("WHERE t0.ESTIMATED_PRICE > #bind($id0 'DECIMAL')"), sql);
    }

    @Test
    public void selectFromWhereGreaterOrEqual() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice >= 2");
        String sql = query.getDefaultTemplate();
        assertTrue(sql.endsWith("WHERE t0.ESTIMATED_PRICE >= #bind($id0 'INTEGER')"), sql);
    }

    @Test
    public void selectFromWhereLess() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice < 1.0");
        String sql = query.getDefaultTemplate();
        assertTrue(sql.endsWith("WHERE t0.ESTIMATED_PRICE < #bind($id0 'DECIMAL')"), sql);
    }

    @Test
    public void selectFromWhereLessOrEqual() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice <= 1.0");
        String sql = query.getDefaultTemplate();
        assertTrue(sql.endsWith("WHERE t0.ESTIMATED_PRICE <= #bind($id0 'DECIMAL')"), sql);
    }

    @Test
    public void selectFromWhereNotEqual() {
        SQLTemplate query = translateSelect("select a from Artist a where a.artistName <> 'Dali'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.endsWith("WHERE t0.ARTIST_NAME <> #bind('Dali' 'VARCHAR')"), sql);
    }

    @Test
    public void selectFromWhereBetween() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice between 3 and 5");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.endsWith("WHERE t0.ESTIMATED_PRICE "
                + "BETWEEN #bind($id0 'INTEGER') AND #bind($id1 'INTEGER')"), sql);
    }

    @Test
    public void selectFromWhereNotBetween() {
        SQLTemplate query = translateSelect("select p from Painting p where p.estimatedPrice not between 3 and 5");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.endsWith("WHERE t0.ESTIMATED_PRICE "
                + "NOT BETWEEN #bind($id0 'INTEGER') AND #bind($id1 'INTEGER')"), sql);
    }

    @Test
    public void selectFromWhereLike() {
        SQLTemplate query = translateSelect("select p from Painting p where p.paintingTitle like 'Stuff'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.endsWith("WHERE t0.PAINTING_TITLE " + "LIKE #bind('Stuff' 'VARCHAR')"), sql);
    }

    @Test
    public void selectFromWhereNotLike() {
        SQLTemplate query = translateSelect("select p from Painting p where p.paintingTitle NOT like 'Stuff'");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.endsWith("WHERE t0.PAINTING_TITLE " + "NOT LIKE #bind('Stuff' 'VARCHAR')"), sql);
    }

    @Test
    public void selectPositionalParameters() {
        Map<Integer, Object> params = new HashMap<Integer, Object>();
        params.put(1, "X");
        params.put(2, "Y");
        SQLTemplate query = translateSelect("select a from Artist a where a.artistName = ?1 or a.artistName = ?2",
                params);
        String sql = query.getDefaultTemplate();
        assertTrue(sql.endsWith("t0.ARTIST_NAME = #bind($id0) OR t0.ARTIST_NAME = #bind($id1)"), sql);
    }

    @Test
    public void max() {
        SQLTemplate query = translateSelect("select max(p.estimatedPrice) from Painting p");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.startsWith("SELECT " + "#result('MAX(t0.ESTIMATED_PRICE)' 'java.math.BigDecimal' 'sc0') "
                + "FROM PAINTING t0"), sql);
    }

    @Test
    public void distinctSum() {
        SQLTemplate query = translateSelect("select sum( distinct p.estimatedPrice) from Painting p");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.startsWith("SELECT #result('SUM(DISTINCT t0.ESTIMATED_PRICE)' 'java.math.BigDecimal' 'sc0') "
                        + "FROM PAINTING t0"), sql);
    }

    @Test
    public void columnPaths() {
        SQLTemplate query = translateSelect("select p.estimatedPrice, p.toArtist.artistName from Painting p");
        String sql = query.getDefaultTemplate();

        assertTrue(sql.startsWith("SELECT "
                + "#result('t0.ESTIMATED_PRICE' 'java.math.BigDecimal' 'sc0' 'sc0' 3), "
                + "#result('t1.ARTIST_NAME' 'java.lang.String' 'sc1' 'sc1' 1) FROM"), sql);
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
    @Test
    public void equalsNullParameter() {
        String ejbql = "select p from Painting p WHERE p.toArtist=:x";
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(ejbql, env.runtime().getDataDomain().getEntityResolver());
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("x", null);

        EJBQLTranslationContext tr = new EJBQLTranslationContext(env.runtime().getDataDomain().getEntityResolver(), query,
                select, new JdbcEJBQLTranslatorFactory(), env.dataNode().getAdapter().getQuotingStrategy());
        select.getExpression().visit(new EJBQLSelectTranslator(tr));
        String sql = tr.getQuery().getDefaultTemplate();
        assertTrue(sql.endsWith("t0.ARTIST_ID IS NULL"), sql);
    }

    // if parameter value is null and more than one parameter in query
    @Test
    public void equalsNullAndNotNullParameter() {
        String ejbql = "select p from Painting p WHERE p.toArtist=:x OR p.toArtist.artistName=:b";
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(ejbql, env.runtime().getDataDomain().getEntityResolver());
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("x", null);
        query.setParameter("b", "Y");

        EJBQLTranslationContext tr = new EJBQLTranslationContext(env.runtime().getDataDomain().getEntityResolver(), query,
                select, new JdbcEJBQLTranslatorFactory(), env.dataNode().getAdapter().getQuotingStrategy());
        select.getExpression().visit(new EJBQLSelectTranslator(tr));
        String sql = tr.getQuery().getDefaultTemplate();
        assertTrue(sql.endsWith("t0.ARTIST_ID IS NULL OR t1.ARTIST_NAME = #bind($id0)"), sql);
    }
}
