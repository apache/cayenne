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
import org.apache.cayenne.map.ProcedureQueryDescriptor;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProcedureQueryDescriptorHandlerTest extends BaseHandlerTest {

    private DataMap map;

    @BeforeEach
    public void createMap() {
        map = new DataMap("map");
    }

    private ProcedureQueryDescriptor parseQuery(String xml) throws Exception {
        parse("procedureQuery", xml, parent -> new ProcedureQueryDescriptorHandler(parent, map));
        return (ProcedureQueryDescriptor) map.getQueryDescriptor("q");
    }

    @Test
    public void procedureQuery() throws Exception {
        Procedure procedure = new Procedure("P1");
        map.addProcedure(procedure);

        ProcedureQueryDescriptor descriptor = parseQuery("""
                <procedureQuery name="q" root="procedure" rootName="P1" resultEntity="Artist"
                                cacheStrategy="LOCAL_CACHE" dataRows="true" pageSize="5" statementFetchSize="7"
                                fetchLimit="10" fetchOffset="20" columnNameCapitalization="upper">
                    <cacheGroup>g</cacheGroup>
                </procedureQuery>
                """);

        assertEquals("q", descriptor.getName());
        assertSame(map, descriptor.getDataMap());
        assertSame(procedure, descriptor.getRoot());
        assertEquals("Artist", descriptor.getResultEntityName());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, descriptor.getCacheStrategy());
        assertEquals("g", descriptor.getCacheGroup());
        assertTrue(descriptor.isFetchingDataRows());
        assertEquals(5, descriptor.getPageSize());
        assertEquals(7, descriptor.getStatementFetchSize());
        assertEquals(10, descriptor.getFetchLimit());
        assertEquals(20, descriptor.getFetchOffset());
        assertEquals(CapsStrategy.UPPER, descriptor.getColumnNamesCapitalization());
    }

    @Test
    public void procedureQueryMinimal() throws Exception {
        ProcedureQueryDescriptor descriptor = parseQuery("""
                <procedureQuery name="q"/>
                """);

        assertSame(map, descriptor.getRoot());
        assertNull(descriptor.getResultEntityName());
        assertEquals(0, descriptor.getFetchLimit());
        assertEquals(0, descriptor.getFetchOffset());
        assertNull(descriptor.getColumnNamesCapitalization());
    }

    @Test
    public void rootNotFound() throws Exception {
        assertSame(map, parseQuery("""
                <procedureQuery name="q" root="procedure" rootName="NoSuchProcedure"/>
                """).getRoot());
    }
}
