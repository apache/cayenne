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

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.util.XMLEncoder;

public class EJBQLQueryTest extends CayenneCase {

    public void testParameters() {
        String ejbql = "select a FROM Artist a WHERE a.artistName = ?1 OR a.artistName = :name";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter(1, "X");
        query.setParameter("name", "Y");

        Map parameters = query.getNamedParameters();
        Map parameters1 = query.getPositionalParameters();
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
        q1.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        String ejbql2 = "select a FROM Artist a WHERE a.artistName = ?1 OR a.artistName = :name";
        EJBQLQuery q2 = new EJBQLQuery(ejbql2);
        q2.setParameter(1, "X");
        q2.setParameter("name", "Y");
        q2.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        EntityResolver resolver = getDomain().getEntityResolver();

        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2
                .getMetaData(resolver)
                .getCacheKey());
    }

    public void testCacheStrategy() throws Exception {
        deleteTestData();

        insertValue();
        DataContext contex = createDataContext();
        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        List<Artist> artist1 = contex.performQuery(query);
        blockQueries();
        List<Artist> artist2;
        try {
            EJBQLQuery query1 = new EJBQLQuery(ejbql);
            query1.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
            artist2 = contex.performQuery(query1);
        }
        finally {
            unblockQueries();
        }

        assertEquals(artist1.get(0).getArtistName(), artist2.get(0).getArtistName());
    }

    public void testDataRows() throws Exception {
        deleteTestData();

        insertValue();
        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setFetchingDataRows(true);
        List artists = createDataContext().performQuery(query);

        Map row = (Map) artists.get(0);
        String artistName = (String) row.get("ARTIST_NAME");

        assertTrue(row instanceof DataRow);

        Artist artist = (Artist) createDataContext().objectFromDataRow(
                "Artist",
                (DataRow) row,
                true);
        assertEquals(artistName, artist.getArtistName());
    }

    private void insertValue() {
        DataContext context = createDataContext();

        for (int i = 0; i < 5; i++) {
            Artist obj = context.newObject(Artist.class);
            obj.setArtistName("a" + i);
            context.commitChanges();
        }
    }

    private void insertPaintValue() {
        DataContext context = createDataContext();

        for (int i = 0; i < 2; i++) {
            Artist art = context.newObject(Artist.class);
            art.setArtistName("a" + i);
            Painting obj = context.newObject(Painting.class);
            obj.setToArtist(art);
            obj.setPaintingTitle("title" + i);
            context.commitChanges();
        }
    }

    public void testGetExpression() {
        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        EJBQLCompiledExpression parsed = query.getExpression(getDomain()
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
        EntityResolver resolver = getDomain().getEntityResolver();
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

        EntityResolver resolver = getDomain().getEntityResolver();
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
        assertTrue(md.isResolvingInherited());
        assertEquals(QueryCacheStrategy.NO_CACHE, md.getCacheStrategy());
    }

    public void testSelectRelationship() {

        insertPaintValue();
        DataContext contex = createDataContext();
        
        String ejbql = "SELECT p.toArtist FROM Painting p"; 
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List result = contex.performQuery(query);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(Artist.class, result.get(0).getClass());

        
        String ejbql2 = "SELECT p.toArtist, p FROM Painting p"; 
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List result2 = contex.performQuery(query2);

        assertNotNull(result2);
        assertEquals(2, result2.size());
        assertEquals(2, ((Object[])result2.get(0)).length);
        
        assertEquals(Artist.class,((Object[])result2.get(0))[0].getClass());
        assertEquals(Painting.class,((Object[])result2.get(0))[1].getClass());
        
        String ejbql3 = "SELECT p.toArtist, p.paintingTitle FROM Painting p"; 
        EJBQLQuery query3 = new EJBQLQuery(ejbql3);

        List result3 = contex.performQuery(query3);

        assertNotNull(result3);
        assertEquals(2, result3.size());
        assertEquals(2, ((Object[])result3.get(0)).length);
        
        assertEquals(Artist.class,((Object[])result3.get(0))[0].getClass());
        assertEquals(String.class,((Object[])result3.get(0))[1].getClass());
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
        createDataContext().performQuery(query);
    }
    
    public void testNullNotEqualsParameter() {
        EJBQLQuery query = new EJBQLQuery("select p from Painting p WHERE p.toArtist<>:x");
        query.setParameter("x", null);
        createDataContext().performQuery(query);
    }
    
    public void testNullPositionalParameter() {
        EJBQLQuery query = new EJBQLQuery("select p from Painting p WHERE p.toArtist=?1");
        query.setParameter(1, null);
        createDataContext().performQuery(query);
    }
    
    public void testNullAndNotNullParameter() {
        EJBQLQuery query = new EJBQLQuery("select p from Painting p WHERE p.toArtist=:x OR p.toArtist.artistName=:b");
        query.setParameter("x", null);
        query.setParameter("b", "Y");
        createDataContext().performQuery(query);
    }
    
    public void testJoinToJoined() {
        ObjectContext context = createDataContext();
        
        EJBQLQuery query = new EJBQLQuery(
            "select g from Gallery g inner join g.paintingArray p where p.toArtist.artistName like '%a%'");
        context.performQuery(query);
    }
    
    //test for CAY-1313
    public void testRelationshipWhereClauseAndToEJBQL() throws Exception {
        ObjectContext context = createDataContext();
        
        Expression exp = ExpressionFactory.matchExp(Painting.TO_GALLERY_PROPERTY, null);
        EJBQLQuery query = new EJBQLQuery("select p.toArtist from Painting p where " + exp.toEJBQL("p"));
    
        context.performQuery(query);
    }
    
    public void testOrBrackets() throws Exception {
        deleteTestData();
        ObjectContext context = createDataContext();
        
        Artist a = context.newObject(Artist.class);
        a.setArtistName("testOrBrackets");
        context.commitChanges();
        
        //this query is equivalent to (false and (false or true)) and
        //should always return 0 rows
        EJBQLQuery query = new EJBQLQuery("select a from Artist a " +
    		"where a.artistName <> a.artistName and " +
    		"(a.artistName <> a.artistName or a.artistName = a.artistName)");
        assertEquals(context.performQuery(query).size(), 0);
        
        //on the other hand, the following is equivalent to (false and false) or true) and
        //should return >0 rows
        query = new EJBQLQuery("select a from Artist a " +
            "where a.artistName <> a.artistName and " +
            "a.artistName <> a.artistName or a.artistName = a.artistName");
        assertTrue(context.performQuery(query).size() > 0);
        
        //checking brackets around not
        query = new EJBQLQuery("select a from Artist a " +
            "where not(a.artistName <> a.artistName and " +
            "a.artistName <> a.artistName or a.artistName = a.artistName)");
        assertEquals(context.performQuery(query).size(), 0);
        
        //not is first to process
        query = new EJBQLQuery("select a from Artist a " +
                "where not a.artistName <> a.artistName or " +
                "a.artistName = a.artistName");
        assertTrue(context.performQuery(query).size() > 0);
    }
}
