/*
 * ==================================================================== The ObjectStyle
 * Group Software License, version 1.1 ObjectStyle Group - http://objectstyle.org/
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors of the
 * software. All rights reserved. Redistribution and use in source and binary forms, with
 * or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must
 * reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. The end-user documentation included with the redistribution, if any, must include
 * the following acknowlegement: "This product includes software developed by independent
 * contributors and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and wherever
 * such third-party acknowlegements normally appear. 4. The names "ObjectStyle Group" and
 * "Cayenne" must not be used to endorse or promote products derived from this software
 * without prior written permission. For written permission, email "andrus at objectstyle
 * dot org". 5. Products derived from this software may not be called "ObjectStyle" or
 * "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their names without prior
 * written permission. THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE OBJECTSTYLE
 * GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ==================================================================== This software
 * consists of voluntary contributions made by many individuals and hosted on ObjectStyle
 * Group web site. For more information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access.trans;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistAssets;
import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.CompoundPainting;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.jdbc.ColumnDescriptor;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionException;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class SelectTranslatorTst extends CayenneTestCase {

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
     * Tests query creation with relationship from derived entity.
     */
    public void testCreateSqlString3() throws Exception {
        ObjectId id = new ObjectId("Artist", "ARTIST_ID", 35);
        Artist a1 = (Artist) createDataContext().localObject(id, null);

        // query with qualifier and ordering
        SelectQuery q = new SelectQuery(ArtistAssets.class, ExpressionFactory.matchExp(
                "toArtist",
                a1));

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {
                String sql = transl.createSqlString();

                // do some simple assertions to make sure all parts are in
                assertNotNull(sql);
                assertTrue(sql.startsWith("SELECT "));
                assertTrue(sql.indexOf(" FROM ") > 0);

                // no WHERE clause
                assertTrue(sql.indexOf(" WHERE ") < 0);

                assertTrue(sql.indexOf(" GROUP BY ") > 0);
                assertTrue(sql.indexOf("ARTIST_ID =") > 0);
                assertTrue(sql.indexOf("ARTIST_ID =") > sql.indexOf(" GROUP BY "));
            }
        };

        test.test(q);
    }

    /**
     * Tests query creation with relationship from derived entity.
     */
    public void testCreateSqlString4() throws Exception {
        // query with qualifier and ordering
        SelectQuery q = new SelectQuery(ArtistAssets.class);
        q.setParentObjEntityName("Painting");
        q.setParentQualifier(ExpressionFactory.matchExp("toArtist.artistName", "abc"));
        q.andParentQualifier(ExpressionFactory.greaterOrEqualExp(
                "estimatedPrice",
                new BigDecimal(1)));
        q.setQualifier(ExpressionFactory.matchExp("estimatedPrice", new BigDecimal(3)));

        Template test = new Template() {

            void test(SelectTranslator transl) throws Exception {
                String sql = transl.createSqlString();

                // do some simple assertions to make sure all parts are in
                assertNotNull(sql);
                assertTrue(sql.startsWith("SELECT "));
                assertTrue(sql.indexOf(" FROM ") > 0);

                // no WHERE clause
                assertTrue("WHERE clause is expected: " + sql, sql.indexOf(" WHERE ") > 0);
                assertTrue("WHERE clause must have estimated price: " + sql, sql
                        .indexOf("ESTIMATED_PRICE >=") > 0);

                assertTrue("GROUP BY clause is expected:" + sql, sql
                        .indexOf(" GROUP BY ") > 0);
                assertTrue("HAVING clause is expected", sql.indexOf(" HAVING ") > 0);
                assertTrue(sql.indexOf("ARTIST_ID =") > 0);
                assertTrue("Relationship join must be in WHERE: " + sql, sql
                        .indexOf("ARTIST_ID =") > sql.indexOf(" WHERE "));
                assertTrue("Relationship join must be in WHERE: " + sql, sql
                        .indexOf("ARTIST_ID =") < sql.indexOf(" GROUP BY "));
                assertTrue("Qualifier for related entity must be in WHERE: " + sql, sql
                        .indexOf("ARTIST_NAME") > sql.indexOf(" WHERE "));
                assertTrue("Qualifier for related entity must be in WHERE: " + sql, sql
                        .indexOf("ARTIST_NAME") < sql.indexOf(" GROUP BY "));
                assertTrue("WHERE clause must have estimated price: " + sql, sql
                        .indexOf("ESTIMATED_PRICE >=") < sql.indexOf(" GROUP BY "));
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
        // TODO: this API is deprecated, need to refactor test cases...
        SelectTranslator transl = (SelectTranslator) getNode()
                .getAdapter()
                .getQueryTranslator(q);
        transl.setEntityResolver(getNode().getEntityResolver());
        return transl;
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