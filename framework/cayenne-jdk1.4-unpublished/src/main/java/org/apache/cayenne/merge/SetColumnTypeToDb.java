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

/**
 * An {@link MergerToken} to use to set type, length and precision.
 */
public class SetColumnTypeToDb extends AbstractToDbToken {

    private DbEntity entity;
    private DbAttribute columnOriginal;
    private DbAttribute columnNew;

    public SetColumnTypeToDb(DbEntity entity, DbAttribute columnOriginal, DbAttribute columnNew) {
        this.entity = entity;
        this.columnOriginal = columnOriginal;
        this.columnNew = columnNew;
    }
    
    /**
     * append the part of the token before the actual column data type
     */
    protected void appendPrefix(StringBuffer sqlBuffer) {
        sqlBuffer.append("ALTER TABLE ");
        sqlBuffer.append(entity.getFullyQualifiedName());
        sqlBuffer.append(" ALTER ");
        sqlBuffer.append(columnNew.getName());
        sqlBuffer.append(" TYPE ");
    }

    public String createSql(DbAdapter adapter) {
        StringBuffer sqlBuffer = new StringBuffer();

        appendPrefix(sqlBuffer);

        // copied from JdbcAdapter.createTableAppendColumn
        String[] types = adapter.externalTypesForJdbcType(columnNew.getType());
        if (types == null || types.length == 0) {
            String entityName = columnNew.getEntity() != null ? ((DbEntity) columnNew
                    .getEntity()).getFullyQualifiedName() : "<null>";
            throw new CayenneRuntimeException("Undefined type for attribute '"
                    + entityName
                    + "."
                    + columnNew.getName()
                    + "': "
                    + columnNew.getType());
        }

        String type = types[0];
        sqlBuffer.append(type);

        // append size and precision (if applicable)
        if (TypesMapping.supportsLength(columnNew.getType())) {
            int len = columnNew.getMaxLength();
            int scale = TypesMapping.isDecimal(columnNew.getType()) ? columnNew.getScale() : -1;

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

        return sqlBuffer.toString();
    }

    public String getTokenName() {
        return "Set Column Type";
    }
    
    public String getTokenValue() {
        // TODO: ..varchar(100)
        return entity.getName() + "." + columnNew.getName();
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createSetColumnTypeToModel(entity, columnNew, columnOriginal);
    }


}
