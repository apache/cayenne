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

package org.apache.cayenne.dba.sybase;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslatorFactory;
import org.apache.cayenne.access.translator.select.QualifierTranslator;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.access.translator.select.SelectTranslator;
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
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.resource.ResourceLocator;

/**
 * DbAdapter implementation for <a href="http://www.sybase.com">Sybase
 * RDBMS</a>.
 */
public class SybaseAdapter extends JdbcAdapter {

    public SybaseAdapter(@Inject RuntimeProperties runtimeProperties,
                         @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
                         @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
                         @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
                         @Inject(Constants.SERVER_RESOURCE_LOCATOR) ResourceLocator resourceLocator,
                         @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator, valueObjectTypeRegistry);
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
	 * @since 4.0
	 */
	@Override
	public SelectTranslator getSelectTranslator(SelectQuery<?> query, EntityResolver entityResolver) {
		return new SybaseSelectTranslator(query, this, entityResolver);
	}

    /**
     * @since 4.0
     */
    @Override
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new SybaseQualifierTranslator(queryAssembler);
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

    /**
     * Creates and returns a primary key generator. Overrides superclass
     * implementation to return an instance of SybasePkGenerator.
     */
    @Override
    protected PkGenerator createPkGenerator() {
        return new SybasePkGenerator(this);
    }

    @Override
    public void bindParameter(PreparedStatement statement, ParameterBinding binding)
            throws SQLException, Exception {

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
}
