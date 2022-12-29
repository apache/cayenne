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

package org.apache.cayenne.dbsync.reverse.dbload;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.dbsync.model.DetectedDbAttribute;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Helper class to process attribute data {@link ResultSet}
 *
 * @since 5.0
 */
class AttributeProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbLoader.class);

    private final DbAdapter adapter;
    boolean firstRow;
    boolean supportAutoIncrement;

    AttributeProcessor(DbAdapter adapter) {
        this.adapter = adapter;
        this.firstRow = true;
    }

    private boolean checkForAutoIncrement(ResultSet rs) throws SQLException {
        ResultSetMetaData rsMetaData = rs.getMetaData();
        for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
            if ("IS_AUTOINCREMENT".equals(rsMetaData.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }

    void processAttribute(ResultSet rs, PatternFilter columnFilter, DbEntity entity) throws SQLException {
        if(firstRow) {
            supportAutoIncrement = checkForAutoIncrement(rs);
            firstRow = false;
        }

        String columnName = rs.getString("COLUMN_NAME");
        if (columnFilter == null || !columnFilter.isIncluded(columnName)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Skip column '" + entity.getName() + "." + columnName +
                        "' (Path: " + entity.getCatalog() + "/" + entity.getSchema() + "; Filter: " + columnFilter + ")");
            }
            return;
        }

        DbAttribute attribute = createDbAttribute(rs);
        addToDbEntity(entity, attribute);
    }

    private void addToDbEntity(DbEntity entity, DbAttribute attribute) {
        attribute.setEntity(entity);

        // override existing attributes if it comes again
        if (entity.getAttribute(attribute.getName()) != null) {
            entity.removeAttribute(attribute.getName());
        }
        entity.addAttribute(attribute);
    }

    private DbAttribute createDbAttribute(ResultSet rs) throws SQLException {

        // gets attribute's (column's) information
        int columnType = rs.getInt("DATA_TYPE");

        // ignore precision of non-decimal columns
        int decimalDigits = -1;
        if (TypesMapping.isDecimal(columnType)) {
            decimalDigits = rs.getInt("DECIMAL_DIGITS");
            if (rs.wasNull()) {
                decimalDigits = -1;
            }
        }

        // create attribute delegating this task to adapter
        DetectedDbAttribute detectedDbAttribute = new DetectedDbAttribute(adapter.buildAttribute(
                rs.getString("COLUMN_NAME"),
                rs.getString("TYPE_NAME"),
                columnType,
                rs.getInt("COLUMN_SIZE"),
                decimalDigits,
                rs.getBoolean("NULLABLE")));

        // store raw type name
        detectedDbAttribute.setJdbcTypeName(rs.getString("TYPE_NAME"));

        if (supportAutoIncrement) {
            if ("YES".equals(rs.getString("IS_AUTOINCREMENT"))) {
                detectedDbAttribute.setGenerated(true);
            }
        }

        return detectedDbAttribute;
    }

}