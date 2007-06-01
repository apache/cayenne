/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access.trans;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistAssets;
import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.ArtistPaintingCounts;
import org.objectstyle.art.CompoundPainting;
import org.objectstyle.art.Painting;
import org.objectstyle.art.SubPainting;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class SelectTranslatorTst extends CayenneTestCase {
    protected SelectQuery q;
    protected DbEntity artistEnt;

    protected void setUp() throws Exception {
        q = new SelectQuery();
        artistEnt = getDbEntity("ARTIST");
    }

    private SelectTranslator buildTranslator(Connection con) throws Exception {
        SelectTranslator transl =
            (SelectTranslator) getNode().getAdapter().getQueryTranslator(q);
        transl.setEngine(getNode());
        transl.setCon(con);

        return transl;
    }

    /**
     * Tests query creation with qualifier and ordering.
     */
    public void testCreateSqlString1() throws Exception {
        Connection con = getConnection();

        try {
            // query with qualifier and ordering
            q.setRoot(Artist.class);
            q.setQualifier(ExpressionFactory.likeExp("artistName", "a%"));
            q.addOrdering("dateOfBirth", Ordering.ASC);

            String generatedSql = buildTranslator(con).createSqlString();

            // do some simple assertions to make sure all parts are in
            assertNotNull(generatedSql);
            assertTrue(generatedSql.startsWith("SELECT "));
            assertTrue(generatedSql.indexOf(" FROM ") > 0);
            assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql.indexOf(" FROM "));
            assertTrue(
                generatedSql.indexOf(" ORDER BY ") > generatedSql.indexOf(" WHERE "));
        }
        finally {
            con.close();
        }
    }

    /**
     * Tests query creation with "distinct" specified.
     */
    public void testCreateSqlString2() throws java.lang.Exception {
        Connection con = getConnection();
        try {
            // query with "distinct" set
            q.setRoot(Artist.class);
            q.setDistinct(true);

            String generatedSql = buildTranslator(con).createSqlString();

            // do some simple assertions to make sure all parts are in
            assertNotNull(generatedSql);
            assertTrue(generatedSql.startsWith("SELECT DISTINCT"));
        }
        finally {
            con.close();
        }
    }

    /**
     * Tests query creation with relationship from derived entity.
     */
    public void testCreateSqlString3() throws Exception {
        ObjectId id = new ObjectId(Artist.class, "ARTIST_ID", 35);
        Artist a1 = (Artist) createDataContext().registeredObject(id);
        Connection con = getConnection();

        try {
            // query with qualifier and ordering
            q.setRoot(ArtistAssets.class);
            q.setQualifier(ExpressionFactory.matchExp("toArtist", a1));

            String sql = buildTranslator(con).createSqlString();

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
        finally {
            con.close();
        }
    }

    /**
     * Tests query creation with relationship from derived entity.
     */
    public void testCreateSqlString4() throws Exception {
        Connection con = getConnection();

        try {
            // query with qualifier and ordering
            q.setRoot(ArtistAssets.class);
            q.setParentObjEntityName("Painting");
            q.setParentQualifier(
                ExpressionFactory.matchExp("toArtist.artistName", "abc"));
            q.andParentQualifier(
                ExpressionFactory.greaterOrEqualExp("estimatedPrice", new BigDecimal(1)));
            q.setQualifier(
                ExpressionFactory.matchExp("estimatedPrice", new BigDecimal(3)));

            String sql = buildTranslator(con).createSqlString();

            // do some simple assertions to make sure all parts are in
            assertNotNull(sql);
            assertTrue(sql.startsWith("SELECT "));
            assertTrue(sql.indexOf(" FROM ") > 0);

            // no WHERE clause
            assertTrue("WHERE clause is expected: " + sql, sql.indexOf(" WHERE ") > 0);
            assertTrue(
                "WHERE clause must have estimated price: " + sql,
                sql.indexOf("ESTIMATED_PRICE >=") > 0);

            assertTrue(
                "GROUP BY clause is expected:" + sql,
                sql.indexOf(" GROUP BY ") > 0);
            assertTrue("HAVING clause is expected", sql.indexOf(" HAVING ") > 0);
            assertTrue(sql.indexOf("ARTIST_ID =") > 0);
            assertTrue(
                "Relationship join must be in WHERE: " + sql,
                sql.indexOf("ARTIST_ID =") > sql.indexOf(" WHERE "));
            assertTrue(
                "Relationship join must be in WHERE: " + sql,
                sql.indexOf("ARTIST_ID =") < sql.indexOf(" GROUP BY "));
            assertTrue(
                "Qualifier for related entity must be in WHERE: " + sql,
                sql.indexOf("ARTIST_NAME") > sql.indexOf(" WHERE "));
            assertTrue(
                "Qualifier for related entity must be in WHERE: " + sql,
                sql.indexOf("ARTIST_NAME") < sql.indexOf(" GROUP BY "));
            assertTrue(
                "WHERE clause must have estimated price: " + sql,
                sql.indexOf("ESTIMATED_PRICE >=") < sql.indexOf(" GROUP BY "));
        }
        finally {
            con.close();
        }
    }

    /**
     * Test aliases when the same table used in more then 1 relationship.
     * Check translation of relationship path "ArtistExhibit.toArtist.artistName"
     * and "ArtistExhibit.toExhibit.toGallery.paintingArray.toArtist.artistName".
     */
    public void testCreateSqlString5() throws Exception {
        Connection con = getConnection();

        try {
            // query with qualifier and ordering
            q.setRoot(ArtistExhibit.class);
            q.setQualifier(ExpressionFactory.likeExp("toArtist.artistName", "a%"));
            q.andQualifier(
                ExpressionFactory.likeExp(
                    "toExhibit.toGallery.paintingArray.toArtist.artistName",
                    "a%"));

            SelectTranslator transl = buildTranslator(con);
            String generatedSql = transl.createSqlString();
            // logObj.warn("Query: " + generatedSql);

            // do some simple assertions to make sure all parts are in
            assertNotNull(generatedSql);
            assertTrue(generatedSql.startsWith("SELECT "));
            assertTrue(generatedSql.indexOf(" FROM ") > 0);
            assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql.indexOf(" FROM "));

            // check that there are 2 distinct aliases for the ARTIST table
            int ind1 = generatedSql.indexOf("ARTIST t", generatedSql.indexOf(" FROM "));
            assertTrue(ind1 > 0);

            int ind2 = generatedSql.indexOf("ARTIST t", ind1 + 1);
            assertTrue(ind2 > 0);

            assertTrue(
                generatedSql.charAt(ind1 + "ARTIST t".length())
                    != generatedSql.charAt(ind2 + "ARTIST t".length()));

        }
        finally {
            con.close();
        }
    }

    /**
     * Test aliases when the same table used in more then 1 relationship.
     * Check translation of relationship path "ArtistExhibit.toArtist.artistName"
     * and "ArtistExhibit.toArtist.paintingArray.paintingTitle".
     */
    public void testCreateSqlString6() throws Exception {
        Connection con = getConnection();

        try {
            // query with qualifier and ordering
            q.setRoot(ArtistExhibit.class);
            q.setQualifier(ExpressionFactory.likeExp("toArtist.artistName", "a%"));
            q.andQualifier(
                ExpressionFactory.likeExp("toArtist.paintingArray.paintingTitle", "p%"));

            SelectTranslator transl = buildTranslator(con);
            String generatedSql = transl.createSqlString();
            // logObj.warn("Query: " + generatedSql);

            // do some simple assertions to make sure all parts are in
            assertNotNull(generatedSql);
            assertTrue(generatedSql.startsWith("SELECT "));
            assertTrue(generatedSql.indexOf(" FROM ") > 0);
            assertTrue(generatedSql.indexOf(" WHERE ") > generatedSql.indexOf(" FROM "));

            // check that there is only one distinct alias for the ARTIST table
            int ind1 = generatedSql.indexOf("ARTIST t", generatedSql.indexOf(" FROM "));
            assertTrue(ind1 > 0);

            int ind2 = generatedSql.indexOf("ARTIST t", ind1 + 1);
            assertTrue(ind2 < 0);
        }
        finally {
            con.close();
        }
    }

    /**
     * Test query when qualifying on the same attribute more than once.
     * Check translation "Artist.dateOfBirth > ? AND Artist.dateOfBirth < ?".
     */
    public void testCreateSqlString7() throws Exception {
        Connection con = getConnection();

        try {
            // query with qualifier and ordering
            q.setRoot(Artist.class);
            q.setQualifier(ExpressionFactory.greaterExp("dateOfBirth", new Date()));
            q.andQualifier(ExpressionFactory.lessExp("dateOfBirth", new Date()));

            SelectTranslator transl = buildTranslator(con);
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
        finally {
            con.close();
        }
    }

    /**
      * Test query when qualifying on the same attribute accessed over
      * relationship, more than once.
      * Check translation "Painting.toArtist.dateOfBirth > ? AND Painting.toArtist.dateOfBirth < ?".
      */
    public void testCreateSqlString8() throws Exception {
        Connection con = getConnection();

        try {
            // query with qualifier and ordering
            q.setRoot(Painting.class);
            q.setQualifier(
                ExpressionFactory.greaterExp("toArtist.dateOfBirth", new Date()));
            q.andQualifier(ExpressionFactory.lessExp("toArtist.dateOfBirth", new Date()));

            SelectTranslator transl = buildTranslator(con);
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
        finally {
            con.close();
        }
    }

    public void testCreateSqlString9() throws Exception {
        Connection con = getConnection();

        try {
            // query for a compound ObjEntity with qualifier
            q.setRoot(CompoundPainting.class);
            q.setQualifier(ExpressionFactory.likeExp("artistName", "a%"));

            String generatedSql = buildTranslator(con).createSqlString();

            // do some simple assertions to make sure all parts are in
            assertNotNull(generatedSql);
            assertTrue(generatedSql.startsWith("SELECT "));

            int i1 = generatedSql.indexOf(" FROM ");
            assertTrue(i1 > 0);

            int i2 = generatedSql.indexOf("PAINTING");
            assertTrue(i2 > 0);

            int i3 = generatedSql.indexOf("ARTIST");
            assertTrue(i3 > 0);

            int i4 = generatedSql.indexOf("GALLERY");
            assertTrue(i4 > 0);

            int i5 = generatedSql.indexOf("PAINTING_INFO");
            assertTrue(i5 > 0);

            int i6 = generatedSql.indexOf("ARTIST_NAME");
            assertTrue(i6 > 0);

            int i7 = generatedSql.indexOf("ESTIMATED_PRICE");
            assertTrue(i7 > 0);

            int i8 = generatedSql.indexOf("GALLERY_NAME");
            assertTrue(i8 > 0);

            int i9 = generatedSql.indexOf("PAINTING_TITLE");
            assertTrue(i9 > 0);

            int i10 = generatedSql.indexOf("TEXT_REVIEW");
            assertTrue(i10 > 0);

            int i11 = generatedSql.indexOf("PAINTING_ID");
            assertTrue(i11 > 0);

            int i12 = generatedSql.indexOf("ARTIST_ID");
            assertTrue(i12 > 0);

            int i13 = generatedSql.indexOf("GALLERY_ID");
            assertTrue(i13 > 0);
        }
        finally {
            con.close();
        }
    }

    public void testBuildColumnList1() throws Exception {
        Connection con = getConnection();

        try {
            // configure query with entity that maps one-to-one to DbEntity
            q.setRoot(Artist.class);
            SelectTranslator transl = buildTranslator(con);
            transl.createSqlString();

            List columns = transl.getColumns();
            Collection dbAttrs = artistEnt.getAttributes();

            assertEquals(dbAttrs.size(), columns.size());
            Iterator it = dbAttrs.iterator();
            while (it.hasNext()) {
                assertTrue(columns.contains(it.next()));
            }

        }
        finally {
            con.close();
        }
    }

    public void testBuildColumnList2() throws Exception {
        Connection con = getConnection();

        try {
            // configure query with custom attributes
            q.setRoot(Artist.class);
            q.addCustomDbAttribute("ARTIST_ID");

            SelectTranslator transl = buildTranslator(con);
            transl.createSqlString();

            List columns = transl.getColumns();
            Object[] dbAttrs = new Object[] { artistEnt.getAttribute("ARTIST_ID")};

            assertEquals(dbAttrs.length, columns.size());
            for (int i = 0; i < dbAttrs.length; i++) {
                assertTrue(columns.contains(dbAttrs[i]));
            }

        }
        finally {
            con.close();
        }
    }

    public void testBuildColumnList3() throws Exception {
        Connection con = getConnection();

        try {
            // configure query with entity that maps to a subset of DbEntity
            q.setRoot(SubPainting.class);
            SelectTranslator transl = buildTranslator(con);
            transl.createSqlString();

            List columns = transl.getColumns();

            // assert that the number of attributes in the query is right
            // 1 (obj attr) + 1 (pk) = 2
            assertEquals(2, columns.size());

        }
        finally {
            con.close();
        }
    }

    public void testBuildColumnList4() throws Exception {
        Connection con = getConnection();

        try {
            // configure query with derived entity that maps to a subset of DbEntity
            q.setRoot(ArtistPaintingCounts.class);
            SelectTranslator transl = buildTranslator(con);
            transl.createSqlString();

            List columns = transl.getColumns();

            // assert that the number of attributes in the query is right
            // 1 (obj attr) + 1 (pk) = 2
            assertEquals(2, columns.size());

        }
        finally {
            con.close();
        }
    }
}