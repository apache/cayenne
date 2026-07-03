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

package org.apache.cayenne.dba.sqlserver;

import org.apache.cayenne.dba.NativeColumnType;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslator;
import org.apache.cayenne.access.types.ByteArrayType;
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
import org.apache.cayenne.dba.DefaultQuotingStrategy;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cayenne DbAdapter implementation for Microsoft SQL Server RDBMS.
 *
 * @since 1.1
 */
public class SQLServerAdapter extends JdbcAdapter {

    // Stores the major version of the database.
    // Database versions 12 and higher supports the use of LIMIT,lower versions use TOP N.
    private Integer version;

    private final List<String> SYSTEM_SCHEMAS = List.of(
            "db_accessadmin", "db_backupoperator",
            "db_datareader", "db_datawriter", "db_ddladmin", "db_denydatareader",
            "db_denydatawriter", "sys", "db_owner", "db_securityadmin", "INFORMATION_SCHEMA"
    );

    private final List<String> SYSTEM_CATALOGS = List.of("model", "msdb", "tempdb");

    public SQLServerAdapter(@Inject RuntimeProperties runtimeProperties,
                            @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
                            @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
                            @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
                            @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, valueObjectTypeRegistry);

        this.setSupportsGeneratedKeys(true);
        this.setSupportsBatchUpdates(true);
    }

    @Override
    protected NativeColumnType[] createNativeTypes() {
        return new NativeColumnType[]{
            NativeColumnType.of(Types.BIGINT, "bigint"),
            NativeColumnType.of(Types.BINARY, "binary"),
            NativeColumnType.of(Types.BIT, "bit"),
            NativeColumnType.of(Types.BLOB, "image"),
            NativeColumnType.of(Types.BOOLEAN, "bit"),
            NativeColumnType.of(Types.CHAR, "char"),
            NativeColumnType.of(Types.CHAR, "varchar(max)").asUnconstrained(),
            NativeColumnType.of(Types.CLOB, "text"),
            NativeColumnType.of(Types.DATE, "date"),
            NativeColumnType.of(Types.DECIMAL, "decimal"),
            NativeColumnType.of(Types.DOUBLE, "double precision"),
            NativeColumnType.of(Types.FLOAT, "float"),
            NativeColumnType.of(Types.INTEGER, "int"),
            NativeColumnType.of(Types.LONGNVARCHAR, "ntext"),
            NativeColumnType.of(Types.LONGVARBINARY, "image"),
            NativeColumnType.of(Types.LONGVARCHAR, "text"),
            NativeColumnType.of(Types.NCHAR, "nchar"),
            NativeColumnType.of(Types.NCHAR, "nvarchar(max)").asUnconstrained(),
            NativeColumnType.of(Types.NCLOB, "ntext"),
            NativeColumnType.of(Types.NUMERIC, "numeric"),
            NativeColumnType.of(Types.NVARCHAR, "nvarchar"),
            NativeColumnType.of(Types.NVARCHAR, "nvarchar(max)").asUnconstrained(),
            NativeColumnType.of(Types.REAL, "real"),
            NativeColumnType.of(Types.ROWID, "ROWID"),
            NativeColumnType.of(Types.SMALLINT, "smallint"),
            NativeColumnType.of(Types.SQLXML, "xml"),
            NativeColumnType.of(Types.TIME, "time"),
            NativeColumnType.of(Types.TIMESTAMP, "datetime"),
            NativeColumnType.of(Types.TINYINT, "tinyint"),
            NativeColumnType.of(Types.VARBINARY, "varbinary"),
            NativeColumnType.of(Types.VARCHAR, "varchar"),
            NativeColumnType.of(Types.VARCHAR, "varchar(max)").asUnconstrained(),
        };
    }

    @Override
    protected QuotingStrategy createQuotingStrategy() {
        return new DefaultQuotingStrategy('[', ']');
    }

    @Override
    protected EJBQLTranslator createEJBQLTranslator() {
        return new SQLServerEJBQLTranslator();
    }

    /**
     * Returns the word "go".
     */
    @Override
    public String getBatchTerminator() {
        return "go";
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected void bind(PreparedStatement statement, Object value, int psPosition, int psType, int psScale,
                        ExtendedType binder) throws Exception {

        // SQL Server driver doesn't like CLOBs and BLOBs as parameters
        if (value == null) {
            int jdbcType = switch (psType) {
                case Types.CLOB, 0 -> Types.VARCHAR;
                case Types.BLOB -> Types.VARBINARY;
                default -> psType;
            };
            statement.setNull(psPosition, jdbcType);
        } else {
            super.bind(statement, value, psPosition, psType, psScale, binder);
        }
    }

    /**
     * Overrides super implementation to correctly set up identity columns.
     */
    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {

        super.createTableAppendColumn(sqlBuffer, column);

        if (column.isGenerated()) {
            // current limitation - we don't allow to set identity parameters...
            sqlBuffer.append(" IDENTITY (1, 1)");
        }
    }

    /**
     * Not supported, see: <a href="https://github.com/microsoft/mssql-jdbc/issues/245">mssql-jdbc #245</a>
     */
    @Override
    public boolean supportsGeneratedKeysForBatchInserts() {
        return false;
    }

    /**
     * @since 4.2
     */
    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        if (getVersion() != null && getVersion() >= 12) {
            return new SQLServerTreeProcessorV12();
        }
        return new SQLServerTreeProcessor();
    }

    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        map.registerType(new CharType(true, false));

        // create specially configured ByteArrayType handler
        map.registerType(new ByteArrayType(true, false));

        // address driver inability to handle java.lang.Short and java.lang.Byte
        map.registerType(new ShortType(true));
        map.registerType(new ByteType(true));

        CharType charType = new CharType(true, false);
        map.registerType(charType);
        map.registerType(new JsonType(charType, true));
    }

    /**
     * Uses SQLServerActionBuilder to create the right action.
     *
     * @since 1.2
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new SQLServerActionBuilder(node, getVersion()));
    }

    @Override
    public List<String> getSystemSchemas() {
        return SYSTEM_SCHEMAS;
    }

    @Override
    public List<String> getSystemCatalogs() {
        return SYSTEM_CATALOGS;
    }

    public Integer getVersion() {
        return version;
    }

    /**
     * @param version of the server as provided by the JDBC driver
     * @since 4.2
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * Generates DDL to create unique index that allows multiple NULL values to comply with ANSI SQL,
     * that is default behaviour for other RDBMS.
     * <br>
     * Example:
     * <pre>
     * {@code
     * CREATE UNIQUE NONCLUSTERED INDEX _idx_entity_attribute
     * ON entity(attribute)
     * WHERE attribute IS NOT NULL
     * }
     * </pre>
     *
     * @param source  entity for the index
     * @param columns source columns for the index
     * @return DDL to create unique index
     * @since 4.2.1
     */
    @Override
    public String createUniqueConstraint(DbEntity source, Collection<DbAttribute> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new CayenneRuntimeException("Can't create UNIQUE constraint - no columns specified.");
        }

        QuotingStrategy quotes = getQuotingStrategy(source);

        return "CREATE UNIQUE NONCLUSTERED INDEX " + uniqueIndexName(source, columns) + " ON " +
                quotes.quotedFQN(source.getCatalog(), source.getSchema(), source.getName()) +
                "(" +
                columns.stream().map(c -> quotes.quoted(c.getName())).collect(Collectors.joining(", ")) +
                ") WHERE " +
                columns.stream().map(c -> quotes.quoted(c.getName()))
                        .map(n -> n + " IS NOT NULL")
                        .collect(Collectors.joining(" AND "));
    }

    private String uniqueIndexName(DbEntity source, Collection<DbAttribute> columns) {
        return "_idx_unique_"
                + source.getName().replace(' ', '_').toLowerCase()
                + "_"
                + columns.stream()
                .map(DbAttribute::getName)
                .map(String::toLowerCase)
                .map(n -> n.replace(' ', '_'))
                .collect(Collectors.joining("_"));
    }
}
