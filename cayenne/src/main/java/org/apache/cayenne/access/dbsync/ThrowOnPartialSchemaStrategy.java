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
package org.apache.cayenne.access.dbsync;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @since 3.0
 */
public class ThrowOnPartialSchemaStrategy implements SchemaUpdateStrategy {

    private final static Logger LOGGER = LoggerFactory.getLogger(ThrowOnPartialSchemaStrategy.class);

    @Override
    public void updateSchema(DbAdapter adapter, DataSource dataSource, Collection<DataMap> dataMaps, SQLLogger logger)
            throws SQLException {

        Collection<DbEntity> entities = dbEntities(dataMaps);
        SchemaAnalyzer analyzer = analyze(dataSource, entities);

        if (analyzer.getTableNoInDB().isEmpty() && analyzer.getErrorMessage() == null) {
            LOGGER.info("Full schema is present");
        } else {
            LOGGER.info("Error - missing or partial schema detected");
            throw new CayenneRuntimeException(mismatchMessage(analyzer, entities.size()));
        }
    }

    static Collection<DbEntity> dbEntities(Collection<DataMap> dataMaps) {
        Collection<DbEntity> entities = new ArrayList<>();
        for (DataMap map : dataMaps) {
            entities.addAll(map.getDbEntities());
        }
        return entities;
    }

    static SchemaAnalyzer analyze(DataSource dataSource, Collection<DbEntity> entities) {

        SchemaAnalyzer analyzer = new SchemaAnalyzer();

        List<String> schemas = new ArrayList<>();
        DatabaseMetaData md = null;
        try {
            try (Connection connection = dataSource.getConnection()) {
                md = connection.getMetaData();

                try (ResultSet rs = md.getSchemas()) {
                    while (rs.next()) {
                        String schemaName = rs.getString(1);
                        schemas.add(schemaName);
                    }
                }

                analyzer.analyzeSchemas(schemas, md);
            }
        } catch (Exception e) {
            LOGGER.debug("Exception analyzing schema, ignoring", e);
        }

        boolean isIncluded = analyzer.compareTables(md, entities);

        if (isIncluded && analyzer.getErrorMessage() == null) {
            try {
                analyzer.compareColumns(md);
            } catch (SQLException e) {
                LOGGER.debug("Exception analyzing schema, ignoring", e);
            }
        }

        return analyzer;
    }

    static String mismatchMessage(SchemaAnalyzer analyzer, int entityCount) {
        StringBuilder buffer = new StringBuilder("Schema mismatch detected");

        List<String> missingTables = analyzer.getTableNoInDB();
        if (analyzer.getErrorMessage() != null) {
            buffer.append(": ").append(analyzer.getErrorMessage());
        } else if (missingTables.size() == entityCount) {
            buffer.append(": no schema found");
        } else if (!missingTables.isEmpty()) {
            buffer.append(": missing table '").append(missingTables.getFirst()).append('\'');
        }

        return buffer.toString();
    }
}
