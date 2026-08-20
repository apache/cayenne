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
package org.apache.cayenne.configuration.runtime;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.datasource.ManagedPoolingDataSource;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.runtime.CayenneRuntimeBuilder;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultDataNodeFactoryIT {

    // a DB of our own, unrelated to the shared test DB, to tell apart the DataSource built off the properties
    private static final String PROPS_URL = "jdbc:hsqldb:mem:dnf_props";
    private static final String SHARED_PROPS_URL = "jdbc:hsqldb:mem:dnf_props_shared";
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

        DataNode node = factory().createDataNode("channel", descriptor);
        assertEquals("n1", node.getName());
    }

    @Test
    public void dataSourceFromDescriptor() throws Exception {
        DataSource dataSource = CayenneTestsEnv.COMMON_SCHEMA.dataSource();
        DataNode node = factory().createDataNode("channel",
                DataNodeDescriptor.of("n1").dataSource(dataSource).build());

        assertNotNull(node.getDataSource());
        assertSame(dataSource, node.getDataSource().unwrap(DataSource.class));
    }

    @Test
    public void dataSourceFromProperties() throws Exception {

        // a dedicated DB, so that the assertions below can't be satisfied by the shared test DataSource
        DataNodeFactory factory = factory(b -> CoreModule.extend(b)
                .setProperty(Constants.JDBC_URL_PROPERTY + ".channel.n1", PROPS_URL)
                .setProperty(Constants.JDBC_DRIVER_PROPERTY + ".channel.n1", PROPS_DRIVER)
                .setProperty(Constants.JDBC_USERNAME_PROPERTY + ".channel.n1", "sa")
                .setProperty(Constants.JDBC_PASSWORD_PROPERTY + ".channel.n1", "")
                .setProperty(Constants.JDBC_MIN_CONNECTIONS_PROPERTY + ".channel.n1", "1")
                .setProperty(Constants.JDBC_MAX_CONNECTIONS_PROPERTY + ".channel.n1", "2"));

        // a descriptor with no DataSource of its own
        DataNode node = factory.createDataNode("channel", DataNodeDescriptor.of("n1").build());

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
                .setProperty(Constants.JDBC_URL_PROPERTY, SHARED_PROPS_URL)
                .setProperty(Constants.JDBC_URL_PROPERTY + ".channel.n1", PROPS_URL)
                .setProperty(Constants.JDBC_DRIVER_PROPERTY, PROPS_DRIVER)
                .setProperty(Constants.JDBC_USERNAME_PROPERTY, "sa")
                .setProperty(Constants.JDBC_PASSWORD_PROPERTY, ""));

        DataNode n1 = factory.createDataNode("channel", DataNodeDescriptor.of("n1").build());
        try (Connection c = n1.getDataSource().getConnection()) {
            assertEquals(PROPS_URL, c.getMetaData().getURL());
        }

        // a node with no properties of its own falls back to the shared URL
        DataNode n2 = factory.createDataNode("channel", DataNodeDescriptor.of("n2").build());
        try (Connection c = n2.getDataSource().getConnection()) {
            assertEquals(SHARED_PROPS_URL, c.getMetaData().getURL());
        }
    }

    @Test
    public void adapterFromDescriptor() {
        DataNode node = factory().createDataNode("channel", DataNodeDescriptor.of("n1")
                .dataSource(CayenneTestsEnv.COMMON_SCHEMA.dataSource())
                .adapter(JdbcAdapter.class)
                .build());

        assertInstanceOf(JdbcAdapter.class, node.getAdapter());
    }

    @Test
    public void autoAdapterWhenNoAdapterInDescriptor() {
        DataNode node = factory().createDataNode("channel", DataNodeDescriptor.of("n1")
                .dataSource(CayenneTestsEnv.COMMON_SCHEMA.dataSource())
                .build());

        assertInstanceOf(AutoAdapter.class, node.getAdapter());
    }

    @Test
    public void schemaUpdateStrategyFromDescriptor() {
        DataNode node = factory().createDataNode("channel", DataNodeDescriptor.of("n1")
                .dataSource(CayenneTestsEnv.COMMON_SCHEMA.dataSource())
                .createSchemaIfNeeded()
                .build());

        assertInstanceOf(CreateIfNoSchemaStrategy.class, node.getSchemaUpdateStrategy());
    }

    @Test
    public void defaultSchemaUpdateStrategy() {
        DataNode node = factory().createDataNode("channel", DataNodeDescriptor.of("n1")
                .dataSource(CayenneTestsEnv.COMMON_SCHEMA.dataSource())
                .build());

        assertInstanceOf(SkipSchemaUpdateStrategy.class, node.getSchemaUpdateStrategy());
    }
}
