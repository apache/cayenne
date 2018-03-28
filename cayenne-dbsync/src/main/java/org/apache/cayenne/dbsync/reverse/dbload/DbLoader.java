/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.dbsync.reverse.dbload;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.map.DataMap;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loads DB schema into a DataMap, creating DbEntities and Procedures. Consists of a list of specialized loaders that
 * iteratively load parts of metadata, such as Entity names, Attributes, Relationships, etc.
 *
 * @see AbstractLoader and its descendants
 * @since 4.0
 */
public class DbLoader {

    private List<AbstractLoader> loaders = new ArrayList<>();

    private final Connection connection;
    private final DbAdapter adapter;
    private final DbLoaderConfiguration config;
    private final DbLoaderDelegate delegate;
    private final ObjectNameGenerator nameGenerator;

    public DbLoader(DbAdapter adapter, Connection connection, DbLoaderConfiguration config,
                    DbLoaderDelegate delegate, ObjectNameGenerator nameGenerator) {
        this.adapter = Objects.requireNonNull(adapter);
        this.connection = Objects.requireNonNull(connection);
        this.config = Objects.requireNonNull(config);
        this.nameGenerator = Objects.requireNonNull(nameGenerator);
        this.delegate = delegate == null ? new DefaultDbLoaderDelegate() : delegate;

        createLoaders();
    }

    /**
     * Order of loaders is important, as loader can rely on data previously loaded
     */
    private void createLoaders() {
        loaders.add(new EntityLoader(adapter, config, delegate));
        loaders.add(new AttributeLoader(adapter, config, delegate));
        loaders.add(new PrimaryKeyLoader(config, delegate));
        loaders.add(new ExportedKeyLoader(config, delegate));
        loaders.add(new RelationshipLoader(config, delegate, nameGenerator));
        loaders.add(new ProcedureLoader(adapter, config, delegate));
        loaders.add(new ProcedureColumnLoader(adapter, config, delegate));
    }

    /**
     * @return new DataMap with data loaded from DB
     */
    public DataMap load() throws SQLException {
        DbLoadDataStore loadedData = new DbLoadDataStore();
        DatabaseMetaData metaData = connection.getMetaData();

        for(AbstractLoader loader : loaders) {
            loader.load(metaData, loadedData);
        }
        return loadedData;
    }

    //// Utility methods that better be moved somewhere ////

    /**
     * Retrieves catalogs for a given connection.
     * using a static method for catalog loading as we don't need a full DbLoader for this operation
     * @return List with the catalog names; empty list if none found.
     */
    public static List<String> loadCatalogs(Connection connection) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getCatalogs()) {
            return getStrings(rs, 1);
        }
    }

    /**
     * Retrieves the schemas for the given connection.
     * using a static method for catalog loading as we don't need a full DbLoader for this operation
     * @return List with the schema names; empty list if none found.
     */
    public static List<String> loadSchemas(Connection connection) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getSchemas()) {
            return getStrings(rs, 1);
        }
    }

    private static List<String> getStrings(ResultSet rs, int columnIndex) throws SQLException {
        List<String> strings = new ArrayList<>();
        while (rs.next()) {
            strings.add(rs.getString(columnIndex));
        }
        return strings;
    }
}
