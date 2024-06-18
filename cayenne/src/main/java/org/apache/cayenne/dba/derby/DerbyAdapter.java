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

package org.apache.cayenne.dba.derby;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslatorFactory;
import org.apache.cayenne.access.translator.ejbql.JdbcEJBQLTranslatorFactory;
import org.apache.cayenne.access.types.ByteType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.JsonType;
import org.apache.cayenne.access.types.ShortType;
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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * DbAdapter implementation for the <a href="http://db.apache.org/derby/"> Derby RDBMS
 * </a>. Sample connection settings to use with Derby are shown below. <h3>Embedded</h3>
 * <p>
 * <pre>
 *  test-derby.jdbc.url = jdbc:derby:testdb;create=true
 *  test-derby.jdbc.driver = org.apache.derby.jdbc.EmbeddedDriver
 * </pre>
 * <p>
 * <h3>Network Server</h3>
 * <p>
 * <pre>
 *  derbynet.jdbc.url = jdbc:derby://localhost/cayenne
 *  derbynet.jdbc.driver = org.apache.derby.jdbc.ClientDriver
 *  derbynet.jdbc.username = someuser
 *  derbynet.jdbc.password = secret;
 * </pre>
 */
public class DerbyAdapter extends JdbcAdapter {

    static final String FOR_BIT_DATA_SUFFIX = " FOR BIT DATA";

    public DerbyAdapter(
            @Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject(Constants.RESOURCE_LOCATOR) ResourceLocator resourceLocator,
            @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(
                runtimeProperties,
                defaultExtendedTypes,
                userExtendedTypes,
                extendedTypeFactories,
                resourceLocator,
                valueObjectTypeRegistry);
        setSupportsGeneratedKeys(true);
        setSupportsBatchUpdates(true);
    }

    /**
     * Not supported, see: <a href="https://issues.apache.org/jira/browse/DERBY-3609">DERBY-3609</a>
     */
	@Override
	public boolean supportsGeneratedKeysForBatchInserts() {
		return false;
	}

    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new DerbyActionBuilder(node));
    }

    /**
     * Installs appropriate ExtendedTypes as converters for passing values between JDBC
     * and Java layers.
     */
    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        CharType charType = new CharType(true, true);
        map.registerType(charType);

        // address Derby driver inability to handle java.lang.Short and java.lang.Byte
        map.registerType(new ShortType(true));
        map.registerType(new ByteType(true));
        map.registerType(new JsonType(charType, true));
    }

    /**
     * Appends SQL for column creation to CREATE TABLE buffer. Only change for Derby is
     * that " NULL" is not supported.
     *
     * @since 1.2
     */
    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        String type = getType(this, column);
        String length = sizeAndPrecision(this, column);

        sqlBuffer.append(quotingStrategy.quotedName(column));
        sqlBuffer.append(' ');

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

    @Override
    public boolean typeSupportsLength(int type) {
        // "BLOB" and "CLOB" type support length. default length is 1M.
        switch (type) {
            case Types.BLOB:
            case Types.CLOB:
            case Types.NCLOB:
                return true;
            default:
                return super.typeSupportsLength(type);
        }
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
        return new DerbySQLTreeProcessor();
    }

    /**
     * @since 3.1
     */
    @Override
    protected EJBQLTranslatorFactory createEJBQLTranslatorFactory() {
        JdbcEJBQLTranslatorFactory translatorFactory = new DerbyEJBQLTranslatorFactory();
        translatorFactory.setCaseInsensitive(caseInsensitiveCollations);
        return translatorFactory;
    }

    @Override
    public void bindParameter(
            PreparedStatement statement,
            ParameterBinding binding) throws SQLException, Exception {

        if (binding.getValue() == null && binding.getJdbcType() == 0) {
            statement.setNull(binding.getStatementPosition(), Types.VARCHAR);
        } else {
            binding.setJdbcType(convertNTypes(binding.getJdbcType()));
            super.bindParameter(statement, binding);
        }
    }

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

}
