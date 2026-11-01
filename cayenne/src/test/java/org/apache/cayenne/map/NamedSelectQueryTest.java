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

package org.apache.cayenne.map;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.SubqueryExp;
import org.apache.cayenne.map.NamedSelectQuery.ResultKind;
import org.apache.cayenne.project.EmptyProjectNodeVisitor;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.resolved.ResolvedSelect;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.jupiter.api.Test;

import static org.apache.cayenne.map.NamedSelectQuery.renamePattern;
import static org.junit.jupiter.api.Assertions.*;

public class NamedSelectQueryTest {

    @Test
    public void buildQuery() {
        NamedSelectQuery namedQuery = namedQuery("from FakeRoot where abc = 5");

        ObjectSelect<?> query = namedQuery.buildQuery();
        assertInstanceOf(ObjectSelect.class, query);
        assertEquals("FakeRoot", query.getEntityName());
        assertEquals(ExpressionFactory.exp("abc = 5"), query.getWhere());
    }

    @Test
    public void buildQueryIsACopy() {
        NamedSelectQuery namedQuery = namedQuery("from FakeRoot");
        assertNotSame(namedQuery.buildQuery(), namedQuery.buildQuery());
        assertNotSame(namedQuery.getParsedQuery(), namedQuery.buildQuery());
    }

    @Test
    public void buildQueryWithParameters() {
        NamedSelectQuery namedQuery = namedQuery("from FakeRoot where abc = $a and def = $b");

        // parameters with no matching value are pruned from the qualifier
        assertEquals(ExpressionFactory.exp("abc = 5"), namedQuery.buildQuery(Map.of("a", 5)).getWhere());
    }

    @Test
    public void buildQueryWithoutParameters() {
        NamedSelectQuery namedQuery = namedQuery("from FakeRoot where abc = $a");
        assertNull(namedQuery.buildQuery(Map.of()).getWhere());
        assertEquals(ExpressionFactory.exp("abc = $a"), namedQuery.buildQuery().getWhere());
    }

    @Test
    public void buildQueryWithSubqueryParameters() {
        NamedSelectQuery namedQuery = namedQuery(
                "from Artist where exists (from Painting where artist = enclosing(self) and price > $price)");

        SubqueryExp bound = (SubqueryExp) namedQuery.buildQuery(Map.of("price", 5)).getWhere().getOperand(0);
        assertEquals(ExpressionFactory.exp("artist = enclosing(self) and price > 5"), bound.getQuery().getWhere());

        SubqueryExp pruned = (SubqueryExp) namedQuery.buildQuery(Map.of()).getWhere().getOperand(0);
        assertEquals(ExpressionFactory.exp("artist = enclosing(self)"), pruned.getQuery().getWhere());
    }

    @Test
    public void buildColumnQuery() {
        NamedSelectQuery namedQuery = namedQuery("select name, count(*) from Artist having count(*) > $min");
        namedQuery.setPageSize(10);

        ObjectSelect<?> query = namedQuery.buildQuery(Map.of("min", 2));
        assertTrue(query.isColumnQuery());
        assertEquals(ExpressionFactory.exp("count(*) > 2"), query.getHaving());
        assertTrue(namedQuery.isColumnQuery());
        assertFalse(namedQuery.isDbQuery());
    }

    @Test
    public void buildDbQuery() {
        DataMap map = new DataMap();
        DbEntity entity = new DbEntity("ARTIST");
        map.addDbEntity(entity);

        NamedSelectQuery namedQuery = namedQuery("from db:ARTIST where NAME = 'a'");
        namedQuery.setDataMap(map);

        ObjectSelect<?> query = namedQuery.buildQuery();
        assertEquals("ARTIST", query.getDbEntityName());
        assertTrue(query.resolve(new EntityResolver(List.of(map))).fetchingDataRows());
        assertTrue(namedQuery.isDbQuery());
        assertSame(entity, namedQuery.getRoot());
    }

    @Test
    public void buildQueryInvalid() {
        NamedSelectQuery namedQuery = namedQuery("from Artist where");
        namedQuery.setName("q");

        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class, namedQuery::buildQuery);
        assertTrue(e.getUnlabeledMessage().startsWith("Query 'q' can't be parsed"), e.getUnlabeledMessage());
    }

    @Test
    public void buildQueryEmpty() {
        NamedSelectQuery namedQuery = new NamedSelectQuery();
        namedQuery.setName("q");
        assertThrows(CayenneRuntimeException.class, namedQuery::buildQuery);
    }

    @Test
    public void getQueryProperties() {
        NamedSelectQuery namedQuery = namedQuery("select distinct self from FakeRoot limit 5 offset 2");
        namedQuery.setPageSize(10);
        namedQuery.setStatementFetchSize(6);
        namedQuery.setFetchingDataRows(true);
        namedQuery.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        namedQuery.setCacheGroup("g1");

        ObjectSelect<?> query = namedQuery.buildQuery();
        assertEquals("select distinct self from FakeRoot limit 5 offset 2", query.toQL());

        // the settings that the query String has no syntax for are only visible once the query is resolved
        ResolvedSelect resolved = query.resolve(fakeRootResolver());
        assertEquals(5, resolved.fetchLimit());
        assertEquals(2, resolved.fetchOffset());
        assertEquals(10, resolved.pageSize());
        assertEquals(6, resolved.statementFetchSize());
        assertTrue(resolved.fetchingDataRows());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, resolved.cacheStrategy());
        assertEquals("g1", resolved.cacheGroup());
    }

    @Test
    public void getQueryPropertiesDefaults() {
        ObjectSelect<?> query = namedQuery("from FakeRoot").buildQuery();
        assertEquals("from FakeRoot", query.toQL());

        ResolvedSelect resolved = query.resolve(fakeRootResolver());
        assertEquals(0, resolved.pageSize());
        assertEquals(0, resolved.statementFetchSize());
        assertFalse(resolved.fetchingDataRows());
        assertEquals(QueryCacheStrategy.NO_CACHE, resolved.cacheStrategy());
        assertNull(resolved.cacheGroup());
    }

    private static EntityResolver fakeRootResolver() {
        DataMap map = new DataMap("map");
        map.addObjEntity(new ObjEntity("FakeRoot"));
        return new EntityResolver(List.of(map));
    }

    @Test
    public void getRoot() {
        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("A");
        map.addObjEntity(entity);

        NamedSelectQuery namedQuery = namedQuery("from A");
        assertEquals("A", namedQuery.getRoot());

        namedQuery.setDataMap(map);
        assertSame(entity, namedQuery.getRoot());

        // a String that can't be parsed has no root
        namedQuery.setQl("from B where");
        assertNull(namedQuery.getRoot());
        assertFalse(namedQuery.isDbQuery());

        namedQuery.setQl("from B");
        assertEquals("B", namedQuery.getRoot());
    }

    @Test
    public void getResultKind() {
        DataMap map = new DataMap();
        map.addObjEntity(new ObjEntity("A"));
        map.addDbEntity(new DbEntity("T"));

        NamedSelectQuery namedQuery = namedQuery("from A");
        namedQuery.setDataMap(map);
        assertEquals(ResultKind.ENTITY, namedQuery.getResultKind());

        namedQuery.setFetchingDataRows(true);
        assertEquals(ResultKind.DATA_ROW, namedQuery.getResultKind());
        namedQuery.setFetchingDataRows(false);

        namedQuery.setQl("from db:T");
        assertEquals(ResultKind.DATA_ROW, namedQuery.getResultKind());

        namedQuery.setQl("select a from A");
        assertEquals(ResultKind.SCALAR, namedQuery.getResultKind());

        namedQuery.setQl("select a, b from A");
        assertEquals(ResultKind.ARRAY, namedQuery.getResultKind());

        // the columns are known without the mapping
        namedQuery.setQl("select a, b from B");
        assertEquals(ResultKind.ARRAY, namedQuery.getResultKind());

        // an unmapped root, or a String that can't be parsed
        namedQuery.setQl("from B");
        assertNull(namedQuery.getResultKind());
        namedQuery.setQl("from A where");
        assertNull(namedQuery.getResultKind());
    }

    @Test
    public void setRoot() {
        assertThrows(UnsupportedOperationException.class, () -> new NamedSelectQuery().setRoot("A"));
    }

    @Test
    public void invalidQl() {
        NamedSelectQuery namedQuery = namedQuery("from Artist where");
        assertEquals("from Artist where", namedQuery.getQl());
        assertFalse(namedQuery.isValid());
        assertThrows(ExpressionException.class, namedQuery::getParsedQuery);

        namedQuery.setQl("from Artist");
        assertTrue(namedQuery.isValid());
        assertNotNull(namedQuery.getParsedQuery());
    }

    @Test
    public void getParameterNames() {
        NamedSelectQuery namedQuery = namedQuery("""
                from Artist where name like $name and dateOfBirth in ($d1, $d2)
                    and exists (from Painting where price > $price and name like $name)""");
        assertEquals(List.of("name", "d1", "d2", "price"), List.copyOf(namedQuery.getParameterNames()));
        assertEquals(Set.of(), namedQuery("from Artist").getParameterNames());
    }

    @Test
    public void encodeAsXML() {
        NamedSelectQuery namedQuery = namedQuery("""
                from Artist
                where artistName = $name
                order by artistName asc""");
        namedQuery.setName("q");
        namedQuery.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        namedQuery.setCacheGroup("g1 & g2");

        // the String is stored verbatim
        assertEquals("""
                <objectQuery name="q" cacheStrategy="LOCAL_CACHE">
                <ql><![CDATA[from Artist
                where artistName = $name
                order by artistName asc]]></ql>
                <cacheGroup><![CDATA[g1 & g2]]></cacheGroup>
                </objectQuery>
                """, encode(namedQuery));
    }

    @Test
    public void encodeAsXMLInvalid() {
        NamedSelectQuery namedQuery = namedQuery("from Artist where");
        namedQuery.setName("q");

        assertEquals("""
                <objectQuery name="q">
                <ql><![CDATA[from Artist where]]></ql>
                </objectQuery>
                """, encode(namedQuery));
    }

    @Test
    public void encodeAsXMLEmpty() {
        NamedSelectQuery namedQuery = new NamedSelectQuery();
        namedQuery.setName("q");

        assertEquals("""
                <objectQuery name="q"/>
                """, encode(namedQuery));
    }

    @Test
    public void renameEntity() {
        NamedSelectQuery namedQuery = namedQuery("select name from  Artist\n  order by name");
        namedQuery.renameEntity(renamePattern("Artist", false), "Painter");
        assertEquals("select name from  Painter\n  order by name", namedQuery.getQl());
        assertEquals("Painter", namedQuery.getParsedQuery().getEntityName());
    }

    @Test
    public void renameEntityInSubquery() {
        NamedSelectQuery namedQuery = namedQuery(
                "from Painting where exists (from Artist where artist = enclosing(self))");
        namedQuery.renameEntity(renamePattern("Artist", false), "Painter");
        assertEquals("from Painting where exists (from Painter where artist = enclosing(self))", namedQuery.getQl());
    }

    @Test
    public void renameEntityWholeTokenOnly() {
        String ql = "from Artist2 where fromArtist = 'Artist' and artist.name = 'x' and from = 1"
                + " and exists (from Artist$1)";
        NamedSelectQuery namedQuery = namedQuery(ql);
        namedQuery.renameEntity(renamePattern("Artist", false), "Painter");
        namedQuery.renameEntity(renamePattern("artist", false), "Painter");
        assertSame(ql, namedQuery.getQl());
    }

    @Test
    public void renameEntitySkipsStringLiterals() {
        String literals = " where name = 'from Artist' or bio = \"from Artist \\\" from Artist\" or name = 'it\\'s'";
        NamedSelectQuery namedQuery = namedQuery("from Artist" + literals);
        namedQuery.renameEntity(renamePattern("Artist", false), "Painter");
        assertEquals("from Painter" + literals, namedQuery.getQl());

        // a literal can span lines
        namedQuery = namedQuery("from Artist where name = 'from\nArtist' and exists (from Artist)");
        namedQuery.renameEntity(renamePattern("Artist", false), "Painter");
        assertEquals("from Painter where name = 'from\nArtist' and exists (from Painter)", namedQuery.getQl());

        // an unterminated literal of a String that can't be parsed doesn't stop the rename
        namedQuery = namedQuery("from Artist where name = 'from Artist");
        namedQuery.renameEntity(renamePattern("Artist", false), "Painter");
        assertEquals("from Painter where name = 'from Painter", namedQuery.getQl());
    }

    @Test
    public void renameDbEntity() {
        NamedSelectQuery namedQuery = namedQuery("from db:ARTIST where NAME = 'a'");
        namedQuery.renameEntity(renamePattern("ARTIST", true), "PAINTER");
        assertEquals("from db:PAINTER where NAME = 'a'", namedQuery.getQl());

        // the grammar allows whitespace after the prefix
        NamedSelectQuery spaced = namedQuery("from db: ARTIST");
        spaced.renameEntity(renamePattern("ARTIST", true), "PAINTER");
        assertEquals("from db: PAINTER", spaced.getQl());

        // an ObjEntity of the same name is a different reference
        namedQuery.renameEntity(renamePattern("PAINTER", false), "ARTIST");
        assertEquals("from db:PAINTER where NAME = 'a'", namedQuery.getQl());

        NamedSelectQuery objQuery = namedQuery("from ARTIST where NAME = 'a'");
        objQuery.renameEntity(renamePattern("ARTIST", true), "PAINTER");
        assertEquals("from ARTIST where NAME = 'a'", objQuery.getQl());
    }

    @Test
    public void renameEntityNullAndSameName() {
        NamedSelectQuery empty = new NamedSelectQuery();
        empty.renameEntity(renamePattern("Artist", false), "Painter");
        assertNull(empty.getQl());

        // a String that can't be parsed is renamed too, as it is kept to be fixed
        NamedSelectQuery invalid = namedQuery("from Artist where");
        invalid.renameEntity(renamePattern("Artist", false), "Painter");
        assertEquals("from Painter where", invalid.getQl());

        String ql = "from Artist";
        NamedSelectQuery same = namedQuery(ql);
        same.renameEntity(renamePattern("Artist", false), "Artist");
        assertSame(ql, same.getQl());
    }

    private static NamedSelectQuery namedQuery(String ql) {
        NamedSelectQuery namedQuery = new NamedSelectQuery();
        namedQuery.setQl(ql);
        return namedQuery;
    }

    private static String encode(NamedSelectQuery namedQuery) {
        StringWriter out = new StringWriter();
        namedQuery.encodeAsXML(new XMLEncoder(new PrintWriter(out)), new EmptyProjectNodeVisitor());
        return out.toString();
    }
}
