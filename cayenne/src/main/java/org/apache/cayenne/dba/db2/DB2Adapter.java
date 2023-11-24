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
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslatorFactory;
import org.apache.cayenne.access.translator.ejbql.JdbcEJBQLTranslatorFactory;
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
import org.apache.cayenne.resource.ResourceLocator;

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
            @Inject(Constants.RESOURCE_LOCATOR) ResourceLocator resourceLocator,
            @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator, valueObjectTypeRegistry);
        setSupportsGeneratedKeys(true);
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
     * @since 4.2
     */
    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        return new DB2SQLTreeProcessor();
    }

    /**
     * @since 4.0
     */
    @Override
    public EJBQLTranslatorFactory getEjbqlTranslatorFactory() {
        JdbcEJBQLTranslatorFactory translatorFactory = new DB2EJBQLTranslatorFactory();
        translatorFactory.setCaseInsensitive(caseInsensitiveCollations);
        return translatorFactory;
    }

    @Override
    public void bindParameter(PreparedStatement statement, ParameterBinding binding) throws Exception {
        if (binding.getValue() == null && (binding.getJdbcType() == 0 || binding.getJdbcType() == Types.BOOLEAN)) {
            statement.setNull(binding.getStatementPosition(), Types.VARCHAR);
        } else {
            binding.setJdbcType(convertNTypes(binding.getJdbcType()));
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
     * @since 4.0
     */
    private int convertNTypes(int sqlType) {
        switch (sqlType) {
            case Types.NCHAR:
                return Types.CHAR;
            case Types.NVARCHAR:
                return Types.VARCHAR;
            case Types.LONGNVARCHAR:
                return Types.LONGVARCHAR;
            case Types.NCLOB:
                return Types.CLOB;

            default:
                return sqlType;
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
