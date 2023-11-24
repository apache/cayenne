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

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslatorFactory;
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
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.resource.ResourceLocator;

/**
 * DbAdapter implementation for <a href="http://www.sybase.com">Sybase RDBMS</a>.
 */
public class SybaseAdapter extends JdbcAdapter {

    public SybaseAdapter(@Inject RuntimeProperties runtimeProperties,
                         @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
                         @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
                         @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
                         @Inject(Constants.RESOURCE_LOCATOR) ResourceLocator resourceLocator,
                         @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator, valueObjectTypeRegistry);
        
		this.setSupportsGeneratedKeys(true);
    }

    @Override
    protected QuotingStrategy createQuotingStrategy() {
        return new DefaultQuotingStrategy("[", "]");
    }

    /**
     * @since 3.0
     */
    @Override
    protected EJBQLTranslatorFactory createEJBQLTranslatorFactory() {
        return new SybaseEJBQLTranslatorFactory();
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
            if (binding.getJdbcType() == Types.CLOB) {
                binding.setJdbcType(Types.VARCHAR);
            } else if (binding.getJdbcType() == Types.BLOB) {
                binding.setJdbcType(Types.VARBINARY);
            }
        }

        if (binding.getValue() == null && binding.getJdbcType() == 0) {
            statement.setNull(binding.getStatementPosition(), Types.VARCHAR);
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
