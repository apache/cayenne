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
package org.apache.cayenne.runtime;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.datasource.CayenneDataSource;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks that Cayenne stack startup is lazy in regard to the DB access: the runtime must start even if the DB is down,
 * and only fail when the first query is run.
 */
public class CayenneRuntimeLazyStartupTest {

    private CayenneRuntime runtime;

    @AfterEach
    public void stopRuntime() {
        if (runtime != null) {
            runtime.shutdown();
        }
    }

    @Test
    public void startup_NoDataSourceAccess() {

        DownDataSource dataSource = new DownDataSource();
        runtime = CayenneRuntime.of().addConfig("cayenne-testmap.xml").defaultDataNode(dataSource).build();

        DataDomain domain = runtime.getDataDomain();
        assertEquals(1, domain.getDataNodes().size());

        DataNode node = domain.getDataNodes().iterator().next();
        assertNotNull(node.getAdapter());
        assertNotNull(node.getDataSource());
        assertNotNull(domain.getEntityResolver().getObjEntity(Artist.class));
        assertNotNull(domain.getEntityResolver().getClassDescriptor("Artist"));

        dataSource.assertNotAccessed();
    }

    @Test
    public void startup_SchemaUpdateStrategy_NoDataSourceAccess() {

        DownDataSource dataSource = new DownDataSource();
        DataNodeDescriptor node = DataNodeDescriptor.of("n1").dataSource(dataSource).createSchemaIfNeeded().build();
        runtime = CayenneRuntime.of().addConfig("cayenne-testmap.xml").defaultDataNode(node).build();

        runtime.getDataDomain();
        runtime.newContext();
        dataSource.assertNotAccessed();

        ObjectContext context = runtime.newContext();
        assertThrows(CayenneRuntimeException.class, () -> ObjectSelect.query(Artist.class).select(context));
        assertFalse(dataSource.accessStacks.isEmpty());
    }

    @Test
    public void startup_ExplicitAdapter_NoDataSourceAccess() {

        DownDataSource dataSource = new DownDataSource();
        DataNodeDescriptor node = DataNodeDescriptor.of("n1").dataSource(dataSource).adapter(H2Adapter.class).build();
        runtime = CayenneRuntime.of().addConfig("cayenne-testmap.xml").defaultDataNode(node).build();

        DataNode dataNode = runtime.getDataDomain().getDataNodes().iterator().next();
        assertNotNull(dataNode.getPkGenerator());
        runtime.newContext();

        dataSource.assertNotAccessed();
    }

    @Test
    public void startup_CayennePool() throws SQLException {

        List<Throwable> accessStacks = new CopyOnWriteArrayList<>();
        Driver driver = mock(Driver.class);
        when(driver.connect(any(), any())).thenAnswer(i -> {
            accessStacks.add(new Throwable("Driver accessed"));
            throw new SQLException("DB is down");
        });

        // the pool itself is not lazy - it tries to open "min" connections when created, but it ignores the failures
        DataSource dataSource = CayenneDataSource.of("jdbc:down:db").driver(driver).pool(1, 2).build();
        assertEquals(1, accessStacks.size());

        // ... and the rest of the stack must not go to the pool on startup
        runtime = CayenneRuntime.of().addConfig("cayenne-testmap.xml").defaultDataNode(dataSource).build();
        runtime.getDataDomain();
        ObjectContext context = runtime.newContext();

        assertEquals(1, accessStacks.size(), () -> "Unexpected Driver access:\n" + accessStacks.stream()
                .skip(1)
                .map(DownDataSource::stackToString)
                .collect(Collectors.joining("\n")));

        assertThrows(CayenneRuntimeException.class, () -> ObjectSelect.query(Artist.class).select(context));
        assertTrue(accessStacks.size() > 1);
    }

    @Test
    public void startup_CayennePool_ZeroMinConnections_NoDriverAccess() throws SQLException {

        Driver driver = mock(Driver.class);
        DataSource dataSource = CayenneDataSource.of("jdbc:down:db").driver(driver).pool(0, 2).build();

        runtime = CayenneRuntime.of().addConfig("cayenne-testmap.xml").defaultDataNode(dataSource).build();
        runtime.getDataDomain();
        runtime.newContext();

        verify(driver, never()).connect(any(), any());
    }

    @Test
    public void context_NoDataSourceAccess() {

        DownDataSource dataSource = new DownDataSource();
        runtime = CayenneRuntime.of().addConfig("cayenne-testmap.xml").defaultDataNode(dataSource).build();

        ObjectContext context = runtime.newContext();
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("a1");
        context.rollbackChanges();

        ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("a1")).orderBy(Artist.ARTIST_NAME.asc());

        dataSource.assertNotAccessed();
    }

    @Test
    public void firstQuery_Throws() {

        DownDataSource dataSource = new DownDataSource();
        runtime = CayenneRuntime.of().addConfig("cayenne-testmap.xml").defaultDataNode(dataSource).build();

        ObjectContext context = runtime.newContext();
        dataSource.assertNotAccessed();

        assertThrows(CayenneRuntimeException.class, () -> ObjectSelect.query(Artist.class).select(context));
        assertFalse(dataSource.accessStacks.isEmpty());
    }

    @Test
    public void firstCommit_Throws() {

        DownDataSource dataSource = new DownDataSource();
        runtime = CayenneRuntime.of().addConfig("cayenne-testmap.xml").defaultDataNode(dataSource).build();

        ObjectContext context = runtime.newContext();
        context.newObject(Artist.class).setArtistName("a1");
        dataSource.assertNotAccessed();

        assertThrows(CayenneRuntimeException.class, context::commitChanges);
        assertFalse(dataSource.accessStacks.isEmpty());
    }

    @Test
    public void repeatedQuery_RetriesDataSource() {

        DownDataSource dataSource = new DownDataSource();
        runtime = CayenneRuntime.of().addConfig("cayenne-testmap.xml").defaultDataNode(dataSource).build();

        ObjectContext context = runtime.newContext();
        assertThrows(CayenneRuntimeException.class, () -> ObjectSelect.query(Artist.class).select(context));

        // a failure must not leave the stack in a permanently broken state... The second attempt should hit the
        // DataSource again instead of rethrowing some cached startup error
        int accessCount = dataSource.accessStacks.size();
        assertThrows(CayenneRuntimeException.class, () -> ObjectSelect.query(Artist.class).select(context));
        assertTrue(dataSource.accessStacks.size() > accessCount);
    }

    static class DownDataSource implements DataSource {

        final List<Throwable> accessStacks = new CopyOnWriteArrayList<>();

        void assertNotAccessed() {
            assertTrue(accessStacks.isEmpty(), () -> "Unexpected DataSource access:\n" + accessStacks.stream()
                    .map(DownDataSource::stackToString)
                    .collect(Collectors.joining("\n")));
        }

        static String stackToString(Throwable th) {
            StringWriter out = new StringWriter();
            th.printStackTrace(new PrintWriter(out));
            return out.toString();
        }

        @Override
        public Connection getConnection() throws SQLException {
            accessStacks.add(new Throwable("DataSource accessed"));
            throw new SQLException("DB is down");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Not a wrapper");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}
