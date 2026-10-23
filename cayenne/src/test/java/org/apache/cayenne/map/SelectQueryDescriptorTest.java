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
import org.apache.cayenne.project.EmptyProjectNodeVisitor;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SelectQueryDescriptorTest {

    @Test
    public void buildQuery() {
        SelectQueryDescriptor descriptor = descriptor("from FakeRoot where abc = 5");

        FluentSelect<?, ?> query = descriptor.buildQuery();
        assertInstanceOf(ObjectSelect.class, query);
        assertEquals("FakeRoot", query.getEntityName());
        assertEquals(ExpressionFactory.exp("abc = 5"), query.getWhere());
    }

    @Test
    public void buildQueryIsACopy() {
        SelectQueryDescriptor descriptor = descriptor("from FakeRoot");
        assertNotSame(descriptor.buildQuery(), descriptor.buildQuery());
        assertNotSame(descriptor.getParsedQuery(), descriptor.buildQuery());
    }

    @Test
    public void buildQueryWithParameters() {
        SelectQueryDescriptor descriptor = descriptor("from FakeRoot where abc = $a and def = $b");

        // parameters with no matching value are pruned from the qualifier
        assertEquals(ExpressionFactory.exp("abc = 5"), descriptor.buildQuery(Map.of("a", 5)).getWhere());
    }

    @Test
    public void buildQueryWithoutParameters() {
        SelectQueryDescriptor descriptor = descriptor("from FakeRoot where abc = $a");
        assertNull(descriptor.buildQuery(Map.of()).getWhere());
        assertEquals(ExpressionFactory.exp("abc = $a"), descriptor.buildQuery().getWhere());
    }

    @Test
    public void buildQueryWithSubqueryParameters() {
        SelectQueryDescriptor descriptor = descriptor(
                "from Artist where exists (from Painting where artist = enclosing(self) and price > $price)");

        SubqueryExp bound = (SubqueryExp) descriptor.buildQuery(Map.of("price", 5)).getWhere().getOperand(0);
        assertEquals(ExpressionFactory.exp("artist = enclosing(self) and price > 5"), bound.getQuery().getWhere());

        SubqueryExp pruned = (SubqueryExp) descriptor.buildQuery(Map.of()).getWhere().getOperand(0);
        assertEquals(ExpressionFactory.exp("artist = enclosing(self)"), pruned.getQuery().getWhere());
    }

    @Test
    public void buildColumnQuery() {
        SelectQueryDescriptor descriptor = descriptor("select name, count(*) from Artist having count(*) > $min");
        descriptor.setPageSize(10);

        FluentSelect<?, ?> query = descriptor.buildQuery(Map.of("min", 2));
        assertInstanceOf(ColumnSelect.class, query);
        assertEquals(ExpressionFactory.exp("count(*) > 2"), query.getHaving());
        assertEquals(10, query.getPageSize());
        assertTrue(descriptor.isColumnQuery());
        assertFalse(descriptor.isDbQuery());
    }

    @Test
    public void buildDbQuery() {
        DataMap map = new DataMap();
        DbEntity entity = new DbEntity("ARTIST");
        map.addDbEntity(entity);

        SelectQueryDescriptor descriptor = descriptor("from db:ARTIST where NAME = 'a'");
        descriptor.setDataMap(map);

        FluentSelect<?, ?> query = descriptor.buildQuery();
        assertEquals("ARTIST", query.getDbEntityName());
        assertTrue(query.isFetchingDataRows());
        assertTrue(descriptor.isDbQuery());
        assertSame(entity, descriptor.getRoot());
    }

    @Test
    public void buildQueryInvalid() {
        SelectQueryDescriptor descriptor = descriptor("from Artist where");
        descriptor.setName("q");

        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class, descriptor::buildQuery);
        assertTrue(e.getUnlabeledMessage().startsWith("Query 'q' can't be parsed"), e.getUnlabeledMessage());
    }

    @Test
    public void buildQueryEmpty() {
        SelectQueryDescriptor descriptor = new SelectQueryDescriptor();
        descriptor.setName("q");
        assertThrows(CayenneRuntimeException.class, descriptor::buildQuery);
    }

    @Test
    public void getQueryProperties() {
        SelectQueryDescriptor descriptor = descriptor("select distinct self from FakeRoot limit 5 offset 2");
        descriptor.setPageSize(10);
        descriptor.setStatementFetchSize(6);
        descriptor.setFetchingDataRows(true);
        descriptor.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        descriptor.setCacheGroup("g1");

        FluentSelect<?, ?> query = descriptor.buildQuery();
        assertEquals(5, query.getLimit());
        assertEquals(2, query.getOffset());
        assertEquals(10, query.getPageSize());
        assertEquals(6, query.getStatementFetchSize());
        assertTrue(query.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, query.getCacheStrategy());
        assertEquals("g1", query.getCacheGroup());
        assertTrue(query.isDistinct());
    }

    @Test
    public void getQueryPropertiesDefaults() {
        FluentSelect<?, ?> query = descriptor("from FakeRoot").buildQuery();
        assertEquals(0, query.getLimit());
        assertEquals(0, query.getOffset());
        assertEquals(0, query.getPageSize());
        assertEquals(0, query.getStatementFetchSize());
        assertFalse(query.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.NO_CACHE, query.getCacheStrategy());
        assertNull(query.getCacheGroup());
        assertFalse(query.isDistinct());
    }

    @Test
    public void getRoot() {
        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("A");
        map.addObjEntity(entity);

        SelectQueryDescriptor descriptor = descriptor("from A");
        assertEquals("A", descriptor.getRoot());

        descriptor.setDataMap(map);
        assertSame(entity, descriptor.getRoot());

        // a String that can't be parsed has no root
        descriptor.setQl("from B where");
        assertNull(descriptor.getRoot());
        assertFalse(descriptor.isDbQuery());

        descriptor.setQl("from B");
        assertEquals("B", descriptor.getRoot());
    }

    @Test
    public void setRoot() {
        assertThrows(UnsupportedOperationException.class, () -> new SelectQueryDescriptor().setRoot("A"));
    }

    @Test
    public void invalidQl() {
        SelectQueryDescriptor descriptor = descriptor("from Artist where");
        assertEquals("from Artist where", descriptor.getQl());
        assertFalse(descriptor.isValid());
        assertThrows(ExpressionException.class, descriptor::getParsedQuery);

        descriptor.setQl("from Artist");
        assertTrue(descriptor.isValid());
        assertNotNull(descriptor.getParsedQuery());
    }

    @Test
    public void getParameterNames() {
        SelectQueryDescriptor descriptor = descriptor("""
                from Artist where name like $name and dateOfBirth in ($d1, $d2)
                    and exists (from Painting where price > $price and name like $name)""");
        assertEquals(List.of("name", "d1", "d2", "price"), List.copyOf(descriptor.getParameterNames()));
        assertEquals(Set.of(), descriptor("from Artist").getParameterNames());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void deprecatedPartGetters() {
        SelectQueryDescriptor descriptor = descriptor("""
                select distinct self from Artist where artistName like $name
                order by artistName desc insensitive, dateOfBirth
                prefetch paintings joint, paintings.gallery disjoint""");

        assertTrue(descriptor.isDistinct());
        assertEquals(ExpressionFactory.exp("artistName like $name"), descriptor.getQualifier());
        assertEquals(List.of(
                new Ordering("artistName", SortOrder.DESCENDING_INSENSITIVE),
                new Ordering("dateOfBirth", SortOrder.ASCENDING)), descriptor.getOrderings());
        assertEquals(Map.of(
                "paintings", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS,
                "paintings.gallery", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS), descriptor.getPrefetchesMap());
    }

    @Test
    public void encodeAsXML() {
        SelectQueryDescriptor descriptor = descriptor("""
                from Artist
                where artistName = $name
                order by artistName asc""");
        descriptor.setName("q");
        descriptor.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        descriptor.setCacheGroup("g1 & g2");

        // the String is stored verbatim
        assertEquals("""
                <objectQuery name="q" cacheStrategy="LOCAL_CACHE">
                <ql><![CDATA[from Artist
                where artistName = $name
                order by artistName asc]]></ql>
                <cacheGroup><![CDATA[g1 & g2]]></cacheGroup>
                </objectQuery>
                """, encode(descriptor));
    }

    @Test
    public void encodeAsXMLInvalid() {
        SelectQueryDescriptor descriptor = descriptor("from Artist where");
        descriptor.setName("q");

        assertEquals("""
                <objectQuery name="q">
                <ql><![CDATA[from Artist where]]></ql>
                </objectQuery>
                """, encode(descriptor));
    }

    @Test
    public void encodeAsXMLEmpty() {
        SelectQueryDescriptor descriptor = new SelectQueryDescriptor();
        descriptor.setName("q");

        assertEquals("""
                <objectQuery name="q"/>
                """, encode(descriptor));
    }

    @Test
    public void renameEntity() {
        SelectQueryDescriptor descriptor = descriptor("select name from  Artist\n  order by name");
        descriptor.renameEntity("Artist", "Painter", false);
        assertEquals("select name from  Painter\n  order by name", descriptor.getQl());
        assertEquals("Painter", descriptor.getParsedQuery().getEntityName());
    }

    @Test
    public void renameEntityInSubquery() {
        SelectQueryDescriptor descriptor = descriptor(
                "from Painting where exists (from Artist where artist = enclosing(self))");
        descriptor.renameEntity("Artist", "Painter", false);
        assertEquals("from Painting where exists (from Painter where artist = enclosing(self))", descriptor.getQl());
    }

    @Test
    public void renameEntityWholeTokenOnly() {
        String ql = "from Artist2 where fromArtist = 'Artist' and artist.name = 'x' and from = 1"
                + " and exists (from Artist$1)";
        SelectQueryDescriptor descriptor = descriptor(ql);
        descriptor.renameEntity("Artist", "Painter", false);
        descriptor.renameEntity("artist", "Painter", false);
        assertSame(ql, descriptor.getQl());
    }

    @Test
    public void renameEntitySkipsStringLiterals() {
        String literals = " where name = 'from Artist' or bio = \"from Artist \\\" from Artist\" or name = 'it\\'s'";
        SelectQueryDescriptor descriptor = descriptor("from Artist" + literals);
        descriptor.renameEntity("Artist", "Painter", false);
        assertEquals("from Painter" + literals, descriptor.getQl());

        // a literal can span lines
        descriptor = descriptor("from Artist where name = 'from\nArtist' and exists (from Artist)");
        descriptor.renameEntity("Artist", "Painter", false);
        assertEquals("from Painter where name = 'from\nArtist' and exists (from Painter)", descriptor.getQl());

        // an unterminated literal of a String that can't be parsed doesn't stop the rename
        descriptor = descriptor("from Artist where name = 'from Artist");
        descriptor.renameEntity("Artist", "Painter", false);
        assertEquals("from Painter where name = 'from Painter", descriptor.getQl());
    }

    @Test
    public void renameDbEntity() {
        SelectQueryDescriptor descriptor = descriptor("from db:ARTIST where NAME = 'a'");
        descriptor.renameEntity("ARTIST", "PAINTER", true);
        assertEquals("from db:PAINTER where NAME = 'a'", descriptor.getQl());

        // the grammar allows whitespace after the prefix
        SelectQueryDescriptor spaced = descriptor("from db: ARTIST");
        spaced.renameEntity("ARTIST", "PAINTER", true);
        assertEquals("from db: PAINTER", spaced.getQl());

        // an ObjEntity of the same name is a different reference
        descriptor.renameEntity("PAINTER", "ARTIST", false);
        assertEquals("from db:PAINTER where NAME = 'a'", descriptor.getQl());

        SelectQueryDescriptor objDescriptor = descriptor("from ARTIST where NAME = 'a'");
        objDescriptor.renameEntity("ARTIST", "PAINTER", true);
        assertEquals("from ARTIST where NAME = 'a'", objDescriptor.getQl());
    }

    @Test
    public void renameEntityNullAndSameName() {
        SelectQueryDescriptor empty = new SelectQueryDescriptor();
        empty.renameEntity("Artist", "Painter", false);
        assertNull(empty.getQl());

        // a String that can't be parsed is renamed too, as it is kept to be fixed
        SelectQueryDescriptor invalid = descriptor("from Artist where");
        invalid.renameEntity("Artist", "Painter", false);
        assertEquals("from Painter where", invalid.getQl());

        String ql = "from Artist";
        SelectQueryDescriptor same = descriptor(ql);
        same.renameEntity("Artist", "Artist", false);
        assertSame(ql, same.getQl());
    }

    private static SelectQueryDescriptor descriptor(String ql) {
        SelectQueryDescriptor descriptor = new SelectQueryDescriptor();
        descriptor.setQl(ql);
        return descriptor;
    }

    private static String encode(SelectQueryDescriptor descriptor) {
        StringWriter out = new StringWriter();
        descriptor.encodeAsXML(new XMLEncoder(new PrintWriter(out)), new EmptyProjectNodeVisitor());
        return out.toString();
    }
}
