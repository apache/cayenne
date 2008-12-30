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
package org.apache.cayenne.access.select;

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

/**
 * @since 3.0
 */
class EntitySelectColumn implements SelectColumn {

    private int jdbcType;
    private String dataRowKey;
    private ExtendedType converter;

    private String columnName;
    private List<DbRelationship> path;

    public int getJdbcType() {
        return jdbcType;
    }

    public String getColumnName(DbEntity unionRoot, String tableAlias) {
        if (tableAlias == null || tableAlias.length() == 0) {
            return columnName;
        }

        return tableAlias + '.' + columnName;
    }

    public String getDataRowKey() {
        return dataRowKey;
    }

    public List<DbRelationship> getPath(DbEntity unionRoot) {
        if (path == null) {
            return Collections.emptyList();
        }

        return path;
    }

    ExtendedType getConverter() {
        return converter;
    }

    void setJdbcType(int jdbcType) {
        this.jdbcType = jdbcType;
    }

    void setPath(List<DbRelationship> path) {
        this.path = path;
    }

    void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    void setDataRowKey(String dataRowKey) {
        this.dataRowKey = dataRowKey;
    }

    void setConverter(ExtendedType converter) {
        this.converter = converter;
    }
}
