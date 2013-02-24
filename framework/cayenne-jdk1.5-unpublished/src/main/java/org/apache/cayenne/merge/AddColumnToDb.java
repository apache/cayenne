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

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

public class AddColumnToDb extends AbstractToDbToken.EntityAndColumn {

    public AddColumnToDb(DbEntity entity, DbAttribute column) {
        super(entity, column);
    }

    /**
     * append the part of the token before the actual column data type
     */
    protected void appendPrefix(StringBuffer sqlBuffer, QuotingStrategy context) {

        sqlBuffer.append("ALTER TABLE ");
        sqlBuffer.append(context.quotedFullyQualifiedName(getEntity()));
        sqlBuffer.append(" ADD COLUMN ");
        sqlBuffer.append(context.quotedName(getColumn()));
        sqlBuffer.append(" ");
    }

    @Override
    public List<String> createSql(DbAdapter adapter) {
        StringBuffer sqlBuffer = new StringBuffer();
        QuotingStrategy context = adapter.getQuotingStrategy();
        appendPrefix(sqlBuffer, context);

        // copied from JdbcAdapter.createTableAppendColumn
        String[] types = adapter.externalTypesForJdbcType(getColumn().getType());
        if (types == null || types.length == 0) {
            String entityName = getColumn().getEntity() != null ? ((DbEntity) getColumn()
                    .getEntity()).getFullyQualifiedName() : "<null>";
            throw new CayenneRuntimeException("Undefined type for attribute '"
                    + entityName
                    + "."
                    + getColumn().getName()
                    + "': "
                    + getColumn().getType());
        }

        String type = types[0];
        sqlBuffer.append(type);

        // append size and precision (if applicable)
        if (TypesMapping.supportsLength(getColumn().getType())) {
            int len = getColumn().getMaxLength();
            int scale = TypesMapping.isDecimal(getColumn().getType()) ? getColumn()
                    .getScale() : -1;

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

        return Collections.singletonList(sqlBuffer.toString());
    }

    public String getTokenName() {
        return "Add Column";
    }

    public MergerToken createReverse(MergerFactory factory) {
        return factory.createDropColumnToModel(getEntity(), getColumn());
    }

}
