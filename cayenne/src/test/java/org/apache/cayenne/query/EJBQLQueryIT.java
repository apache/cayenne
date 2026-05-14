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
package org.apache.cayenne.query;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.EmptyConfigurationNodeVisitor;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.EJBQLQueryDescriptor;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EJBQLQueryIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;
    private CayenneRuntime runtime;

    private TableHelper tArtist;
    private TableHelper tPainting;

    @BeforeEach
    public void setUp() throws Exception {
        context = env.context();
        runtime = env.runtime();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");

        tPainting = env.table("PAINTING", "PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE");
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
        tPainting.insert(33003, 33002, "%%?_title%%_");
    }

    @Test
    public void parameters() {
        String ejbql = "select a FROM Artist a WHERE a.artistName = ?1 OR a.artistName = :name";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter(1, "X");
        query.setParameter("name", "Y");

        Map<String, Object> parameters = query.getNamedParameters();
        Map<Integer, Object> parameters1 = query.getPositionalParameters();
        assertEquals(1, parameters.size());
        assertEquals(1, parameters1.size());
        assertEquals("X", parameters1.get(1));
        assertEquals("Y", parameters.get("name"));
    }

    @Test
    public void cacheParameters() {
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

    @Test
    public void cacheStrategy() throws Exception {

        // insertValue();
        createArtistsDataSet();

        final String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        final List<Artist> artist1 = context.performQuery(query);

        env.runWithQueriesBlocked(() -> {
            List<Artist> artist2;
            EJBQLQuery query1 = new EJBQLQuery(ejbql);
            query1.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
            artist2 = context.performQuery(query1);

            assertEquals(artist1.get(0).getArtistName(), artist2
                    .get(0)
                    .getArtistName());
        });

    }

    @Test
    public void dataRows() throws Exception {

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

    @Test
    public void getExpression() {
        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        EJBQLCompiledExpression parsed = query.getExpression(runtime
                .getDataDomain()
                .getEntityResolver());
        assertNotNull(parsed);
        assertEquals(ejbql, parsed.getSource());
    }

    /**
     * <p>If an expression has an 'entity variable' used in the SELECT clause then there should be a
     * corresponding definition for the 'entity variable' in the FROM clause.  This did, at some
     * point throw an NPE.</p>
     */

    @Test
    public void missingEntityBeanVariable() {
        String ejbql = "SELECT b FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        EJBQLException e = assertThrows(EJBQLException.class, () -> context.performQuery(query));
        assertEquals("the entity variable 'b' does not refer to any entity in the FROM clause", e.getUnlabeledMessage());
    }

    @Test
    public void uniqueKeyEntity() {
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

    @Test
    public void getMetadata() {

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

    @Test
    public void selectRelationship() throws Exception {

        // insertPaintValue();
        createPaintingsDataSet();

        String ejbql = "SELECT p.toArtist FROM Painting p";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> result = context.performQuery(query);

        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals(Artist.class, result.get(0).getClass());

        String ejbql2 = "SELECT p.toArtist, p FROM Painting p";
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List<?> result2 = context.performQuery(query2);

        assertNotNull(result2);
        assertEquals(3, result2.size());
        assertEquals(2, ((Object[]) result2.get(0)).length);

        assertEquals(Artist.class, ((Object[]) result2.get(0))[0].getClass());
        assertEquals(Painting.class, ((Object[]) result2.get(0))[1].getClass());

        String ejbql3 = "SELECT p.toArtist, p.paintingTitle FROM Painting p";
        EJBQLQuery query3 = new EJBQLQuery(ejbql3);

        List<?> result3 = context.performQuery(query3);

        assertNotNull(result3);
        assertEquals(3, result3.size());
        assertEquals(2, ((Object[]) result3.get(0)).length);

        assertEquals(Artist.class, ((Object[]) result3.get(0))[0].getClass());
        assertEquals(String.class, ((Object[]) result3.get(0))[1].getClass());
    }

    @Test
    public void encodeAsXML() {

        String ejbql = "select a FROM Artist a";
        String name = "Test";

        StringWriter w = new StringWriter();
        XMLEncoder e = new XMLEncoder(new PrintWriter(w));

        String separator = System.getProperty("line.separator");
        String s = "<query name=\"" + name + "\" type=\"EJBQLQuery\">" + separator +
                "<ejbql><![CDATA[" + ejbql + "]]></ejbql>" + separator +
                "</query>" + separator;

        EJBQLQueryDescriptor descriptor = new EJBQLQueryDescriptor();
        descriptor.setEjbql(ejbql);
        descriptor.setName(name);
        descriptor.encodeAsXML(e, new EmptyConfigurationNodeVisitor());

        assertEquals(w.getBuffer().toString(), s);
    }

    @Test
    public void inWithMultipleStringPositionalParameters_withBrackets() throws Exception {
        createPaintingsDataSet();
        EJBQLQuery query = new EJBQLQuery("select p from Painting p where p.paintingTitle in (?1,?2,?3)");
        query.setParameter(1,"title0");
        query.setParameter(2,"title1");
        query.setParameter(3,"title2");
        List<Painting> paintings = context.performQuery(query);
        assertEquals(2, paintings.size());
    }

    @Test
    public void inWithSingleStringPositionalParameter_withoutBrackets() throws Exception {
        createPaintingsDataSet();
        EJBQLQuery query = new EJBQLQuery("select p from Painting p where p.paintingTitle in ?1");
        query.setParameter(1,"title0");
        List<Painting> paintings = context.performQuery(query);
        assertEquals(1, paintings.size());
    }

    @Test
    public void inWithSingleCollectionNamedParameter_withoutBrackets() throws Exception {
        createPaintingsDataSet();
        EJBQLQuery query = new EJBQLQuery("select p from Painting p where p.toArtist in :artists");
        query.setParameter("artists", ObjectSelect.query(Artist.class).select(context));
        List<Painting> paintings = context.performQuery(query);
        assertEquals(3, paintings.size());
    }

    @Test
    public void inWithSingleCollectionPositionalParameter_withoutBrackets() throws Exception {
        createPaintingsDataSet();
        EJBQLQuery query = new EJBQLQuery("select p from Painting p where p.toArtist in ?1");
        query.setParameter(1, ObjectSelect.query(Artist.class).select(context));
        List<Painting> paintings = context.performQuery(query);
        assertEquals(3, paintings.size());
    }

    @Test
    public void inWithSingleCollectionNamedParameter_withBrackets() throws Exception {
        createPaintingsDataSet();
        EJBQLQuery query = new EJBQLQuery("select p from Painting p where p.toArtist in (:artists)");
        query.setParameter("artists", ObjectSelect.query(Artist.class).select(context));
        List<Painting> paintings = context.performQuery(query);
        assertEquals(3, paintings.size());
    }

    @Test
    public void inWithSingleCollectionPositionalParameter_withBrackets() throws Exception {
        createPaintingsDataSet();
        EJBQLQuery query = new EJBQLQuery("select p from Painting p where p.toArtist in (?1)");
        query.setParameter(1, ObjectSelect.query(Artist.class).select(context));
        List<Painting> paintings = context.performQuery(query);
        assertEquals(3, paintings.size());
    }

    @Test
    public void nullParameter() {
        EJBQLQuery query = new EJBQLQuery("select p from Painting p WHERE p.toArtist=:x");
        query.setParameter("x", null);
        context.performQuery(query);
    }

    @Test
    public void nullNotEqualsParameter() {
        EJBQLQuery query = new EJBQLQuery("select p from Painting p WHERE p.toArtist<>:x");
        query.setParameter("x", null);
        context.performQuery(query);
    }

    @Test
    public void nullPositionalParameter() {
        EJBQLQuery query = new EJBQLQuery("select p from Painting p WHERE p.toArtist=?1");
        query.setParameter(1, null);
        context.performQuery(query);
    }

    @Test
    public void nullAndNotNullParameter() {
        EJBQLQuery query = new EJBQLQuery(
                "select p from Painting p WHERE p.toArtist=:x OR p.toArtist.artistName=:b");
        query.setParameter("x", null);
        query.setParameter("b", "Y");
        context.performQuery(query);
    }

    @Test
    public void likeWithExplicitEscape() throws Exception {
        createPaintingsDataSet();
        EJBQLQuery query = new EJBQLQuery("SELECT p FROM Painting p WHERE p.paintingTitle LIKE '|%|%?|_title|%|%|_' ESCAPE '|'");
        List<Painting> paintings = context.performQuery(query);
        assertEquals(1, paintings.size());
        assertEquals("%%?_title%%_", paintings.get(0).getPaintingTitle());
    }

    @Test
    public void joinToJoined() {
        EJBQLQuery query = new EJBQLQuery(
                "select g from Gallery g inner join g.paintingArray p where p.toArtist.artistName like '%a%'");
        context.performQuery(query);
    }

    @Test
    public void joinAndCount() {
        EJBQLQuery query = new EJBQLQuery(
                "select count(p) from Painting p where p.toGallery.galleryName LIKE '%a%' AND ("
                        + "p.paintingTitle like '%a%' or "
                        + "p.toArtist.artistName like '%a%'"
                        + ")");
        context.performQuery(query);
    }

    @Test
    public void relationshipWhereClause() throws Exception {
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

    @Test
    public void relationshipWhereClause2() {
        Expression exp = Painting.TO_GALLERY.isNull();
        EJBQLQuery query = new EJBQLQuery("select p.toArtist from Painting p where "
                + exp.toEJBQL("p"));

        context.performQuery(query);
    }

    @Test
    public void orBrackets() {
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

	@Test
	public void orderBy() throws Exception {
		tPainting.insert(3, null, "title0");
		tPainting.insert(2, null, "title1");
		tPainting.insert(1, null, "title2");

		EJBQLQuery asc = new EJBQLQuery("select p from Painting p order by p.paintingTitle");
		List<Painting> paintingsAsc = context.performQuery(asc);
		assertEquals(3, paintingsAsc.size());
		assertEquals("title0", paintingsAsc.get(0).getPaintingTitle());
		assertEquals("title1", paintingsAsc.get(1).getPaintingTitle());
		assertEquals("title2", paintingsAsc.get(2).getPaintingTitle());

		EJBQLQuery desc = new EJBQLQuery("select p from Painting p order by p.paintingTitle desc");
		List<Painting> paintingsDesc = context.performQuery(desc);
		assertEquals(3, paintingsDesc.size());
		assertEquals("title2", paintingsDesc.get(0).getPaintingTitle());
		assertEquals("title1", paintingsDesc.get(1).getPaintingTitle());
		assertEquals("title0", paintingsDesc.get(2).getPaintingTitle());
	}

	@Test
	public void orderBy_Aggregates() throws Exception {
		tArtist.insert(1, "a0");
		tArtist.insert(2, "a1");

		tPainting.insert(3, 1, "title0");
		tPainting.insert(2, 1, "title1");
		tPainting.insert(1, 2, "title2");

		EJBQLQuery asc = new EJBQLQuery("select a, count(p) from Artist a INNER JOIN a.paintingArray p GROUP BY a order by count(p)");
		List<Object[]> artistAsc = context.performQuery(asc);
		assertEquals(2, artistAsc.size());
		assertEquals("a1", ((Artist) artistAsc.get(0)[0]).getArtistName());
		assertEquals("a0", ((Artist) artistAsc.get(1)[0]).getArtistName());

		EJBQLQuery desc = new EJBQLQuery("select a, count(p) from Artist a INNER JOIN a.paintingArray p GROUP BY a order by count(p) DESC");
		List<Object[]> artistDesc = context.performQuery(desc);
		assertEquals(2, artistDesc.size());
		assertEquals("a0", ((Artist) artistDesc.get(0)[0]).getArtistName());
		assertEquals("a1", ((Artist) artistDesc.get(1)[0]).getArtistName());
	}

    @Test
    public void outerJoinCountByIdentifier() throws Exception {
        tArtist.insert(1, "a0");
        tArtist.insert(2, "a1");
        tArtist.insert(3, "a2");

        tPainting.insert(3, 1, "title0");
        tPainting.insert(2, 1, "title1");
        tPainting.insert(1, 2, "title2");

        EJBQLQuery asc = new EJBQLQuery("select a, count(p) from Artist a LEFT JOIN a.paintingArray p " +
                "GROUP BY a order by count(p) DESC");
        List<Object[]> artistAsc = context.performQuery(asc);
        assertEquals(3, artistAsc.size());
        assertEquals("a0", ((Artist) artistAsc.get(0)[0]).getArtistName());
        assertEquals("a1", ((Artist) artistAsc.get(1)[0]).getArtistName());
        assertEquals("a2", ((Artist) artistAsc.get(2)[0]).getArtistName());

        assertEquals(2L, artistAsc.get(0)[1]);
        assertEquals(1L, artistAsc.get(1)[1]);
        assertEquals(0L, artistAsc.get(2)[1]);
    }

    @Test
    public void outerJoinCountAll() throws Exception {
        tArtist.insert(1, "a0");
        tArtist.insert(2, "a1");
        tArtist.insert(3, "a2");

        tPainting.insert(3, 1, "title0");
        tPainting.insert(2, 1, "title1");
        tPainting.insert(1, 2, "title2");

        EJBQLQuery asc = new EJBQLQuery("SELECT a, count(1) FROM Artist a LEFT JOIN a.paintingArray p " +
                "GROUP BY a ORDER BY count(1) DESC, a.artistName");
        List<Object[]> artistAsc = context.performQuery(asc);
        assertEquals(3, artistAsc.size());
        assertEquals("a0", ((Artist) artistAsc.get(0)[0]).getArtistName());
        assertEquals("a1", ((Artist) artistAsc.get(1)[0]).getArtistName());
        assertEquals("a2", ((Artist) artistAsc.get(2)[0]).getArtistName());

        assertEquals(2L, artistAsc.get(0)[1]);
        assertEquals(1L, artistAsc.get(1)[1]);
        assertEquals(1L, artistAsc.get(2)[1]); // here is a difference with other cases
    }

    @Test
    public void outerJoinCountByPath() throws Exception {
        tArtist.insert(1, "a0");
        tArtist.insert(2, "a1");
        tArtist.insert(3, "a2");

        tPainting.insert(3, 1, "title0");
        tPainting.insert(2, 1, "title1");
        tPainting.insert(1, 2, "title2");

        EJBQLQuery asc = new EJBQLQuery("select a, count(a.paintingArray+) from Artist a " +
                "GROUP BY a order by count(a.paintingArray+) DESC");
        List<Object[]> artistAsc = context.performQuery(asc);
        assertEquals(3, artistAsc.size());
        assertEquals("a0", ((Artist) artistAsc.get(0)[0]).getArtistName());
        assertEquals("a1", ((Artist) artistAsc.get(1)[0]).getArtistName());
        assertEquals("a2", ((Artist) artistAsc.get(2)[0]).getArtistName());

        assertEquals(2L, artistAsc.get(0)[1]);
        assertEquals(1L, artistAsc.get(1)[1]);
        assertEquals(0L, artistAsc.get(2)[1]);
    }

	@Test
	public void nullObjects() throws Exception {
        tArtist.insert(1, "a1");
        tArtist.insert(2, "a2");
        tArtist.insert(3, "a3");

        tPainting.insert(1, 2, "title1");
        tPainting.insert(2, 1, "title2");
        tPainting.insert(3, 1, "title3");

        EJBQLQuery queryFullProduct = new EJBQLQuery("select a, p from Artist a, Painting p");
        List<Object[]> result1 = context.performQuery(queryFullProduct);
        assertEquals(9, result1.size());
        for(Object[] next : result1) {
            assertEquals(2, next.length);
            assertNotNull(next[0]);
            assertNotNull(next[1]);
        }

        EJBQLQuery queryToOneRel = new EJBQLQuery("select p.toGallery+, p.toArtist+, p from Painting p");
        List<Object[]> result2 = context.performQuery(queryToOneRel);
        assertEquals(3, result2.size());
        for(Object[] next : result2) {
            assertNull(next[0]); // Gallery
            assertInstanceOf(Artist.class, next[1]);
            assertInstanceOf(Painting.class, next[2]);
        }
    }

    @Test
    public void nullObjectsCallback() throws Exception {
        tArtist.insert(1, "a1");
        tArtist.insert(2, "a2");
        tArtist.insert(3, "a3");

        tPainting.insert(1, 2, "title1");
        tPainting.insert(2, 1, "title2");
        tPainting.insert(3, 1, "title3");

        // set callback to be called
        LifecycleCallbackRegistry registry = runtime
                .getDataDomain()
                .getEntityResolver()
                .getCallbackRegistry();
        registry.addCallback(LifecycleEvent.POST_LOAD, Painting.class, "postAddCallback");

        // select Paintings, where one of it will be null
        EJBQLQuery query = new EJBQLQuery("select a.paintingArray+ from Artist a order by a.artistName");
        List<Painting> result1 = context.performQuery(query);
        assertEquals(4, result1.size());
        assertNull(result1.get(3));
        for(int i=0; i<3; i++) {
            assertNotNull(result1.get(i));
            assertTrue(result1.get(i).isPostAdded());
        }
    }

    @Test
    public void orderByDbPath() throws Exception {
        tArtist.insert(1, "a3");
        tArtist.insert(2, "a2");
        tArtist.insert(3, "a1");

        EJBQLQuery query = new EJBQLQuery("SELECT a FROM Artist a ORDER BY db:a.ARTIST_ID DESC");
        List<Artist> result = context.performQuery(query);
        assertEquals("a1", result.get(0).getArtistName());
        assertEquals("a2", result.get(1).getArtistName());
        assertEquals("a3", result.get(2).getArtistName());
    }

    @Test
    public void selectFromNestedContext() throws Exception {
        tArtist.insert(1, "a1");
        tArtist.insert(2, "a2");

        tPainting.insert(1, 2, "title1");
        tPainting.insert(2, 1, "title2");
        tPainting.insert(3, 1, "title3");

        ObjectContext nested = runtime.newContext(context);

        EJBQLQuery query = new EJBQLQuery("SELECT a, COUNT(a.paintingArray) FROM Artist a GROUP BY a");
        List<Object[]> result = nested.performQuery(query);
        assertEquals(2, result.size());
        for(Object[] next : result) {
            assertInstanceOf(Artist.class, next[0]);
            assertInstanceOf(Number.class, next[1]);
        }

    }
}
