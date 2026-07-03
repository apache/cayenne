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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslator;
import org.apache.cayenne.access.translator.ejbql.JdbcEJBQLTranslator;
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
import org.apache.cayenne.dba.NativeColumnType;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

/**
 * DbAdapter implementation for the Derby RDBMS
 */
public class DerbyAdapter extends JdbcAdapter {

    static final String FOR_BIT_DATA_SUFFIX = " FOR BIT DATA";

    public DerbyAdapter(
            @Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(
                runtimeProperties,
                defaultExtendedTypes,
                userExtendedTypes,
                extendedTypeFactories,
                valueObjectTypeRegistry);
        setSupportsGeneratedKeys(true);
        setSupportsBatchUpdates(true);
    }

    @Override
    protected NativeColumnType[] createNativeTypes() {
        return new NativeColumnType[]{
            NativeColumnType.of(Types.ARRAY, "ARRAY"),
            NativeColumnType.of(Types.BIGINT, "BIGINT"),
            NativeColumnType.of(Types.BINARY, "CHAR FOR BIT DATA"),
            NativeColumnType.of(Types.BIT, "SMALLINT"),
            NativeColumnType.of(Types.BLOB, "BLOB"),
            NativeColumnType.of(Types.BOOLEAN, "BOOLEAN"),
            NativeColumnType.of(Types.CHAR, "CHAR"),
            NativeColumnType.of(Types.CLOB, "CLOB"),
            NativeColumnType.of(Types.DATALINK, "DATALINK"),
            NativeColumnType.of(Types.DATE, "DATE"),
            NativeColumnType.of(Types.DECIMAL, "DECIMAL"),
            NativeColumnType.of(Types.DOUBLE, "DOUBLE PRECISION"),
            NativeColumnType.of(Types.FLOAT, "DOUBLE PRECISION"),
            NativeColumnType.of(Types.INTEGER, "INTEGER"),
            NativeColumnType.of(Types.JAVA_OBJECT, "JAVA_OBJECT"),
            NativeColumnType.of(Types.LONGNVARCHAR, "LONG VARCHAR"),
            NativeColumnType.of(Types.LONGVARBINARY, "LONG VARCHAR FOR BIT DATA"),
            NativeColumnType.of(Types.LONGVARCHAR, "LONG VARCHAR"),
            NativeColumnType.of(Types.NCHAR, "CHAR"),
            NativeColumnType.of(Types.NCLOB, "CLOB"),
            NativeColumnType.of(Types.NUMERIC, "DECIMAL"),
            NativeColumnType.of(Types.NVARCHAR, "VARCHAR"),
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
        String type = preferredNativeColumnType(column).nativeType();
        String length = sizeAndScale(this, column);

        QuotingStrategy quotes = getQuotingStrategy(column.getEntity());
        quotes.appendStart(sqlBuffer);
        sqlBuffer.append(column.getName());
        quotes.appendEnd(sqlBuffer);
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
    protected EJBQLTranslator createEJBQLTranslator() {
        JdbcEJBQLTranslator translatorFactory = new DerbyEJBQLTranslator();
        translatorFactory.setCaseInsensitive(caseInsensitiveCollations);
        return translatorFactory;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected void bind(PreparedStatement statement, Object value, int psPosition, int psType, int psScale,
                        ExtendedType binder) throws Exception {
        if (value == null && psType == 0) {
            statement.setNull(psPosition, Types.VARCHAR);
        } else {
            super.bind(statement, value, psPosition, psType, psScale, binder);
        }
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

}
