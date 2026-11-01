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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.NamedProcedureQuery;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NamedProcedureQueryHandlerTest extends BaseHandlerTest {

    private DataMap map;

    @BeforeEach
    public void createMap() {
        map = new DataMap("map");
    }

    private NamedProcedureQuery parseQuery(String xml) throws Exception {
        parse("procedureQuery", xml, parent -> new NamedProcedureQueryHandler(parent, map));
        return (NamedProcedureQuery) map.getNamedQuery("q");
    }

    @Test
    public void procedureQuery() throws Exception {
        Procedure procedure = new Procedure("P1");
        map.addProcedure(procedure);

        NamedProcedureQuery namedQuery = parseQuery("""
                <procedureQuery name="q" root="procedure" rootName="P1" resultEntity="Artist"
                                cacheStrategy="LOCAL_CACHE" dataRows="true" pageSize="5" statementFetchSize="7"
                                fetchLimit="10" fetchOffset="20" columnNameCapitalization="upper">
                    <cacheGroup>g</cacheGroup>
                </procedureQuery>
                """);

        assertEquals("q", namedQuery.getName());
        assertSame(map, namedQuery.getDataMap());
        assertSame(procedure, namedQuery.getRoot());
        assertEquals("Artist", namedQuery.getResultEntityName());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, namedQuery.getCacheStrategy());
        assertEquals("g", namedQuery.getCacheGroup());
        assertTrue(namedQuery.isFetchingDataRows());
        assertEquals(5, namedQuery.getPageSize());
        assertEquals(7, namedQuery.getStatementFetchSize());
        assertEquals(10, namedQuery.getFetchLimit());
        assertEquals(20, namedQuery.getFetchOffset());
        assertEquals(CapsStrategy.UPPER, namedQuery.getColumnNamesCapitalization());
    }

    @Test
    public void procedureQueryMinimal() throws Exception {
        NamedProcedureQuery namedQuery = parseQuery("""
                <procedureQuery name="q"/>
                """);

        assertSame(map, namedQuery.getRoot());
        assertNull(namedQuery.getResultEntityName());
        assertEquals(0, namedQuery.getFetchLimit());
        assertEquals(0, namedQuery.getFetchOffset());
        assertNull(namedQuery.getColumnNamesCapitalization());
    }

    @Test
    public void rootNotFound() throws Exception {
        assertSame(map, parseQuery("""
                <procedureQuery name="q" root="procedure" rootName="NoSuchProcedure"/>
                """).getRoot());
    }
}
