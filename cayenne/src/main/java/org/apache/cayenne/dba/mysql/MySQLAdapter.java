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

package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.dba.NativeColumnType;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.EJBQLTranslator;
import org.apache.cayenne.access.translator.ejbql.JdbcEJBQLTranslator;
import org.apache.cayenne.access.translator.ProcedureTranslator;
import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.DateType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.JsonType;
import org.apache.cayenne.access.types.TimeType;
import org.apache.cayenne.access.types.TimestampType;
import org.apache.cayenne.access.types.UtilDateType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.DefaultQuotingStrategy;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * DbAdapter implementation for MySQL RDBMS.
 */
public class MySQLAdapter extends JdbcAdapter {

    static final String DEFAULT_STORAGE_ENGINE = "InnoDB";
    static final List<String> SYSTEM_CATALOGS = List.of("sys", "information_schema", "mysql", "performance_schema");

    protected String storageEngine;

    public MySQLAdapter(@Inject RuntimeProperties runtimeProperties,
                        @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
                        @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
                        @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
                        @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, valueObjectTypeRegistry);

        // init defaults
        this.storageEngine = DEFAULT_STORAGE_ENGINE;

        setSupportsBatchUpdates(true);
        setSupportsUniqueConstraints(true);
        setSupportsGeneratedKeys(true);
    }

    @Override
    protected NativeColumnType[] createNativeTypes() {
        return new NativeColumnType[]{
            NativeColumnType.of(Types.BIGINT, "BIGINT"),
            NativeColumnType.of(Types.BINARY, "BINARY"),
            NativeColumnType.of(Types.BIT, "BIT"),
            NativeColumnType.of(Types.BLOB, "LONGBLOB"),
            NativeColumnType.of(Types.BOOLEAN, "BOOL"),
            NativeColumnType.of(Types.CHAR, "CHAR"),
            NativeColumnType.of(Types.CHAR, "longtext").asUnconstrained(),
            NativeColumnType.of(Types.CLOB, "LONGTEXT"),
            NativeColumnType.of(Types.DATE, "DATE"),
            NativeColumnType.of(Types.DECIMAL, "DECIMAL"),
            NativeColumnType.of(Types.DOUBLE, "DOUBLE"),
            NativeColumnType.of(Types.FLOAT, "FLOAT"),
            NativeColumnType.of(Types.INTEGER, "INT"),
            NativeColumnType.of(Types.LONGNVARCHAR, "LONGTEXT"),
            NativeColumnType.of(Types.LONGVARBINARY, "LONGBLOB"),
            NativeColumnType.of(Types.LONGVARCHAR, "LONGTEXT"),
            NativeColumnType.of(Types.NCHAR, "CHAR"),
            NativeColumnType.of(Types.NCHAR, "longtext").asUnconstrained(),
            NativeColumnType.of(Types.NCLOB, "LONGTEXT"),
            NativeColumnType.of(Types.NUMERIC, "DECIMAL"),
            NativeColumnType.of(Types.NVARCHAR, "VARCHAR"),
            NativeColumnType.of(Types.NVARCHAR, "longtext").asUnconstrained(),
            NativeColumnType.of(Types.REAL, "DOUBLE"),
            NativeColumnType.of(Types.SMALLINT, "SMALLINT"),
            NativeColumnType.of(Types.SQLXML, "LONGTEXT"),
            NativeColumnType.of(Types.TIME, "TIME"),
            NativeColumnType.of(Types.TIMESTAMP, "DATETIME"),
            NativeColumnType.of(Types.TINYINT, "TINYINT"),
            NativeColumnType.of(Types.VARBINARY, "VARBINARY"),
            NativeColumnType.of(Types.VARCHAR, "VARCHAR"),
            NativeColumnType.of(Types.VARCHAR, "longtext").asUnconstrained(),
        };
    }

    @Override
    protected QuotingStrategy createQuotingStrategy() {
        return new DefaultQuotingStrategy('`', '`');
    }

    /**
     * @since 4.2
     */
    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        return MySQLTreeProcessor.getInstance(caseInsensitiveCollations);
    }

    /**
     * Uses special action builder to create the right action.
     *
     * @since 1.2
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new MySQLActionBuilder(node));
    }

    /**
     * @since 5.0
     */
    @Override
    public ProcedureTranslator getProcedureTranslator(ProcedureQuery query, EntityResolver entityResolver) {
        return new MySQLProcedureTranslator();
    }

    /**
     * @since 3.0
     */
    @Override
    public Collection<String> dropTableStatements(DbEntity table) {
        // note that CASCADE is a noop as of MySQL 5.0, so we have to use FK
        // checks
        // statement
        StringBuilder buf = new StringBuilder();
        QuotingStrategy quotes = getQuotingStrategy(table);
        quotes.appendFQN(buf, table.getCatalog(), table.getSchema(), table.getName());

        return List.of("SET FOREIGN_KEY_CHECKS=0", "DROP TABLE IF EXISTS " + buf + " CASCADE",
                "SET FOREIGN_KEY_CHECKS=1");
    }

    /**
     * Installs appropriate ExtendedTypes used as converters for passing values
     * between JDBC and Java layers.
     */
    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // TODO: this may need to be made universal across adapters
        map.registerType(new MySQLLocalDateTimeType());

        // must handle CLOBs as strings, otherwise there
        // are problems with NULL clobs that are treated
        // as empty strings... somehow this doesn't happen
        // for BLOBs (ConnectorJ v. 3.0.9)
        CharType charType = new CharType(false, false);
        map.registerType(charType);
        map.registerType(new ByteArrayType(false, false));
        map.registerType(new JsonType(charType, true));

        // register non-default types for the dates, see CAY-2691
        map.registerType(new DateType(true));
        map.registerType(new TimeType(true));
        map.registerType(new TimestampType(true));
        map.registerType(new UtilDateType(true));
    }

    @Override
    public DbAttribute buildAttribute(
            String name,
            String typeName,
            int type,
            int maxLength,
            int scale,
            boolean allowNulls) {

        String normalTypeName = typeName != null ? typeName.toLowerCase() : null;
        switch (type) {
            // all LOB types are returned by the driver as OTHER... must remap them manually (at least on MySQL 3.23)
            case Types.OTHER -> {
                if (normalTypeName != null) {
                    type = switch (normalTypeName) {
                        case "longblob", "mediumblob", "blob" -> Types.BLOB;
                        case "tinyblob" -> Types.VARBINARY;
                        case "longtext", "mediumtext", "text" -> Types.CLOB;
                        case "tinytext" -> Types.VARCHAR;
                        default -> type;
                    };
                }
            }
            // A special case for the JSON type and older MySQL drivers (5.x)
            case Types.CHAR -> {
                if ("json".equals(normalTypeName)) {
                    type = Types.LONGVARCHAR;
                }
            }
            // driver reports column size that we should "translate" to the column precision see CAY-2694 for details
            case Types.TIME -> {
                scale = Math.max(0, maxLength - 9);
                maxLength = -1;
            }
            case Types.TIMESTAMP -> {
                scale = Math.max(0, maxLength - 20);
                maxLength = -1;
            }
            // types like "int unsigned" map to Long
            default -> {
                if (normalTypeName != null && normalTypeName.endsWith(" unsigned")) {
                    // per http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-type-conversions.html
                    if ("int unsigned".equals(normalTypeName)
                            || "integer unsigned".equals(normalTypeName)
                            || "mediumint unsigned".equals(normalTypeName)) {
                        type = Types.BIGINT;
                    }
                    // BIGINT UNSIGNED maps to BigInteger according to MySQL docs, but there is no JDBC mapping for BigInteger
                }
            }
        }

        DbAttribute a = new DbAttribute(name);
        a.setMandatory(!allowNulls);
        a.setType(type);

        if (maxLength >= 0) {
            a.setMaxLength(maxLength);
        }

        if (scale >= 0) {
            a.setScale(scale);
        }

        return a;
    }

    @Override
    public int preferredBindingType(int jdbcType) {
        return switch (jdbcType) {
            case Types.NCHAR -> Types.CHAR;
            case Types.NCLOB -> Types.CLOB;
            case Types.NVARCHAR -> Types.VARCHAR;
            case Types.LONGNVARCHAR -> Types.LONGVARCHAR;
            default -> jdbcType;
        };
    }

    /**
     * @since 3.0
     */
    @Override
    protected EJBQLTranslator createEJBQLTranslator() {
        JdbcEJBQLTranslator translatorFactory = new MySQLEJBQLTranslator();
        translatorFactory.setCaseInsensitive(caseInsensitiveCollations);
        return translatorFactory;
    }

    /**
     * Overrides super implementation to explicitly set table engine to InnoDB
     * if FK constraints are supported by this adapter.
     */
    @Override
    public String createTable(DbEntity entity) {
        String ddlSQL = super.createTable(entity);

        if (storageEngine != null) {
            ddlSQL += " ENGINE=" + storageEngine;
        }

        return ddlSQL;
    }

    /**
     * Customizes PK clause semantics to ensure that generated columns are in
     * the beginning of the PK definition, as this seems to be a requirement for
     * InnoDB tables.
     *
     * @since 1.2
     */
    // See CAY-358 for details of the InnoDB problem
    @Override
    protected void createTableAppendPKClause(StringBuffer sqlBuffer, DbEntity entity) {

        // must move generated to the front...
        List<DbAttribute> pkList = new ArrayList<>(entity.getPrimaryKeys());
        pkList.sort(PKComparator.INSTANCE);

        QuotingStrategy quotes = getQuotingStrategy(entity);

        Iterator<DbAttribute> pkit = pkList.iterator();
        if (pkit.hasNext()) {

            sqlBuffer.append(", PRIMARY KEY (");
            boolean firstPk = true;
            while (pkit.hasNext()) {
                if (firstPk) {
                    firstPk = false;
                } else {
                    sqlBuffer.append(", ");
                }

                DbAttribute at = pkit.next();
                quotes.appendStart(sqlBuffer);
                sqlBuffer.append(at.getName());
                quotes.appendEnd(sqlBuffer);
            }
            sqlBuffer.append(')');
        }
    }

    /**
     * Appends AUTO_INCREMENT clause to the column definition for generated
     * columns.
     */
    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {

        String type = preferredNativeColumnType(column).nativeType();

        QuotingStrategy quotes = getQuotingStrategy(column.getEntity());
        quotes.appendStart(sqlBuffer);
        sqlBuffer.append(column.getName());
        quotes.appendEnd(sqlBuffer);
        sqlBuffer.append(' ').append(type);

        // append size and precision (if applicable)s
        appendLengthAndScale(sqlBuffer, column);

        sqlBuffer.append(column.isMandatory() ? " NOT NULL" : " NULL");

        if (column.isGenerated()) {
            sqlBuffer.append(" AUTO_INCREMENT");
        }
    }

    private void appendLengthAndScale(StringBuffer sqlBuffer, DbAttribute column) {
        if (column.getType() == Types.TIME || column.getType() == Types.TIMESTAMP) {
            int scale = column.getScale();
            if (scale >= 0) {
                sqlBuffer.append('(').append(scale).append(')');
            }
        } else if (typeSupportsLength(column.getType())) {
            int len = column.getMaxLength();

            int scale = TypesMapping.isDecimal(column.getType()) ? column.getScale() : -1;

            // sanity check
            if (scale > len) {
                scale = -1;
            }

            if (len > 0) {
                sqlBuffer.append('(').append(len);

                if (scale >= 0) {
                    sqlBuffer.append(", ").append(scale);
                }

                sqlBuffer.append(')');
            }
        }
    }


    @Override
    public boolean typeSupportsLength(int type) {
        // As of MySQL 5.6.4 the "TIMESTAMP" and "TIME" types support length,
        // which is the number of decimal places for fractional seconds
        // http://dev.mysql.com/doc/refman/5.6/en/fractional-seconds.html
        return switch (type) {
            case Types.TIMESTAMP, Types.TIME -> true;
            default -> super.typeSupportsLength(type);
        };
    }

    @Override
    public List<String> getSystemCatalogs() {
        return SYSTEM_CATALOGS;
    }

    /**
     * @since 3.0
     */
    public String getStorageEngine() {
        return storageEngine;
    }

    /**
     * @since 3.0
     */
    public void setStorageEngine(String engine) {
        this.storageEngine = engine;
    }

    static final class PKComparator implements Comparator<DbAttribute> {

        static final PKComparator INSTANCE = new PKComparator();

        public int compare(DbAttribute a1, DbAttribute a2) {
            if (a1.isGenerated() != a2.isGenerated()) {
                return a1.isGenerated() ? -1 : 1;
            } else {
                return a1.getName().compareTo(a2.getName());
            }
        }
    }
}
