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

package org.apache.cayenne.access.trans;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.art.Artist;
import org.apache.art.ArtistExhibit;
import org.apache.art.CompoundPainting;
import org.apache.art.Painting;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class SelectTranslatorTest extends CayenneCase {

    /**
     * Tests query creation with qualifier and ordering.
     */
    public void testCreateSqlString1() throws Exception {
        // query with qualifier and ordering
        SelectQuery q = new SelectQuery(Artist.class, ExpressionFactory.likeExp(
                "artistName",
                "a%"));
        q.addOrdering("dateOfBirth", Ordering.ASC);

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {
                String generatedSql = transl.createSqlString();

                // do some simple assertions to make sure all parts are in
                assertNotNull(generatedSql);
                assertTrue(generatedSql.startsWith("SELECT "));
                assertTrue(generatedSql.indexOf(" FROM ") > 0);
                assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql
                        .indexOf(" FROM "));
                assertTrue(generatedSql.indexOf(" ORDER BY ") > generatedSql
                        .indexOf(" WHERE "));
            }
        };

        test.test(q);
    }

    /**
     * Tests query creation with "distinct" specified.
     */
    public void testCreateSqlString2() throws Exception {
        // query with "distinct" set
        SelectQuery q = new SelectQuery(Artist.class);
        q.setDistinct(true);

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {
                String generatedSql = transl.createSqlString();

                // do some simple assertions to make sure all parts are in
                assertNotNull(generatedSql);
                assertTrue(generatedSql.startsWith("SELECT DISTINCT"));
            }
        };

        test.test(q);
    }

    /**
     * Test aliases when the same table used in more then 1 relationship. Check
     * translation of relationship path "ArtistExhibit.toArtist.artistName" and
     * "ArtistExhibit.toExhibit.toGallery.paintingArray.toArtist.artistName".
     */
    public void testCreateSqlString5() throws Exception {
        // query with qualifier and ordering
        SelectQuery q = new SelectQuery(ArtistExhibit.class);
        q.setQualifier(ExpressionFactory.likeExp("toArtist.artistName", "a%"));
        q.andQualifier(ExpressionFactory.likeExp(
                "toExhibit.toGallery.paintingArray.toArtist.artistName",
                "a%"));

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {
                String generatedSql = transl.createSqlString();
                // logObj.warn("Query: " + generatedSql);

                // do some simple assertions to make sure all parts are in
                assertNotNull(generatedSql);
                assertTrue(generatedSql.startsWith("SELECT "));
                assertTrue(generatedSql.indexOf(" FROM ") > 0);
                assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql
                        .indexOf(" FROM "));

                // check that there are 2 distinct aliases for the ARTIST table
                int ind1 = generatedSql.indexOf("ARTIST t", generatedSql
                        .indexOf(" FROM "));
                assertTrue(ind1 > 0);

                int ind2 = generatedSql.indexOf("ARTIST t", ind1 + 1);
                assertTrue(ind2 > 0);

                assertTrue(generatedSql.charAt(ind1 + "ARTIST t".length()) != generatedSql
                        .charAt(ind2 + "ARTIST t".length()));
            }
        };

        test.test(q);

    }

    /**
     * Test aliases when the same table used in more then 1 relationship. Check
     * translation of relationship path "ArtistExhibit.toArtist.artistName" and
     * "ArtistExhibit.toArtist.paintingArray.paintingTitle".
     */
    public void testCreateSqlString6() throws Exception {
        // query with qualifier and ordering
        SelectQuery q = new SelectQuery(ArtistExhibit.class);
        q.setQualifier(ExpressionFactory.likeExp("toArtist.artistName", "a%"));
        q.andQualifier(ExpressionFactory.likeExp(
                "toArtist.paintingArray.paintingTitle",
                "p%"));

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {
                String generatedSql = transl.createSqlString();

                // do some simple assertions to make sure all parts are in
                assertNotNull(generatedSql);
                assertTrue(generatedSql.startsWith("SELECT "));
                assertTrue(generatedSql.indexOf(" FROM ") > 0);
                assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql
                        .indexOf(" FROM "));

                // check that there is only one distinct alias for the ARTIST table
                int ind1 = generatedSql.indexOf("ARTIST t", generatedSql
                        .indexOf(" FROM "));
                assertTrue(ind1 > 0);

                int ind2 = generatedSql.indexOf("ARTIST t", ind1 + 1);
                assertTrue(ind2 < 0);
            }
        };

        test.test(q);
    }

    /**
     * Test query when qualifying on the same attribute more than once. Check translation
     * "Artist.dateOfBirth > ? AND Artist.dateOfBirth < ?".
     */
    public void testCreateSqlString7() throws Exception {
        SelectQuery q = new SelectQuery(Artist.class);
        q.setQualifier(ExpressionFactory.greaterExp("dateOfBirth", new Date()));
        q.andQualifier(ExpressionFactory.lessExp("dateOfBirth", new Date()));

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {
                String generatedSql = transl.createSqlString();
                // logObj.warn("Query: " + generatedSql);

                // do some simple assertions to make sure all parts are in
                assertNotNull(generatedSql);
                assertTrue(generatedSql.startsWith("SELECT "));

                int i1 = generatedSql.indexOf(" FROM ");
                assertTrue(i1 > 0);

                int i2 = generatedSql.indexOf(" WHERE ");
                assertTrue(i2 > i1);

                int i3 = generatedSql.indexOf("DATE_OF_BIRTH", i2 + 1);
                assertTrue(i3 > i2);

                int i4 = generatedSql.indexOf("DATE_OF_BIRTH", i3 + 1);
                assertTrue("No second DOB comparison: " + i4 + ", " + i3, i4 > i3);
            }
        };

        test.test(q);
    }

    /**
     * Test query when qualifying on the same attribute accessed over relationship, more
     * than once. Check translation "Painting.toArtist.dateOfBirth > ? AND
     * Painting.toArtist.dateOfBirth < ?".
     */
    public void testCreateSqlString8() throws Exception {
        SelectQuery q = new SelectQuery();
        q.setRoot(Painting.class);
        q.setQualifier(ExpressionFactory.greaterExp("toArtist.dateOfBirth", new Date()));
        q.andQualifier(ExpressionFactory.lessExp("toArtist.dateOfBirth", new Date()));

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {
                String generatedSql = transl.createSqlString();

                // do some simple assertions to make sure all parts are in
                assertNotNull(generatedSql);
                assertTrue(generatedSql.startsWith("SELECT "));

                int i1 = generatedSql.indexOf(" FROM ");
                assertTrue(i1 > 0);

                int i2 = generatedSql.indexOf(" WHERE ");
                assertTrue(i2 > i1);

                int i3 = generatedSql.indexOf("DATE_OF_BIRTH", i2 + 1);
                assertTrue(i3 > i2);

                int i4 = generatedSql.indexOf("DATE_OF_BIRTH", i3 + 1);
                assertTrue("No second DOB comparison: " + i4 + ", " + i3, i4 > i3);
            }
        };

        test.test(q);
    }

    public void testCreateSqlString9() throws Exception {
        // query for a compound ObjEntity with qualifier
        SelectQuery q = new SelectQuery(CompoundPainting.class, ExpressionFactory
                .likeExp("artistName", "a%"));

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {

                String sql = transl.createSqlString();

                // do some simple assertions to make sure all parts are in
                assertNotNull(sql);
                assertTrue(sql.startsWith("SELECT "));

                int i1 = sql.indexOf(" FROM ");
                assertTrue(i1 > 0);

                int i2 = sql.indexOf("PAINTING");
                assertTrue(i2 > 0);

                int i3 = sql.indexOf("ARTIST");
                assertTrue(i3 > 0);

                int i4 = sql.indexOf("GALLERY");
                assertTrue(i4 > 0);

                int i5 = sql.indexOf("PAINTING_INFO");
                assertTrue(i5 > 0);

                int i6 = sql.indexOf("ARTIST_NAME");
                assertTrue(i6 > 0);

                int i7 = sql.indexOf("ESTIMATED_PRICE");
                assertTrue(i7 > 0);

                int i8 = sql.indexOf("GALLERY_NAME");
                assertTrue(i8 > 0);

                int i9 = sql.indexOf("PAINTING_TITLE");
                assertTrue(i9 > 0);

                int i10 = sql.indexOf("TEXT_REVIEW");
                assertTrue(i10 > 0);

                int i11 = sql.indexOf("PAINTING_ID");
                assertTrue(i11 > 0);

                int i12 = sql.indexOf("ARTIST_ID");
                assertTrue(i12 > 0);

                int i13 = sql.indexOf("GALLERY_ID");
                assertTrue(i13 > 0);
            }
        };

        test.test(q);
    }

    public void testCreateSqlString10() throws Exception {
        // query with to-many joint prefetches
        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {

                String sql = transl.createSqlString();
                assertNotNull(sql);
                assertTrue(sql.startsWith("SELECT "));

                int i1 = sql.indexOf("ARTIST_ID");
                assertTrue(sql, i1 > 0);

                int i2 = sql.indexOf("FROM");
                assertTrue(sql, i2 > 0);

                // make sure FK column is skipped
                int i3 = sql.indexOf("ARTIST_ID", i1 + 1);
                assertTrue(sql, i3 < 0 || i3 > i2);

                assertTrue(sql, sql.indexOf("PAINTING_ID") > 0);

                // assert we have one join
                assertEquals(1, transl.dbRelList.size());
            }
        };

        test.test(q);
    }

    public void testCreateSqlString11() throws Exception {
        // query with joint prefetches and other joins
        SelectQuery q = new SelectQuery(Artist.class, Expression
                .fromString("paintingArray.paintingTitle = 'a'"));
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {

                transl.createSqlString();

                // assert we only have one join
                assertEquals(1, transl.dbRelList.size());
            }
        };

        test.test(q);
    }

    public void testCreateSqlString12() throws Exception {
        // query with to-one joint prefetches
        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {

                String sql = transl.createSqlString();
                assertNotNull(sql);
                assertTrue(sql.startsWith("SELECT "));

                int i1 = sql.indexOf("ARTIST_ID");
                assertTrue(sql, i1 > 0);

                int i2 = sql.indexOf("FROM");
                assertTrue(sql, i2 > 0);

                // make sure FK column is skipped
                int i3 = sql.indexOf("ARTIST_ID", i1 + 1);
                assertTrue(sql, i3 < 0 || i3 > i2);

                assertTrue(sql, sql.indexOf("PAINTING_ID") > 0);

                // assert we have one join
                assertEquals(1, transl.dbRelList.size());
            }
        };

        test.test(q);
    }

    public void testCreateSqlString13() throws Exception {
        // query with invalid joint prefetches
        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch("invalid.invalid").setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {

                try {
                    transl.createSqlString();
                    fail("Invalid jointPrefetch must have thrown...");
                }
                catch (ExpressionException e) {
                    // expected
                }
            }
        };

        test.test(q);
    }

    /**
     * Tests columns generated for a simple object query.
     */
    public void testBuildResultColumns1() throws Exception {
        SelectQuery q = new SelectQuery(Painting.class);
        SelectTranslator tr = makeTranslator(q);

        List columns = tr.buildResultColumns();

        // all DbAttributes must be included
        DbEntity entity = getDbEntity("PAINTING");
        Iterator it = entity.getAttributes().iterator();
        while (it.hasNext()) {
            DbAttribute a = (DbAttribute) it.next();
            ColumnDescriptor c = new ColumnDescriptor(a, "t0");
            assertTrue("No descriptor for " + a + ", columns: " + columns, columns
                    .contains(c));
        }
    }

    /**
     * Tests columns generated for an object query with joint prefetch.
     */
    public void testBuildResultColumns2() throws Exception {
        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
        SelectTranslator tr = makeTranslator(q);

        List columns = tr.buildResultColumns();

        // assert root entity columns
        DbEntity entity = getDbEntity("PAINTING");
        Iterator it = entity.getAttributes().iterator();
        while (it.hasNext()) {
            DbAttribute a = (DbAttribute) it.next();
            ColumnDescriptor c = new ColumnDescriptor(a, "t0");
            assertTrue("No descriptor for " + a + ", columns: " + columns, columns
                    .contains(c));
        }

        // assert joined columns
        DbEntity joined = getDbEntity("ARTIST");
        Iterator itj = joined.getAttributes().iterator();
        while (itj.hasNext()) {
            DbAttribute a = (DbAttribute) itj.next();

            // skip ARTIST PK, it is joined from painting
            if (Artist.ARTIST_ID_PK_COLUMN.equals(a.getName())) {
                continue;
            }

            ColumnDescriptor c = new ColumnDescriptor(a, "t1");
            c.setLabel("toArtist." + a.getName());
            assertTrue("No descriptor for " + a + ", columns: " + columns, columns
                    .contains(c));
        }
    }

    SelectTranslator makeTranslator(Query q) throws Exception {

        SelectTranslator translator = new SelectTranslator();
        translator.setQuery(q);
        translator.setAdapter(getNode().getAdapter());
        translator.setEntityResolver(getNode().getEntityResolver());
        return translator;
    }

    /**
     * Helper class that serves as a template to streamline testing that requires an open
     * connection.
     */
    abstract class Template {

        void test(SelectQuery q) throws Exception {
            SelectTranslator transl = makeTranslator(q);

            Connection c = getConnection();
            try {

                transl.setConnection(c);
                test(transl);
            }
            finally {
                try {
                    c.close();
                }
                catch (SQLException ex) {

                }
            }
        }

        abstract void test(SelectTranslator transl) throws Exception;
    }
}
