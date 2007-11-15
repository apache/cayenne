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
package org.apache.cayenne.merge;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

public class AddColumnToDb extends AbstractToDbToken {

    private DbEntity entity;
    private DbAttribute column;

    public AddColumnToDb(DbEntity entity, DbAttribute column) {
        this.entity = entity;
        this.column = column;
    }

    /**
     * append the part of the token before the actual column data type
     */
    protected void appendPrefix(StringBuffer sqlBuffer) {
        sqlBuffer.append("ALTER TABLE ");
        sqlBuffer.append(entity.getFullyQualifiedName());
        sqlBuffer.append(" ADD COLUMN ");
        sqlBuffer.append(column.getName());
        sqlBuffer.append(" ");
    }

    public String createSql(DbAdapter adapter) {
        StringBuffer sqlBuffer = new StringBuffer();

        appendPrefix(sqlBuffer);

        // copied from JdbcAdapter.createTableAppendColumn
        String[] types = adapter.externalTypesForJdbcType(column.getType());
        if (types == null || types.length == 0) {
            String entityName = column.getEntity() != null ? ((DbEntity) column
                    .getEntity()).getFullyQualifiedName() : "<null>";
            throw new CayenneRuntimeException("Undefined type for attribute '"
                    + entityName
                    + "."
                    + column.getName()
                    + "': "
                    + column.getType());
        }

        String type = types[0];
        sqlBuffer.append(type);

        // append size and precision (if applicable)
        if (TypesMapping.supportsLength(column.getType())) {
            int len = column.getMaxLength();
            int scale = TypesMapping.isDecimal(column.getType()) ? column.getScale() : -1;

            // sanity check
            if (scale > len) {
                scale = -1;
            }

            if (len > 0) {
                sqlBuffer.append('(').append(len);

                if (scale >= 0) {
                    sqlBuffer.append(", ").append(scale);
                }

                sqlBuffer.append(')');
            }
        }

        // use separate token to set value and not null if needed
        // sqlBuffer.append(" NULL");

        return sqlBuffer.toString();
    }

    public String getTokenName() {
        return "Add Column";
    }

    public String getTokenValue() {
        return entity.getName() + "." + column.getName();
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createDropColumToModel(entity, column);
    }

}
