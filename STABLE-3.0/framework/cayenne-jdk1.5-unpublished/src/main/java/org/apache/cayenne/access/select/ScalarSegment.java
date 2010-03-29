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

import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

/**
 * Describes a scalar segment of the result consisting of a single column.
 * 
 * @since 3.0
 */
class ScalarSegment implements SelectDescriptor<Object>, SelectColumn {

    private String columnName;
    private RowReader<Object> rowReader;

    ScalarSegment(String columnName, ExtendedType converter) {
        this.columnName = columnName;
        this.rowReader = new ScalarRowReader(converter, TypesMapping.NOT_DEFINED);
    }

    public List<SelectColumn> getColumns() {
        return Collections.<SelectColumn> singletonList(this);
    }

    public RowReader<Object> getRowReader(ResultSet resultSet) {
        return rowReader;
    }

    public String getColumnName(DbEntity unionRoot, String tableAlias) {
        if (tableAlias == null || tableAlias.length() == 0) {
            return columnName;
        }

        return tableAlias + '.' + columnName;
    }

    public int getJdbcType() {
        return TypesMapping.NOT_DEFINED;
    }

    public String getDataRowKey() {
        throw new UnsupportedOperationException(
                "'dataRowKey' is meaningless for Scalar segments");
    }

    public List<DbRelationship> getPath(DbEntity unionRoot) {
        throw new UnsupportedOperationException(
                "'getPath' is unsupported for Scalar segments");
    }
}
