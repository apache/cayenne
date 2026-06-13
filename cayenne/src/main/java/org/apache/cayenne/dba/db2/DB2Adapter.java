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

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
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
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import java.util.HashMap;
import java.util.Map;

/**
 * DbAdapter implementation for the <a href="http://www.ibm.com/db2/"> DB2 RDBMS </a>.
 * Sample connection settings to use with DB2 are shown below:
 * 
 * <pre>
 *       test-db2.jdbc.username = test
 *       test-db2.jdbc.password = secret
 *       test-db2.jdbc.url = jdbc:db2://servername:50000/databasename
 *       test-db2.jdbc.driver = com.ibm.db2.jcc.DB2Driver
 * </pre>
 */
public class DB2Adapter extends JdbcAdapter {

    private static final String FOR_BIT_DATA_SUFFIX = " FOR BIT DATA";

    private static final String TRIM_FUNCTION = "RTRIM";

    public DB2Adapter(@Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, valueObjectTypeRegistry);
        setSupportsGeneratedKeys(true);
    }

    @Override
    protected Map<Integer, String[]> createExternalTypes() {
        Map<Integer, String[]> types = new HashMap<>();
        types.put(Types.ARRAY, new String[]{"ARRAY"});
        types.put(Types.BIGINT, new String[]{"BIGINT"});
        types.put(Types.BINARY, new String[]{"CHAR FOR BIT DATA"});
        types.put(Types.BIT, new String[]{"SMALLINT"});
        types.put(Types.BLOB, new String[]{"BLOB"});
        types.put(Types.BOOLEAN, new String[]{"SMALLINT"});
        types.put(Types.CHAR, new String[]{"CHAR"});
        types.put(Types.CLOB, new String[]{"CLOB"});
        types.put(Types.DATALINK, new String[]{"DATALINK"});
        types.put(Types.DATE, new String[]{"DATE"});
        types.put(Types.DECIMAL, new String[]{"DECIMAL"});
        types.put(Types.DOUBLE, new String[]{"DOUBLE"});
        types.put(Types.FLOAT, new String[]{"FLOAT"});
        types.put(Types.INTEGER, new String[]{"INTEGER"});
        types.put(Types.JAVA_OBJECT, new String[]{"JAVA_OBJECT"});
        types.put(Types.LONGNVARCHAR, new String[]{"DBCLOB"});
        types.put(Types.LONGVARBINARY, new String[]{"BLOB"});
        types.put(Types.LONGVARCHAR, new String[]{"CLOB"});
        types.put(Types.NCHAR, new String[]{"GRAPHIC"});
        types.put(Types.NCLOB, new String[]{"NCLOB"});
        types.put(Types.NUMERIC, new String[]{"DECIMAL"});
        types.put(Types.NVARCHAR, new String[]{"VARGRAPHIC"});
        types.put(Types.OTHER, new String[]{"OTHER"});
        types.put(Types.REAL, new String[]{"REAL"});
        types.put(Types.REF, new String[]{"REF"});
        types.put(Types.SMALLINT, new String[]{"SMALLINT"});
        types.put(Types.STRUCT, new String[]{"STRUCT"});
        types.put(Types.TIME, new String[]{"TIME"});
        types.put(Types.TIMESTAMP, new String[]{"TIMESTAMP"});
        types.put(Types.TINYINT, new String[]{"SMALLINT"});
        types.put(Types.VARBINARY, new String[]{"VARCHAR FOR BIT DATA"});
        types.put(Types.VARCHAR, new String[]{"VARCHAR"});
        return types;
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
        String type = getType(this, column);

        sqlBuffer.append(quotingStrategy.quotedName(column)).append(' ');

        // DB2 GRAPHIC type that is used for NCHAR type length is in characters not in bytes
        // so divide max size by 2 and later restore the value
        int maxLength = column.getMaxLength();
        if(column.getType() == Types.NCHAR) {
            column.setMaxLength(maxLength / 2);
        }
        String length = sizeAndPrecision(this, column);
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
        switch (jdbcType) {
            case Types.NCHAR:
                return Types.CHAR;
            case Types.NVARCHAR:
                return Types.VARCHAR;
            case Types.LONGNVARCHAR:
                return Types.LONGVARCHAR;
            case Types.NCLOB:
                return Types.CLOB;

            default:
                return jdbcType;
        }
    }

    final class DB2BooleanType extends BooleanType {
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
