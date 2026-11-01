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
package org.apache.cayenne.project.xml;

import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.NamedSelectQuery;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.ql.ParsedObjectSelect;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NamedSelectQueryHandlerTest extends BaseHandlerTest {

    private DataMap map;

    @BeforeEach
    public void createMap() {
        map = new DataMap("map");
    }

    private NamedSelectQuery parseQuery(String xml) throws Exception {
        parse("objectQuery", xml, parent -> new NamedSelectQueryHandler(parent, map));
        return (NamedSelectQuery) map.getNamedQuery("q");
    }

    @Test
    public void querySettings() throws Exception {
        NamedSelectQuery namedQuery = parseQuery("""
                <objectQuery name="q" cacheStrategy="SHARED_CACHE" dataRows="true" pageSize="5" statementFetchSize="7">
                    <ql>from Artist</ql>
                    <cacheGroup>g</cacheGroup>
                </objectQuery>
                """);

        assertEquals("q", namedQuery.getName());
        assertSame(map, namedQuery.getDataMap());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, namedQuery.getCacheStrategy());
        assertEquals("g", namedQuery.getCacheGroup());
        assertTrue(namedQuery.isFetchingDataRows());
        assertEquals(5, namedQuery.getPageSize());
        assertEquals(7, namedQuery.getStatementFetchSize());
    }

    @Test
    public void querySettingsDefaults() throws Exception {
        NamedSelectQuery namedQuery = parseQuery("""
                <objectQuery name="q">
                    <ql>from Artist</ql>
                </objectQuery>
                """);

        assertEquals(QueryCacheStrategy.getDefaultStrategy(), namedQuery.getCacheStrategy());
        assertNull(namedQuery.getCacheGroup());
        assertFalse(namedQuery.isFetchingDataRows());
        assertEquals(0, namedQuery.getPageSize());
        assertEquals(0, namedQuery.getStatementFetchSize());
    }

    @Test
    public void select() throws Exception {
        ObjEntity entity = new ObjEntity("Artist");
        map.addObjEntity(entity);

        NamedSelectQuery namedQuery = parseQuery("""
                <objectQuery name="q">
                    <ql>select distinct self from Artist where artistName like $name
                        order by artistName desc insensitive, dateOfBirth asc limit 10 offset 20
                        prefetch paintings joint</ql>
                </objectQuery>
                """);

        // the String is kept as is, with its formatting
        assertEquals("""
                select distinct self from Artist where artistName like $name
                        order by artistName desc insensitive, dateOfBirth asc limit 10 offset 20
                        prefetch paintings joint""", namedQuery.getQl());

        // parameters are preserved, to be bound when the query is executed
        assertEquals("select distinct self from Artist where artistName like $name "
                + "order by artistName desc insensitive, dateOfBirth limit 10 offset 20 prefetch paintings joint",
                ParsedObjectSelect.parseUnbound(namedQuery.getQl()).toQL());
    }

    @Test
    public void selectWithoutQl() throws Exception {
        NamedSelectQuery namedQuery = parseQuery("""
                <objectQuery name="q">
                    <ql> </ql>
                </objectQuery>
                """);

        assertNull(namedQuery.getQl());
    }

    @Test
    public void selectWithUnknownRoot() throws Exception {
        NamedSelectQuery namedQuery = parseQuery("""
                <objectQuery name="q">
                    <ql>from Artist where artistName = 'a'</ql>
                </objectQuery>
                """);

        // an entity that is not in the DataMap is a problem of the validator, the String is loaded as is
        assertEquals("from Artist where artistName = 'a'", namedQuery.getQl());
    }

    @Test
    public void selectColumnsHavingAndDbRoot() throws Exception {
        for (String ql : List.of(
                "select artistName from Artist",
                "from Artist having count(paintings) > 1",
                "from db:ARTIST")) {
            NamedSelectQuery namedQuery = parseQuery("<objectQuery name=\"q\"><ql>" + ql + "</ql></objectQuery>");
            assertEquals(ql, namedQuery.getQl());
            assertNotNull(ParsedObjectSelect.parseUnbound(namedQuery.getQl()), ql);
            map.removeNamedQuery("q");
        }
    }

    @Test
    public void selectInvalid() throws Exception {
        // a String that can't be parsed is loaded, so that it can be fixed in the Modeler
        NamedSelectQuery namedQuery = parseQuery("""
                <objectQuery name="q">
                    <ql>from Artist where</ql>
                </objectQuery>
                """);

        assertEquals("from Artist where", namedQuery.getQl());
        assertThrows(ExpressionException.class, () -> ParsedObjectSelect.parseUnbound(namedQuery.getQl()));
    }
}
