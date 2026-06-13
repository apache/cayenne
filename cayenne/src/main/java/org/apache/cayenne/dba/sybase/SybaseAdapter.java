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

package org.apache.cayenne.dba.sybase;

import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslator;
import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.ByteType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.ShortType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.DefaultQuotingStrategy;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DbAdapter implementation for <a href="http://www.sybase.com">Sybase RDBMS</a>.
 */
public class SybaseAdapter extends JdbcAdapter {

    public SybaseAdapter(@Inject RuntimeProperties runtimeProperties,
                         @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
                         @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
                         @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
                         @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, valueObjectTypeRegistry);
        
		this.setSupportsGeneratedKeys(true);
    }

    @Override
    protected Map<Integer, String[]> createExternalTypes() {
        Map<Integer, String[]> types = new HashMap<>();
        types.put(Types.BIGINT, new String[]{"decimal(19,0)"});
        types.put(Types.BINARY, new String[]{"binary"});
        types.put(Types.BIT, new String[]{"bit"});
        types.put(Types.BLOB, new String[]{"image"});
        types.put(Types.BOOLEAN, new String[]{"bit"});
        types.put(Types.CHAR, new String[]{"char"});
        types.put(Types.CLOB, new String[]{"text"});
        types.put(Types.DATE, new String[]{"datetime"});
        types.put(Types.DECIMAL, new String[]{"decimal"});
        types.put(Types.DOUBLE, new String[]{"double precision"});
        types.put(Types.FLOAT, new String[]{"float"});
        types.put(Types.INTEGER, new String[]{"int"});
        types.put(Types.LONGNVARCHAR, new String[]{"text"});
        types.put(Types.LONGVARBINARY, new String[]{"image"});
        types.put(Types.LONGVARCHAR, new String[]{"text"});
        types.put(Types.NCHAR, new String[]{"nchar"});
        types.put(Types.NCLOB, new String[]{"text"});
        types.put(Types.NUMERIC, new String[]{"numeric"});
        types.put(Types.NVARCHAR, new String[]{"nvarchar"});
        types.put(Types.REAL, new String[]{"real"});
        types.put(Types.SMALLINT, new String[]{"smallint"});
        types.put(Types.SQLXML, new String[]{"text"});
        types.put(Types.TIME, new String[]{"datetime"});
        types.put(Types.TIMESTAMP, new String[]{"datetime"});
        types.put(Types.TINYINT, new String[]{"tinyint"});
        types.put(Types.VARBINARY, new String[]{"varbinary"});
        types.put(Types.VARCHAR, new String[]{"varchar"});
        return types;
    }

    @Override
    protected QuotingStrategy createQuotingStrategy() {
        return new DefaultQuotingStrategy("[", "]");
    }

    /**
     * @since 3.0
     */
    @Override
    protected EJBQLTranslator createEJBQLTranslator() {
        return new SybaseEJBQLTranslator();
    }

    /**
     * @since 4.2
     */
    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        return new SybaseSQLTreeProcessor();
    }

    /**
     * Returns word "go".
     * 
     * @since 1.0.4
     */
    @Override
    public String getBatchTerminator() {
        return "go";
    }

    /**
     * Installs appropriate ExtendedTypes as converters for passing values
     * between JDBC and Java layers.
     */
    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        map.registerType(new CharType(true, false));

        // create specially configured ByteArrayType handler
        map.registerType(new ByteArrayType(true, false));

        // address Sybase driver inability to handle java.lang.Short and
        // java.lang.Byte
        map.registerType(new ShortType(true));
        map.registerType(new ByteType(true));
    }

    @Override
    public void bindParameter(PreparedStatement statement, ParameterBinding binding) throws Exception {

        // Sybase driver doesn't like CLOBs and BLOBs as parameters
        if (binding.getValue() == null) {
            int jdbcType = switch (binding.getJdbcType()) {
                case Types.CLOB, 0 -> Types.VARCHAR;
                case Types.BLOB -> Types.VARBINARY;
                default -> binding.getJdbcType();
            };
            statement.setNull(binding.getStatementPosition(), jdbcType);
        } else {
            super.bindParameter(statement, binding);
        }
    }

    /**
     * Overrides super implementation to correctly set up identity columns.
     *
     * @since 1.2
     * @since 4.1 moved from SQLServerAdapter to SybaseAdapter as it supports this too
     */
    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {

        super.createTableAppendColumn(sqlBuffer, column);

        if (column.isGenerated()) {
            // current limitation - we don't allow to set identity parameters...
            sqlBuffer.append(" IDENTITY (1, 1)");
        }
    }
}
