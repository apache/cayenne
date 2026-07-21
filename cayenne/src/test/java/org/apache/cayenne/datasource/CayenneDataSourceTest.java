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
package org.apache.cayenne.datasource;

import org.apache.cayenne.CayenneRuntimeException;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CayenneDataSourceTest {

    private static final String DRIVER = "org.hsqldb.jdbcDriver";

    @Test
    public void nonPoolingWhenNoPoolSettings() throws Exception {
        DataSource dataSource = CayenneDataSource.of("jdbc:hsqldb:mem:cds_non_pooling")
                .driverClass(DRIVER)
                .userName("sa")
                .password("")
                .build();

        assertInstanceOf(DriverDataSource.class, dataSource);

        try (Connection c = dataSource.getConnection()) {
            assertFalse(c.isClosed());
        }
    }

    @Test
    public void poolingWhenPoolRequested() throws Exception {
        DataSource dataSource = CayenneDataSource.of("jdbc:hsqldb:mem:cds_pooling")
                .driverClass(DRIVER)
                .userName("sa")
                .password("")
                .pool(1, 2)
                .build();

        try (ManagedPoolingDataSource pooling = assertInstanceOf(ManagedPoolingDataSource.class, dataSource);
             Connection c = pooling.getConnection()) {
            assertFalse(c.isClosed());
        }
    }

    @Test
    public void minGreaterThanMaxFails() {
        CayenneDataSource.Builder builder = CayenneDataSource.of("jdbc:hsqldb:mem:cds_invalid")
                .driverClass(DRIVER);

        assertThrows(CayenneRuntimeException.class, () -> builder.pool(5, 2));
    }

    @Test
    public void negativeConnectionsFail() {
        CayenneDataSource.Builder builder = CayenneDataSource.of("jdbc:hsqldb:mem:cds_negative")
                .driverClass(DRIVER);

        assertThrows(CayenneRuntimeException.class, () -> builder.pool(-1, 2));
    }

    @Test
    public void poolSettingsWithoutPoolIgnored() {
        DataSource dataSource = CayenneDataSource.of("jdbc:hsqldb:mem:cds_no_pool")
                .driverClass(DRIVER)
                .validationQuery("SELECT 1")
                .build();

        assertInstanceOf(DriverDataSource.class, dataSource);
    }

    @Test
    public void driverResolvedFromUrl() throws Exception {
        DataSource dataSource = CayenneDataSource.of("jdbc:hsqldb:mem:cds_url_resolved")
                .userName("sa")
                .password("")
                .build();

        assertInstanceOf(DriverDataSource.class, dataSource);

        try (Connection c = dataSource.getConnection()) {
            assertFalse(c.isClosed());
        }
    }

    @Test
    public void unknownUrlWithoutDriverFails() {
        CayenneDataSource.Builder builder = CayenneDataSource.of("jdbc:nosuchdb:mem:cds");

        assertThrows(CayenneRuntimeException.class, builder::build);
    }

    @Test
    public void fromPropertiesNonPooling() throws Exception {
        Map<String, String> properties = Map.of(
                "cayenne.jdbc.url", "jdbc:hsqldb:mem:cds_props",
                "cayenne.jdbc.username", "sa",
                "cayenne.jdbc.password", "");

        DataSource dataSource = CayenneDataSource.fromProperties(properties).build();

        assertInstanceOf(DriverDataSource.class, dataSource);

        try (Connection c = dataSource.getConnection()) {
            assertFalse(c.isClosed());
        }
    }

    @Test
    public void fromPropertiesPooling() throws Exception {
        Map<String, String> properties = Map.of(
                "cayenne.jdbc.url", "jdbc:hsqldb:mem:cds_props_pool",
                "cayenne.jdbc.username", "sa",
                "cayenne.jdbc.password", "",
                "cayenne.jdbc.min_connections", "1",
                "cayenne.jdbc.max_connections", "2");

        DataSource dataSource = CayenneDataSource.fromProperties(properties).build();

        try (ManagedPoolingDataSource pooling = assertInstanceOf(ManagedPoolingDataSource.class, dataSource);
             Connection c = pooling.getConnection()) {
            assertFalse(c.isClosed());
        }
    }

    @Test
    public void fromPropertiesWithNodeSuffixAndFallback() throws Exception {
        Map<String, String> properties = Map.of(
                "cayenne.jdbc.url", "jdbc:hsqldb:mem:cds_props_base",
                "cayenne.jdbc.url.node1", "jdbc:hsqldb:mem:cds_props_node1",
                "cayenne.jdbc.username", "sa",
                "cayenne.jdbc.password", "");

        DataSource dataSource = CayenneDataSource.fromProperties(properties, "node1").build();

        assertInstanceOf(DriverDataSource.class, dataSource);

        try (Connection c = dataSource.getConnection()) {
            assertEquals("jdbc:hsqldb:mem:cds_props_node1", c.getMetaData().getURL());
        }
    }

    @Test
    public void fromPropertiesMissingUrlFails() {
        Map<String, String> properties = Map.of();

        assertThrows(CayenneRuntimeException.class, () -> CayenneDataSource.fromProperties(properties));
    }

    @Test
    public void fromPropertiesInvalidConnectionCountTreatedAsUnset() {
        Map<String, String> properties = Map.of(
                "cayenne.jdbc.url", "jdbc:hsqldb:mem:cds_props_bad_int",
                "cayenne.jdbc.username", "sa",
                "cayenne.jdbc.password", "",
                "cayenne.jdbc.min_connections", "not_a_number");

        DataSource dataSource = CayenneDataSource.fromProperties(properties).build();

        assertInstanceOf(DriverDataSource.class, dataSource);
    }

    @Test
    public void fromPropertiesNullNodeNameFails() {
        Map<String, String> properties = Map.of(
                "cayenne.jdbc.url", "jdbc:hsqldb:mem:cds_props");

        assertThrows(NullPointerException.class, () -> CayenneDataSource.fromProperties(properties, null));
    }

    @Test
    public void unknownDriverFails() {
        CayenneDataSource.Builder builder = CayenneDataSource.of("jdbc:example:none")
                .driverClass("com.example.NoSuchDriver");

        assertThrows(CayenneRuntimeException.class, builder::build);
    }
}
