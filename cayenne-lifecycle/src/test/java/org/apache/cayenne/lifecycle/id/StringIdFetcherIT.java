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
package org.apache.cayenne.lifecycle.id;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.datasource.CayenneDataSource;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DbHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringIdFetcherIT {

    private CayenneRuntime runtime;
    private TableHelper e1Helper;
    private TableHelper e2Helper;

    @BeforeEach
    public void setUp() throws Exception {
        DataSource dataSource = CayenneDataSource.of("jdbc:hsqldb:mem:lifecycle")
                .driverClass("org.hsqldb.jdbcDriver")
                .userName("sa")
                .pool(1, 1)
                .build();

        DataNodeDescriptor dataNode = DataNodeDescriptor.of("lifecycle")
                .dataSource(dataSource)
                .createSchemaIfNeeded()
                .build();

        runtime = CayenneRuntime.of()
                .addConfig("cayenne-lifecycle.xml")
                .defaultDataNode(dataNode)
                .build();
        DbHelper dbHelper = new DbHelper(runtime.getDataSource());
        e1Helper = new TableHelper(dbHelper, "E1", "ID");
        e2Helper = new TableHelper(dbHelper, "E2", "ID");

        e1Helper.deleteAll();
        e2Helper.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        runtime.shutdown();
    }

    @Test
    public void fetchOne() throws Exception {
        e1Helper.insert(3).insert(4);

        ObjectContext context = runtime.newContext();
        Persistent e1 = StringIdFetcher.fetchOne(context, "E1:3");
        assertNotNull(e1);
        assertEquals("E1", e1.getObjectId().getEntityName());
        assertEquals(3L, e1.getObjectId().getIdSnapshot().get("ID"));

        assertNull(StringIdFetcher.fetchOne(context, "E1:5"));
    }

    @Test
    public void fetch_Empty() {
        assertTrue(StringIdFetcher.fetch(runtime.newContext()).isEmpty());
        assertTrue(StringIdFetcher.fetch(runtime.newContext(), List.of()).isEmpty());
    }

    @Test
    public void fetch_SingleEntity() throws Exception {
        e1Helper.insert(3).insert(4);

        Map<String, Persistent> objects = StringIdFetcher
                .fetch(runtime.newContext(), "E1:3", "E1:4", "E1:5", "E1:3");
        assertEquals(2, objects.size());
        assertEquals(3L, objects.get("E1:3").getObjectId().getIdSnapshot().get("ID"));
        assertEquals(4L, objects.get("E1:4").getObjectId().getIdSnapshot().get("ID"));
    }

    @Test
    public void fetch_MultipleEntities() throws Exception {
        e1Helper.insert(3).insert(4);
        e2Helper.insert(5).insert(6).insert(7);

        Map<String, Persistent> objects = StringIdFetcher
                .fetch(runtime.newContext(), List.of("E1:3", "E1:4", "E2:6", "E1:5"));
        assertEquals(3L, objects.size());
        assertEquals("E1", objects.get("E1:3").getObjectId().getEntityName());
        assertEquals("E1", objects.get("E1:4").getObjectId().getEntityName());
        assertEquals("E2", objects.get("E2:6").getObjectId().getEntityName());
        assertEquals(6L, objects.get("E2:6").getObjectId().getIdSnapshot().get("ID"));
    }

    @Test
    public void fetch_UnknownEntity() {
        assertThrows(IllegalArgumentException.class,
                () -> StringIdFetcher.fetch(runtime.newContext(), "NoSuchEntity:1"));
    }
}
