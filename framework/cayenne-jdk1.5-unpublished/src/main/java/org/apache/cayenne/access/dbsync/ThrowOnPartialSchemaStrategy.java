/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.access.dbsync;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DbEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @since 3.0
 */
public class ThrowOnPartialSchemaStrategy extends BaseSchemaUpdateStrategy {

    final Log log = LogFactory.getLog(ThrowOnPartialSchemaStrategy.class);

    /**
     * @since 3.0
     */
    @Override
    public void generateUpdateSchema(DataNode dataNode) {

        SchemaAnalyzer analyzer = new SchemaAnalyzer();

        List<String> schemas = new ArrayList<String>();
        DatabaseMetaData md = null;
        try {
            Connection connection = dataNode.getDataSource().getConnection();
            md = connection.getMetaData();
            ResultSet rs = md.getSchemas();

            try {
                while (rs.next()) {
                    String schema_name = rs.getString(1);
                    schemas.add(schema_name);
                }
            }
            finally {
                rs.close();
            }
            connection.close();
            analyzer.analyzeSchemas(schemas, md);
        }
        catch (Exception e) {
            log.debug("Exception analyzing schema, ignoring", e);
        }

        Collection<DbEntity> entities = dataNode.getEntityResolver().getDbEntities();

        boolean isIncluded = analyzer.compareTables(md, entities);

        if (isIncluded && analyzer.getErrorMessage() == null) {
            try {
                analyzer.compareColumns(md);
            }
            catch (SQLException e) {
                log.debug("Exception analyzing schema, ignoring", e);
            }
        }
        analyze(dataNode, analyzer.getTableNoInDB(), analyzer.getErrorMessage(), entities
                .size());
    }

    protected void analyze(
            DataNode dataNode,
            List<String> mergerOnlyTable,
            String errorMessage,
            int entitiesSize) {

        if (mergerOnlyTable.size() == 0 && errorMessage == null) {
        }
        else {
            String err = "Partial schema detected: ";
            if (errorMessage != null) {
                err += errorMessage;
            }
            else if (mergerOnlyTable.size() == entitiesSize) {
                err += "no schema in database";
            }
            else {
                if (mergerOnlyTable.size() > 0) {
                    err += "expect table " + mergerOnlyTable.get(0);
                }
            }
            throw new CayenneRuntimeException(err);
        }
    }
}
