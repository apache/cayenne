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
package org.apache.cayenne.query;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.XMLEncoder;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class EJBQLQueryTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private ServerRuntime runtime;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;
    private TableHelper tPainting;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE");
    }

    protected void createArtistsDataSet() throws Exception {
        tArtist.insert(33001, "a0");
        tArtist.insert(33002, "a1");
        tArtist.insert(33003, "a2");
        tArtist.insert(33004, "a3");
        tArtist.insert(33005, "a4");
    }

    protected void createPaintingsDataSet() throws Exception {
        tArtist.insert(33001, "a0");
        tArtist.insert(33002, "a1");
        tPainting.insert(33001, 33001, "title0");
        tPainting.insert(33002, 33002, "title1");
    }

    public void testParameters() {
        String ejbql = "select a FROM Artist a WHERE a.artistName = ?1 OR a.artistName = :name";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter(1, "X");
        query.setParameter("name", "Y");

        Map<String, Object> parameters = query.getNamedParameters();
        Map<Integer, Object> parameters1 = query.getPositionalParameters();
        assertEquals(1, parameters.size());
        assertEquals(1, parameters1.size());
        assertEquals("X", parameters1.get(new Integer(1)));
        assertEquals("Y", parameters.get("name"));
    }

    public void testCacheParameters() {
        String ejbql1 = "select a FROM Artist a WHERE a.artistName = ?1 OR a.artistName = :name";
        EJBQLQuery q1 = new EJBQLQuery(ejbql1);
        q1.setParameter(1, "X");
        q1.setParameter("name", "Y");
        q1.setFetchOffset(1);
        q1.setFetchLimit(5);
        q1.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        String ejbql2 = "select a FROM Artist a WHERE a.artistName = ?1 OR a.artistName = :name";
        EJBQLQuery q2 = new EJBQLQuery(ejbql2);
        q2.setParameter(1, "X");
        q2.setParameter("name", "Y");
        q2.setFetchOffset(1);
        q2.setFetchLimit(5);
        q2.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();

        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2
                .getMetaData(resolver)
                .getCacheKey());
    }

    public void testCacheStrategy() throws Exception {

        // insertValue();
        createArtistsDataSet();

        final String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        final List<Artist> artist1 = context.performQuery(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                List<Artist> artist2;
                EJBQLQuery query1 = new EJBQLQuery(ejbql);
                query1.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
                artist2 = context.performQuery(query1);

                assertEquals(artist1.get(0).getArtistName(), artist2
                        .get(0)
                        .getArtistName());
            }
        });

    }

    public void testDataRows() throws Exception {

        // insertValue();
        createArtistsDataSet();

        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setFetchingDataRows(true);
        List<?> artists = context.performQuery(query);

        DataRow row = (DataRow) artists.get(0);
        String artistName = (String) row.get("ARTIST_NAME");

        Artist artist = (Artist) context.objectFromDataRow("Artist", row);
        assertEquals(artistName, artist.getArtistName());
    }

    public void testGetExpression() {
        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        EJBQLCompiledExpression parsed = query.getExpression(runtime
                .getDataDomain()
                .getEntityResolver());
        assertNotNull(parsed);
        assertEquals(ejbql, parsed.getSource());
    }

    public void testGetName() {
        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        assertNull(query.getName());
        query.setName("XYZ");
        assertEquals("XYZ", query.getName());
    }

    public void testUniqueKeyEntity() {
        // insertValue();
        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();
        String ejbql = "select a FROM Artist a";

        EJBQLQuery q1 = new EJBQLQuery(ejbql);
        q1.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        EJBQLQuery q2 = new EJBQLQuery(ejbql);
        q2.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2
                .getMetaData(resolver)
                .getCacheKey());
    }

    public void testGetMetadata() {

        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();
        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        QueryMetadata md = query.getMetaData(resolver);

        assertNotNull(md);

        assertNotNull(md.getClassDescriptor());
        assertSame(resolver.getClassDescriptor("Artist"), md.getClassDescriptor());

        assertNotNull(md.getObjEntity());
        assertSame(resolver.getObjEntity("Artist"), md.getObjEntity());

        assertFalse(md.isFetchingDataRows());
        assertTrue(md.isRefreshingObjects());
        assertEquals(QueryCacheStrategy.NO_CACHE, md.getCacheStrategy());
    }

    public void testSelectRelationship() throws Exception {

        // insertPaintValue();
        createPaintingsDataSet();

        String ejbql = "SELECT p.toArtist FROM Painting p";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> result = context.performQuery(query);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(Artist.class, result.get(0).getClass());

        String ejbql2 = "SELECT p.toArtist, p FROM Painting p";
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List<?> result2 = context.performQuery(query2);

        assertNotNull(result2);
        assertEquals(2, result2.size());
        assertEquals(2, ((Object[]) result2.get(0)).length);

        assertEquals(Artist.class, ((Object[]) result2.get(0))[0].getClass());
        assertEquals(Painting.class, ((Object[]) result2.get(0))[1].getClass());

        String ejbql3 = "SELECT p.toArtist, p.paintingTitle FROM Painting p";
        EJBQLQuery query3 = new EJBQLQuery(ejbql3);

        List<?> result3 = context.performQuery(query3);

        assertNotNull(result3);
        assertEquals(2, result3.size());
        assertEquals(2, ((Object[]) result3.get(0)).length);

        assertEquals(Artist.class, ((Object[]) result3.get(0))[0].getClass());
        assertEquals(String.class, ((Object[]) result3.get(0))[1].getClass());
    }

    public void testEncodeAsXML() {

        String ejbql = "select a FROM Artist a";
        String name = "Test";

        StringWriter w = new StringWriter();
        XMLEncoder e = new XMLEncoder(new PrintWriter(w));

        String separator = System.getProperty("line.separator");

        StringBuffer s = new StringBuffer("<query name=\"");
        s.append(name);
        s.append("\" factory=\"");
        s.append("org.apache.cayenne.map.EjbqlBuilder");
        s.append("\">");
        s.append(separator);

        EJBQLQuery query = new EJBQLQuery(ejbql);

        if (query.getEjbqlStatement() != null) {
            s.append("<ejbql><![CDATA[");
            s.append(query.getEjbqlStatement());
            s.append("]]></ejbql>");
        }
        s.append(separator);
        s.append("</query>");
        s.append(separator);
        query.setName(name);
        query.encodeAsXML(e);

        assertEquals(w.getBuffer().toString(), s.toString());
    }

    public void testNullParameter() {
        EJBQLQuery query = new EJBQLQuery("select p from Painting p WHERE p.toArtist=:x");
        query.setParameter("x", null);
        context.performQuery(query);
    }

    public void testNullNotEqualsParameter() {
        EJBQLQuery query = new EJBQLQuery("select p from Painting p WHERE p.toArtist<>:x");
        query.setParameter("x", null);
        context.performQuery(query);
    }

    public void testNullPositionalParameter() {
        EJBQLQuery query = new EJBQLQuery("select p from Painting p WHERE p.toArtist=?1");
        query.setParameter(1, null);
        context.performQuery(query);
    }

    public void testNullAndNotNullParameter() {
        EJBQLQuery query = new EJBQLQuery(
                "select p from Painting p WHERE p.toArtist=:x OR p.toArtist.artistName=:b");
        query.setParameter("x", null);
        query.setParameter("b", "Y");
        context.performQuery(query);
    }

    public void testJoinToJoined() {
        EJBQLQuery query = new EJBQLQuery(
                "select g from Gallery g inner join g.paintingArray p where p.toArtist.artistName like '%a%'");
        context.performQuery(query);
    }

    public void testJoinAndCount() {
        EJBQLQuery query = new EJBQLQuery(
                "select count(p) from Painting p where p.toGallery.galleryName LIKE '%a%' AND ("
                        + "p.paintingTitle like '%a%' or "
                        + "p.toArtist.artistName like '%a%'"
                        + ")");
        context.performQuery(query);
    }

    // SELECT COUNT(p) from Product p where p.vsCatalog.id = 1 and
    // (
    // p.displayName like '%rimadyl%'
    // or p.manufacturer.name like '%rimadyl%'
    // or p.description like '%rimadyl%'
    // or p.longdescription like '%rimadyl%'
    // or p.longdescription2 like '%rimadyl%'
    // or p.manufacturerPartNumber like '%rimadyl%'
    // or p.partNumber like '%rimadyl%'
    // )

    public void testRelationshipWhereClause() throws Exception {
        Artist a = context.newObject(Artist.class);
        a.setArtistName("a");
        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("p");
        p.setToArtist(a);
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery("select p from Painting p where p.toArtist=:a");
        query.setParameter("a", a);

        List<Painting> paintings = context.performQuery(query);
        assertEquals(1, paintings.size());
        assertSame(p, paintings.get(0));
    }

    public void testRelationshipWhereClause2() throws Exception {
        Expression exp = ExpressionFactory.matchExp(Painting.TO_GALLERY_PROPERTY, null);
        EJBQLQuery query = new EJBQLQuery("select p.toArtist from Painting p where "
                + exp.toEJBQL("p"));

        context.performQuery(query);
    }

    public void testOrBrackets() throws Exception {
        Artist a = context.newObject(Artist.class);
        a.setArtistName("testOrBrackets");
        context.commitChanges();

        // this query is equivalent to (false and (false or true)) and
        // should always return 0 rows
        EJBQLQuery query = new EJBQLQuery("select a from Artist a "
                + "where a.artistName <> a.artistName and "
                + "(a.artistName <> a.artistName or a.artistName = a.artistName)");
        assertEquals(context.performQuery(query).size(), 0);

        // on the other hand, the following is equivalent to (false and false) or true)
        // and
        // should return >0 rows
        query = new EJBQLQuery("select a from Artist a "
                + "where a.artistName <> a.artistName and "
                + "a.artistName <> a.artistName or a.artistName = a.artistName");
        assertTrue(context.performQuery(query).size() > 0);

        // checking brackets around not
        query = new EJBQLQuery("select a from Artist a "
                + "where not(a.artistName <> a.artistName and "
                + "a.artistName <> a.artistName or a.artistName = a.artistName)");
        assertEquals(context.performQuery(query).size(), 0);

        // not is first to process
        query = new EJBQLQuery("select a from Artist a "
                + "where not a.artistName <> a.artistName or "
                + "a.artistName = a.artistName");
        assertTrue(context.performQuery(query).size() > 0);
    }
}
