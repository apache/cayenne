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
package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.RuntimeProperties;
import org.apache.cayenne.CoreModule;
import org.apache.cayenne.project.DataNodeDescriptor;
import org.apache.cayenne.datasource.CayenneDataSource;
import org.apache.cayenne.datasource.ManagedPoolingDataSource;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.CayenneRuntime;
import org.apache.cayenne.CayenneRuntimeBuilder;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultDataNodeFactoryIT {

    // a DB of our own, unrelated to the shared test DB, to tell apart the DataSource built off the properties
    private static final String PROPS_URL = "jdbc:hsqldb:mem:dnf_props";
    private static final String SHARED_PROPS_URL = "jdbc:hsqldb:mem:dnf_props_shared";
    private static final String SCHEMA_URL = "jdbc:hsqldb:mem:dnf_schema";
    private static final String NO_SCHEMA_URL = "jdbc:hsqldb:mem:dnf_no_schema";
    private static final String PROPS_DRIVER = "org.hsqldb.jdbcDriver";

    private CayenneRuntime runtime;

    @AfterEach
    public void stopRuntime() {
        if (runtime != null) {
            runtime.shutdown();
            runtime = null;
        }
    }

    private DataNodeFactory factory(Module... extraModules) {
        CayenneRuntimeBuilder builder = CayenneRuntime.of()
                .addConfig(CayenneProjects.TESTMAP_PROJECT)
                .defaultDataNode(CayenneTestsEnv.COMMON_SCHEMA.dataSource());

        Stream.of(extraModules).forEach(builder::addModule);

        runtime = builder.build();
        return runtime.getInjector().getInstance(DataNodeFactory.class);
    }

    @Test
    public void nodeName() {

        DataNodeDescriptor descriptor = DataNodeDescriptor.of("n1")
                .dataSource(CayenneTestsEnv.COMMON_SCHEMA.dataSource())
                .build();

        DataNode node = factory().createDataNode("channel", descriptor, List.of());
        assertEquals("n1", node.getName());
    }

    @Test
    public void dataSourceFromDescriptor() throws Exception {
        DataSource dataSource = CayenneTestsEnv.COMMON_SCHEMA.dataSource();
        DataNode node = factory().createDataNode("channel",
                DataNodeDescriptor.of("n1").dataSource(dataSource).build(), List.of());

        assertNotNull(node.getDataSource());
        assertSame(dataSource, node.getDataSource().unwrap(DataSource.class));
    }

    @Test
    public void dataSourceFromProperties() throws Exception {

        // a dedicated DB, so that the assertions below can't be satisfied by the shared test DataSource
        DataNodeFactory factory = factory(b -> CoreModule.extend(b)
                .setProperty(RuntimeProperties.JDBC_URL_PROPERTY + ".channel.n1", PROPS_URL)
                .setProperty(RuntimeProperties.JDBC_DRIVER_PROPERTY + ".channel.n1", PROPS_DRIVER)
                .setProperty(RuntimeProperties.JDBC_USERNAME_PROPERTY + ".channel.n1", "sa")
                .setProperty(RuntimeProperties.JDBC_PASSWORD_PROPERTY + ".channel.n1", "")
                .setProperty(RuntimeProperties.JDBC_MIN_CONNECTIONS_PROPERTY + ".channel.n1", "1")
                .setProperty(RuntimeProperties.JDBC_MAX_CONNECTIONS_PROPERTY + ".channel.n1", "2"));

        // a descriptor with no DataSource of its own
        DataNode node = factory.createDataNode("channel", DataNodeDescriptor.of("n1").build(), List.of());

        DataSource dataSource = node.getDataSource().unwrap(DataSource.class);
        assertNotSame(CayenneTestsEnv.COMMON_SCHEMA.dataSource(), dataSource);

        // pooling is only turned on by the connection count properties, so this proves they were read too
        assertInstanceOf(ManagedPoolingDataSource.class, dataSource);

        try (Connection c = node.getDataSource().getConnection()) {
            assertEquals(PROPS_URL, c.getMetaData().getURL());
        }
    }

    @Test
    public void dataSourceFromProperties_NodeSpecificWins() throws Exception {

        // node properties are keyed by "channelName.nodeName", falling back to the unsuffixed shared ones
        DataNodeFactory factory = factory(b -> CoreModule.extend(b)
                .setProperty(RuntimeProperties.JDBC_URL_PROPERTY, SHARED_PROPS_URL)
                .setProperty(RuntimeProperties.JDBC_URL_PROPERTY + ".channel.n1", PROPS_URL)
                .setProperty(RuntimeProperties.JDBC_DRIVER_PROPERTY, PROPS_DRIVER)
                .setProperty(RuntimeProperties.JDBC_USERNAME_PROPERTY, "sa")
                .setProperty(RuntimeProperties.JDBC_PASSWORD_PROPERTY, ""));

        DataNode n1 = factory.createDataNode("channel", DataNodeDescriptor.of("n1").build(), List.of());
        try (Connection c = n1.getDataSource().getConnection()) {
            assertEquals(PROPS_URL, c.getMetaData().getURL());
        }

        // a node with no properties of its own falls back to the shared URL
        DataNode n2 = factory.createDataNode("channel", DataNodeDescriptor.of("n2").build(), List.of());
        try (Connection c = n2.getDataSource().getConnection()) {
            assertEquals(SHARED_PROPS_URL, c.getMetaData().getURL());
        }
    }

    @Test
    public void adapterFromDescriptor() {
        DataNode node = factory().createDataNode("channel", DataNodeDescriptor.of("n1")
                .dataSource(CayenneTestsEnv.COMMON_SCHEMA.dataSource())
                .adapter(JdbcAdapter.class)
                .build(), List.of());

        assertInstanceOf(JdbcAdapter.class, node.getAdapter());
    }

    @Test
    public void detectedAdapterWhenNoAdapterInDescriptor() {
        DataNode node = factory().createDataNode("channel", DataNodeDescriptor.of("n1")
                .dataSource(CayenneTestsEnv.COMMON_SCHEMA.dataSource())
                .build(), List.of());

        DbAdapter adapter = node.getAdapter();
        assertNotNull(adapter);
        assertSame(runtime.getDataDomain().getDefaultNode().getAdapter().getClass(), adapter.getClass(),
                "Must detect the test DB adapter");

        // resolved once and cached
        assertSame(adapter, node.getAdapter());
    }

    @Test
    public void adapterDetectionIsLazy() throws SQLException {
        DataSource brokenDataSource = mock(DataSource.class);
        when(brokenDataSource.getConnection()).thenThrow(new SQLException("DB is down"));

        // the stack must start when the DB is not available...
        DataNode node = factory().createDataNode("channel", DataNodeDescriptor.of("n1")
                .dataSource(brokenDataSource)
                .build(), List.of());

        // ... and fail only when the adapter is actually needed
        assertThrows(CayenneRuntimeException.class, node::getAdapter);
        verify(brokenDataSource).getConnection();
    }

    @Test
    public void schemaUpdateStrategyFromDescriptor() throws SQLException {

        // a dedicated empty DB, to see the strategy creating the schema in it
        DataSource dataSource = CayenneDataSource.of(SCHEMA_URL).driverClass(PROPS_DRIVER).userName("sa").build();
        DataNodeFactory factory = factory();
        DataMap map = runtime.getDataDomain().getDataMap("testmap");
        DataNode node = factory.createDataNode("channel", DataNodeDescriptor.of("n1")
                .dataSource(dataSource)
                .createSchemaIfNeeded()
                .build(), List.of(map));

        // the schema is created on the first use of the node, not on creation
        assertFalse(tableExists(dataSource, "ARTIST"));
        node.getAdapter();
        assertTrue(tableExists(dataSource, "ARTIST"));
    }

    @Test
    public void defaultSchemaUpdateStrategy() throws SQLException {

        DataSource dataSource = CayenneDataSource.of(NO_SCHEMA_URL).driverClass(PROPS_DRIVER).userName("sa").build();
        DataNodeFactory factory = factory();
        DataMap map = runtime.getDataDomain().getDataMap("testmap");
        DataNode node = factory.createDataNode("channel", DataNodeDescriptor.of("n1")
                .dataSource(dataSource)
                .build(), List.of(map));

        // no strategy in the descriptor means skipping the schema update
        node.getAdapter();
        assertFalse(tableExists(dataSource, "ARTIST"));
    }

    @Test
    public void pkGeneratorFromDescriptor() {
        PkGenerator generator = mock(PkGenerator.class);
        DataNode node = factory().createDataNode("channel", DataNodeDescriptor.of("n1")
                .dataSource(CayenneTestsEnv.COMMON_SCHEMA.dataSource())
                .pkGenerator(generator)
                .build(), List.of());

        // the custom generator is used in place of the adapter default
        assertSame(generator, node.getPkGenerator());
        node.getAdapter();
        assertSame(generator, node.getPkGenerator());
    }

    private static boolean tableExists(DataSource dataSource, String table) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            try (ResultSet rs = c.getMetaData().getTables(null, null, table, null)) {
                return rs.next();
            }
        }
    }
}
