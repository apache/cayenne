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
package org.apache.cayenne.dbsync.reverse.db;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
* @since 4.0.
*/
public abstract class DbAttributesBaseLoader implements DbAttributesLoader {
    private final String catalog;
    private final String schema;

    private final DatabaseMetaData metaData;
    private final DbAdapter adapter;

    public DbAttributesBaseLoader(String catalog, String schema, DatabaseMetaData metaData, DbAdapter adapter) {
        this.catalog = catalog;
        this.schema = schema;
        this.metaData = metaData;
        this.adapter = adapter;
    }

    protected DbAttribute loadDbAttribute(Set<String> columns, ResultSet rs) throws SQLException {

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
        DbAttribute attr = adapter.buildAttribute(
                rs.getString("COLUMN_NAME"),
                rs.getString("TYPE_NAME"),
                columnType,
                rs.getInt("COLUMN_SIZE"),
                decimalDigits,
                rs.getBoolean("NULLABLE"));

        if (columns.contains("IS_AUTOINCREMENT")) {
            String autoIncrement = rs.getString("IS_AUTOINCREMENT");
            if ("YES".equals(autoIncrement)) {
                attr.setGenerated(true);
            }
        }
        return attr;
    }

    @Override
    public void loadDbAttributes(DbEntity entity) {
        for (DbAttribute attr : loadDbAttributes(entity.getName())) {
            attr.setEntity(entity);

            // override existing attributes if it comes again
            if (entity.getAttribute(attr.getName()) != null) {
                entity.removeAttribute(attr.getName());
            }
            entity.addAttribute(attr);
        }
    }

    protected abstract List<DbAttribute> loadDbAttributes(String tableName);

    protected String getCatalog() {
        return catalog;
    }

    protected String getSchema() {
        return schema;
    }

    protected DatabaseMetaData getMetaData() {
        return metaData;
    }
}
