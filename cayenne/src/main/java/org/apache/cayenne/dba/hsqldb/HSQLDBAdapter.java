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

package org.apache.cayenne.dba.hsqldb;

import org.apache.cayenne.CoreModule;
import org.apache.cayenne.dba.NativeColumnType;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.ProcedureTranslator;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.JsonType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.RuntimeProperties;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.compiled.CompiledColumnPair;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.query.resolved.ResolvedProcedure;

import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * DbAdapter implementation for the HSQLDB RDBMS.
 */
public class HSQLDBAdapter extends JdbcAdapter {

    public static final String TRIM_FUNCTION = "RTRIM";

    public HSQLDBAdapter(
            @Inject RuntimeProperties runtimeProperties,
            @Inject(CoreModule.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(CoreModule.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(CoreModule.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, valueObjectTypeRegistry);
        setSupportsGeneratedKeys(true);
    }

    @Override
    protected NativeColumnType[] createNativeTypes() {
        return new NativeColumnType[]{
            NativeColumnType.of(Types.ARRAY, "ARRAY"),
            NativeColumnType.of(Types.BIGINT, "BIGINT"),
            NativeColumnType.of(Types.BINARY, "BINARY"),
            NativeColumnType.of(Types.BIT, "BIT"),
            NativeColumnType.of(Types.BLOB, "LONGVARBINARY"),
            NativeColumnType.of(Types.BOOLEAN, "BOOLEAN"),
            NativeColumnType.of(Types.CHAR, "CHAR").asUnconstrained(),
            NativeColumnType.of(Types.CLOB, "LONGVARCHAR"),
            NativeColumnType.of(Types.DATALINK, "DATALINK"),
            NativeColumnType.of(Types.DATE, "DATE"),
            NativeColumnType.of(Types.DECIMAL, "DECIMAL"),
            NativeColumnType.of(Types.DOUBLE, "DOUBLE"),
            NativeColumnType.of(Types.FLOAT, "FLOAT"),
            NativeColumnType.of(Types.INTEGER, "INTEGER"),
            NativeColumnType.of(Types.JAVA_OBJECT, "JAVA_OBJECT"),
            NativeColumnType.of(Types.LONGNVARCHAR, "LONGVARCHAR"),
            NativeColumnType.of(Types.LONGVARBINARY, "LONGVARBINARY"),
            NativeColumnType.of(Types.LONGVARCHAR, "LONGVARCHAR"),
            NativeColumnType.of(Types.NCHAR, "CHAR").asUnconstrained(),
            NativeColumnType.of(Types.NCLOB, "LONGVARCHAR"),
            NativeColumnType.of(Types.NUMERIC, "NUMERIC"),
            NativeColumnType.of(Types.NVARCHAR, "VARCHAR").asUnconstrained(),
            NativeColumnType.of(Types.OTHER, "OTHER"),
            NativeColumnType.of(Types.REAL, "REAL"),
            NativeColumnType.of(Types.REF, "REF"),
            NativeColumnType.of(Types.SMALLINT, "SMALLINT"),
            NativeColumnType.of(Types.STRUCT, "STRUCT"),
            NativeColumnType.of(Types.TIME, "TIME"),
            NativeColumnType.of(Types.TIMESTAMP, "TIMESTAMP"),
            NativeColumnType.of(Types.TINYINT, "TINYINT"),
            NativeColumnType.of(Types.VARBINARY, "VARBINARY"),
            NativeColumnType.of(Types.VARCHAR, "VARCHAR").asUnconstrained(),
        };
    }

    /**
     * @since 4.0
     */
    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        CharType charType = new CharType(true, true);
        map.registerType(charType);

        map.registerType(new JsonType(charType, true));
    }

    /**
     * @since 4.2
     */
    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        return new HSQLTreeProcessor();
    }

    /**
     * @since 5.0
     */
    @Override
    public ProcedureTranslator getProcedureTranslator(ResolvedProcedure procedure) {
        return new HSQLDBProcedureTranslator();
    }

    /**
     * Generate fully-qualified name for 1.8 and on. Subclass generates
     * unqualified name.
     *
     * @since 1.2
     */
    protected String getTableName(CompiledTable table) {
        QuotingStrategy quotes = getQuotingStrategy(table);
        StringBuilder buf = new StringBuilder();
        quotes.appendFQN(buf, table.catalog(), table.schema(), table.name());
        return buf.toString();
    }

    /**
     * Returns DbEntity schema name for 1.8 and on. Subclass generates
     * unqualified name.
     *
     * @since 1.2
     */
    protected String getSchemaName(CompiledTable table) {
        return table.schema();
    }

    /**
     * Uses special action builder to create the right action.
     *
     * @since 1.2
     */

    /**
     * Returns a DDL string to create a unique constraint over a set of columns.
     *
     * @since 1.1
     */
    @Override
    public String createUniqueConstraint(CompiledTable source, Collection<CompiledColumn> columns) {

        if (columns == null || columns.isEmpty()) {
            throw new CayenneRuntimeException("Can't create UNIQUE constraint - no columns specified.");
        }

        QuotingStrategy quotes = getQuotingStrategy(source);

        String srcName = getTableName(source);

        StringBuilder buf = new StringBuilder();

        buf.append("ALTER TABLE ").append(srcName);
        buf.append(" ADD CONSTRAINT ");

        String name = "U_" + source.name() + "_" + (long) (System.currentTimeMillis() / (Math.random() * 100000));
        quotes.appendFQN(buf, source.schema(), name);
        buf.append(" UNIQUE (");

        Iterator<CompiledColumn> it = columns.iterator();
        CompiledColumn first = it.next();
        quotes.appendStart(buf);
        buf.append(first.name());
        quotes.appendEnd(buf);

        while (it.hasNext()) {
            CompiledColumn next = it.next();
            buf.append(", ");
            quotes.appendStart(buf);
            buf.append(next.name());
            quotes.appendEnd(buf);
        }

        buf.append(")");

        return buf.toString();
    }

    /**
     * Adds an ADD CONSTRAINT clause to a relationship constraint.
     *
     * @see JdbcAdapter#createFkConstraint(CompiledJoin)
     */
    @Override
    public String createFkConstraint(CompiledJoin rel) {

        StringBuilder buf = new StringBuilder();
        StringBuilder refBuf = new StringBuilder();

        String srcName = getTableName(rel.source());
        String dstName = getTableName(rel.target());

        buf.append("ALTER TABLE ");
        buf.append(srcName);

        // hsqldb requires the ADD CONSTRAINT statement
        buf.append(" ADD CONSTRAINT ");

        String name = "U_" + rel.source().name() + "_"
                + (long) (System.currentTimeMillis() / (Math.random() * 100000));

        CompiledTable sourceTable = rel.source();
        QuotingStrategy quotes = getQuotingStrategy(sourceTable);

        quotes.appendFQN(buf, sourceTable.schema(), name);
        buf.append(" FOREIGN KEY (");

        boolean first = true;
        for (CompiledColumnPair join : rel.columnPairs()) {
            if (!first) {
                buf.append(", ");
                refBuf.append(", ");
            } else {
                first = false;
            }

            quotes.appendStart(buf);
            buf.append(join.source().name());
            quotes.appendEnd(buf);
            quotes.appendStart(refBuf);
            refBuf.append(join.target().name());
            quotes.appendEnd(refBuf);
        }

        buf.append(") REFERENCES ");
        buf.append(dstName);
        buf.append(" (");
        buf.append(refBuf.toString());
        buf.append(')');

        // also make sure we delete dependent FKs
        buf.append(" ON DELETE CASCADE");

        return buf.toString();
    }

    /**
     * Uses "CREATE CACHED TABLE" instead of "CREATE TABLE".
     *
     * @since 1.2
     */
    @Override
    public String createTable(CompiledTable table) {
        // SET SCHEMA <schemaname>
        String sql = super.createTable(table);
        if (sql != null && sql.toUpperCase().startsWith("CREATE TABLE ")) {
            sql = "CREATE CACHED TABLE " + sql.substring("CREATE TABLE ".length());
        }

        return sql;
    }

    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, CompiledColumn column) {
        // CAY-1095: if the column is type double, treat the max length as 0 to avoid adding precision information.
        if (column.jdbcType() == Types.DOUBLE && column.maxLength() > 0) {
            QuotingStrategy quotes = getQuotingStrategy(column.table());
            quotes.appendStart(sqlBuffer);
            sqlBuffer.append(column.name());
            quotes.appendEnd(sqlBuffer);
            sqlBuffer.append(' ').append(preferredNativeColumnType(column).nativeType());
            sqlBuffer.append(sizeAndScale(this, column, 0));
            sqlBuffer.append(column.isMandatory() ? " NOT NULL" : " NULL");
        } else {
            super.createTableAppendColumn(sqlBuffer, column);
        }

        if (column.isGenerated()) {
            sqlBuffer.append(" GENERATED BY DEFAULT AS IDENTITY (START WITH 1)");
        }
    }
}
