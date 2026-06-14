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

package org.apache.cayenne.dba.db2;

import org.apache.cayenne.dba.NativeColumnType;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslator;
import org.apache.cayenne.access.translator.ejbql.JdbcEJBQLTranslator;
import org.apache.cayenne.access.types.BooleanType;
import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.JsonType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

/**
 * DbAdapter implementation for the DB2 RDBMS.
 */
public class DB2Adapter extends JdbcAdapter {

    private static final String FOR_BIT_DATA_SUFFIX = " FOR BIT DATA";

    public DB2Adapter(@Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, valueObjectTypeRegistry);
        setSupportsGeneratedKeys(true);
    }

    @Override
    protected NativeColumnType[] createNativeTypes() {
        return new NativeColumnType[]{
            NativeColumnType.of(Types.ARRAY, "ARRAY"),
            NativeColumnType.of(Types.BIGINT, "BIGINT"),
            NativeColumnType.of(Types.BINARY, "CHAR FOR BIT DATA"),
            NativeColumnType.of(Types.BIT, "SMALLINT"),
            NativeColumnType.of(Types.BLOB, "BLOB"),
            NativeColumnType.of(Types.BOOLEAN, "SMALLINT"),
            NativeColumnType.of(Types.CHAR, "CHAR"),
            NativeColumnType.of(Types.CLOB, "CLOB"),
            NativeColumnType.of(Types.DATALINK, "DATALINK"),
            NativeColumnType.of(Types.DATE, "DATE"),
            NativeColumnType.of(Types.DECIMAL, "DECIMAL"),
            NativeColumnType.of(Types.DOUBLE, "DOUBLE"),
            NativeColumnType.of(Types.FLOAT, "FLOAT"),
            NativeColumnType.of(Types.INTEGER, "INTEGER"),
            NativeColumnType.of(Types.JAVA_OBJECT, "JAVA_OBJECT"),
            NativeColumnType.of(Types.LONGNVARCHAR, "DBCLOB"),
            NativeColumnType.of(Types.LONGVARBINARY, "BLOB"),
            NativeColumnType.of(Types.LONGVARCHAR, "CLOB"),
            NativeColumnType.of(Types.NCHAR, "GRAPHIC"),
            NativeColumnType.of(Types.NCLOB, "NCLOB"),
            NativeColumnType.of(Types.NUMERIC, "DECIMAL"),
            NativeColumnType.of(Types.NVARCHAR, "VARGRAPHIC"),
            NativeColumnType.of(Types.OTHER, "OTHER"),
            NativeColumnType.of(Types.REAL, "REAL"),
            NativeColumnType.of(Types.REF, "REF"),
            NativeColumnType.of(Types.SMALLINT, "SMALLINT"),
            NativeColumnType.of(Types.STRUCT, "STRUCT"),
            NativeColumnType.of(Types.TIME, "TIME"),
            NativeColumnType.of(Types.TIMESTAMP, "TIMESTAMP"),
            NativeColumnType.of(Types.TINYINT, "SMALLINT"),
            NativeColumnType.of(Types.VARBINARY, "VARCHAR FOR BIT DATA"),
            NativeColumnType.of(Types.VARCHAR, "VARCHAR"),
        };
    }

    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        CharType charType = new CharType(true, true);
        map.registerType(charType);
        // configure boolean type to work with numeric columns
        map.registerType(new DB2BooleanType());

        map.registerType(new ByteArrayType(false, false));
        map.registerType(new JsonType(charType, true));
    }

    /**
     * @since 4.0
     */
    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        String type = preferredNativeColumnType(column).nativeType();

        sqlBuffer.append(quotingStrategy.quotedName(column)).append(' ');

        // DB2 GRAPHIC type that is used for NCHAR type length is in characters not in bytes
        // so divide max size by 2 and later restore the value
        int maxLength = column.getMaxLength();
        if(column.getType() == Types.NCHAR) {
            column.setMaxLength(maxLength / 2);
        }
        String length = sizeAndScale(this, column);
        column.setMaxLength(maxLength);

        // assemble...
        // note that max length for types like XYZ FOR BIT DATA must be entered in the
        // middle of type name, e.g. VARCHAR (100) FOR BIT DATA.
        int suffixIndex = type.indexOf(FOR_BIT_DATA_SUFFIX);
        if (!length.isEmpty() && suffixIndex > 0) {
            sqlBuffer.append(type.substring(0, suffixIndex)).append(length).append(FOR_BIT_DATA_SUFFIX);
        } else {
            sqlBuffer.append(type).append(" ").append(length);
        }

        if (column.isMandatory()) {
            sqlBuffer.append(" NOT NULL");
        }

        if (column.isGenerated()) {
            sqlBuffer.append(" GENERATED BY DEFAULT AS IDENTITY");
        }
    }

    /**
     * @since 4.0
     */
    @Override
    public boolean typeSupportsLength(int type) {
        return type == Types.LONGVARCHAR || type == Types.LONGVARBINARY || super.typeSupportsLength(type);
    }

    /**
     * @since 5.0
     */
    @Override
    public boolean typeSupportsScale(int type) {
        return type != Types.TIME && super.typeSupportsScale(type);
    }

    /**
     * @since 4.2
     */
    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        return new DB2SQLTreeProcessor();
    }

    /**
     * @since 5.0
     */
    @Override
    public EJBQLTranslator getEjbqlTranslator() {
        JdbcEJBQLTranslator translator = new DB2EJBQLTranslator();
        translator.setCaseInsensitive(caseInsensitiveCollations);
        return translator;
    }

    @Override
    public void bindParameter(PreparedStatement statement, ParameterBinding binding) throws Exception {
        if (binding.getValue() == null && (binding.getJdbcType() == 0 || binding.getJdbcType() == Types.BOOLEAN)) {
            statement.setNull(binding.getStatementPosition(), Types.VARCHAR);
        } else {
            super.bindParameter(statement, binding);
        }
    }
    
    /**
     * Uses special action builder to create the right action.
     * 
     * @since 3.1
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new DB2ActionBuilder(node));
    }

    /**
     * @since 5.0
     */
    @Override
    public int preferredBindingType(int jdbcType) {
        return switch (jdbcType) {
            case Types.NCHAR -> Types.CHAR;
            case Types.NVARCHAR -> Types.VARCHAR;
            case Types.LONGNVARCHAR -> Types.LONGVARCHAR;
            case Types.NCLOB -> Types.CLOB;
            default -> jdbcType;
        };
    }

    static final class DB2BooleanType extends BooleanType {
        @Override
        public void setJdbcObject(PreparedStatement st, Boolean val, int pos, int type, int precision) throws Exception {
            if (val != null) {
                st.setInt(pos, val ? 1 : 0);
            } else {
                st.setNull(pos, type);
            }
        }
    }
}
