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
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DbEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ThrowOnPartialSchemaStrategy extends BaseSchemaUpdateStrategy {

    private final static Logger LOGGER = LoggerFactory.getLogger(ThrowOnPartialSchemaStrategy.class);


    @Override
    protected void processSchemaUpdate(DataNode dataNode) throws SQLException {

        SchemaAnalyzer analyzer = new SchemaAnalyzer();

        List<String> schemas = new ArrayList<>();
        DatabaseMetaData md = null;
        try {

            try (Connection connection = dataNode.getDataSource().getConnection();) {
                md = connection.getMetaData();

                try (ResultSet rs = md.getSchemas();) {
                    while (rs.next()) {
                        String schemaName = rs.getString(1);
                        schemas.add(schemaName);
                    }
                }
            }

            analyzer.analyzeSchemas(schemas, md);
        } catch (Exception e) {
            LOGGER.debug("Exception analyzing schema, ignoring", e);
        }

        Collection<DbEntity> entities = dataNode.getEntityResolver().getDbEntities();

        boolean isIncluded = analyzer.compareTables(md, entities);

        if (isIncluded && analyzer.getErrorMessage() == null) {
            try {
                analyzer.compareColumns(md);
            } catch (SQLException e) {
                LOGGER.debug("Exception analyzing schema, ignoring", e);
            }
        }

        processSchemaUpdate(dataNode, analyzer.getTableNoInDB(), analyzer.getErrorMessage(), entities.size());
    }

    protected void processSchemaUpdate(DataNode dataNode, List<String> mergerOnlyTable, String errorMessage, int entitiesSize)
            throws SQLException {

        if (mergerOnlyTable.size() == 0 && errorMessage == null) {
            LOGGER.info("Full schema is present");
        } else {
            LOGGER.info("Error - missing or partial schema detected");
            StringBuilder buffer = new StringBuilder("Schema mismatch detected");

            if (errorMessage != null) {
                buffer.append(": ").append(errorMessage);
            } else if (mergerOnlyTable.size() == entitiesSize) {
                buffer.append(": no schema found");
            } else {
                if (mergerOnlyTable.size() > 0) {
                    buffer.append(": missing table '").append(mergerOnlyTable.get(0)).append('\'');
                }
            }

            throw new CayenneRuntimeException(buffer.toString());
        }
    }
}
