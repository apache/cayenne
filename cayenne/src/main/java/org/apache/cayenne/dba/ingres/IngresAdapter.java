/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.dba.ingres;

import org.apache.cayenne.dba.NativeColumnType;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

/**
 * DbAdapter implementation for Ingres RDBMS.
 */
public class IngresAdapter extends JdbcAdapter {

    public IngresAdapter(@Inject RuntimeProperties runtimeProperties,
                         @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
                         @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
                         @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
                         @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, valueObjectTypeRegistry);
        setSupportsUniqueConstraints(true);
        setSupportsGeneratedKeys(true);
    }

    @Override
    protected NativeColumnType[] createNativeTypes() {
        return new NativeColumnType[]{
            NativeColumnType.of(Types.ARRAY, "ARRAY"),
            NativeColumnType.of(Types.BIGINT, "BIGINT"),
            NativeColumnType.of(Types.BINARY, "BYTE"),
            NativeColumnType.of(Types.BIT, "TINYINT"),
            NativeColumnType.of(Types.BLOB, "BLOB"),
            NativeColumnType.of(Types.BOOLEAN, "BOOLEAN"),
            NativeColumnType.of(Types.CHAR, "CHAR"),
            NativeColumnType.of(Types.CLOB, "CLOB"),
            NativeColumnType.of(Types.DATALINK, "DATALINK"),
            NativeColumnType.of(Types.DATE, "DATE"),
            NativeColumnType.of(Types.DECIMAL, "DECIMAL"),
            NativeColumnType.of(Types.DOUBLE, "FLOAT"),
            NativeColumnType.of(Types.FLOAT, "FLOAT"),
            NativeColumnType.of(Types.INTEGER, "INTEGER"),
            NativeColumnType.of(Types.JAVA_OBJECT, "JAVA_OBJECT"),
            NativeColumnType.of(Types.LONGNVARCHAR, "LONG NVARCHAR"),
            NativeColumnType.of(Types.LONGVARBINARY, "LONG BYTE"),
            NativeColumnType.of(Types.LONGVARCHAR, "LONG VARCHAR"),
            NativeColumnType.of(Types.NCHAR, "NCHAR"),
            NativeColumnType.of(Types.NCLOB, "LONG NVARCHAR"),
            NativeColumnType.of(Types.NUMERIC, "NUMERIC"),
            NativeColumnType.of(Types.NVARCHAR, "NVARCHAR"),
            NativeColumnType.of(Types.OTHER, "OTHER"),
            NativeColumnType.of(Types.REAL, "REAL"),
            NativeColumnType.of(Types.REF, "REF"),
            NativeColumnType.of(Types.SMALLINT, "SMALLINT"),
            NativeColumnType.of(Types.TIME, "TIME"),
            NativeColumnType.of(Types.TIMESTAMP, "TIMESTAMP"),
            NativeColumnType.of(Types.TINYINT, "TINYINT"),
            NativeColumnType.of(Types.VARBINARY, "BYTE VARYING"),
            NativeColumnType.of(Types.VARCHAR, "VARCHAR"),
        };
    }

    /**
     * @since 4.2
     */
    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        return new IngressSQLTreeProcessor();
    }

    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new IngresActionBuilder(node));
    }

    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);
        map.registerType(new IngresCharType());

        // configure boolean type to work with numeric columns
        map.registerType(new IngresBooleanType());
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected void bind(PreparedStatement statement, Object value, int psPosition, int psType, int psScale,
                        ExtendedType binder) throws Exception {
        if (value == null && (psType == Types.BIT)) {
            statement.setNull(psPosition, Types.SMALLINT);
        } else {
            super.bind(statement, value, psPosition, psType, psScale, binder);
        }
    }

    @Override
    public void createTableAppendColumn(StringBuffer buf, DbAttribute column) {
        String type = preferredNativeColumnType(column).nativeType();
        QuotingStrategy quotes = getQuotingStrategy(column.getEntity());
        quotes.appendStart(buf);
        buf.append(column.getName());
        quotes.appendEnd(buf);
        buf.append(' ').append(type);

        // append size and precision (if applicable)
        if (typeSupportsLength(column.getType())) {
            int len = column.getMaxLength();
            int scale = TypesMapping.isDecimal(column.getType()) ? column.getScale() : -1;

            // sanity check
            if (scale > len) {
                scale = -1;
            }

            if (len > 0) {
                buf.append('(').append(len);

                if (scale >= 0) {
                    buf.append(", ").append(scale);
                }

                buf.append(')');
            }
        }

        if (column.isGenerated()) {
            buf.append(" GENERATED BY DEFAULT AS IDENTITY ");
        }

        // Ingres does not like "null" for non mandatory fields
        if (column.isMandatory()) {
            buf.append(" NOT NULL");
        }
    }
}
