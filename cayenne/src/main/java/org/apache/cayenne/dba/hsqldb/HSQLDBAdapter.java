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

import org.apache.cayenne.dba.NativeColumnType;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslator;
import org.apache.cayenne.access.translator.ejbql.JdbcEJBQLTranslator;
import org.apache.cayenne.access.translator.procedure.ProcedureTranslator;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.JsonType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

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
            @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
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
    public ProcedureTranslator getProcedureTranslator(ProcedureQuery query, EntityResolver entityResolver) {
        return new HSQLDBProcedureTranslator();
    }

    /**
     * @since 4.0
     */
    @Override
    protected EJBQLTranslator createEJBQLTranslator() {
        JdbcEJBQLTranslator translatorFactory = new HSQLEJBQLTranslator();
        translatorFactory.setCaseInsensitive(caseInsensitiveCollations);
        return translatorFactory;
    }

    /**
     * Generate fully-qualified name for 1.8 and on. Subclass generates
     * unqualified name.
     *
     * @since 1.2
     */
    protected String getTableName(DbEntity entity) {
        return quotingStrategy.quotedFullyQualifiedName(entity);
    }

    /**
     * Returns DbEntity schema name for 1.8 and on. Subclass generates
     * unqualified name.
     *
     * @since 1.2
     */
    protected String getSchemaName(DbEntity entity) {
        return entity.getSchema();
    }

    /**
     * Uses special action builder to create the right action.
     *
     * @since 1.2
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new HSQLActionBuilder(node));
    }

    /**
     * Returns a DDL string to create a unique constraint over a set of columns.
     *
     * @since 1.1
     */
    @Override
    public String createUniqueConstraint(DbEntity source, Collection<DbAttribute> columns) {

        if (columns == null || columns.isEmpty()) {
            throw new CayenneRuntimeException("Can't create UNIQUE constraint - no columns specified.");
        }

        String srcName = getTableName(source);

        StringBuilder buf = new StringBuilder();

        buf.append("ALTER TABLE ").append(srcName);
        buf.append(" ADD CONSTRAINT ");

        String name = "U_" + source.getName() + "_" + (long) (System.currentTimeMillis() / (Math.random() * 100000));
        buf.append(quotingStrategy.quotedIdentifier(source, source.getSchema(), name));
        buf.append(" UNIQUE (");

        Iterator<DbAttribute> it = columns.iterator();
        DbAttribute first = it.next();
        buf.append(quotingStrategy.quotedName(first));

        while (it.hasNext()) {
            DbAttribute next = it.next();
            buf.append(", ");
            buf.append(quotingStrategy.quotedName(next));
        }

        buf.append(")");

        return buf.toString();
    }

    /**
     * Adds an ADD CONSTRAINT clause to a relationship constraint.
     *
     * @see JdbcAdapter#createFkConstraint(DbRelationship)
     */
    @Override
    public String createFkConstraint(DbRelationship rel) {

        StringBuilder buf = new StringBuilder();
        StringBuilder refBuf = new StringBuilder();

        String srcName = getTableName(rel.getSourceEntity());
        String dstName = getTableName(rel.getTargetEntity());

        buf.append("ALTER TABLE ");
        buf.append(srcName);

        // hsqldb requires the ADD CONSTRAINT statement
        buf.append(" ADD CONSTRAINT ");

        String name = "U_" + rel.getSourceEntity().getName() + "_"
                + (long) (System.currentTimeMillis() / (Math.random() * 100000));

        DbEntity sourceEntity = rel.getSourceEntity();

        buf.append(quotingStrategy.quotedIdentifier(sourceEntity, sourceEntity.getSchema(), name));
        buf.append(" FOREIGN KEY (");

        boolean first = true;
        for (DbJoin join : rel.getJoins()) {
            if (!first) {
                buf.append(", ");
                refBuf.append(", ");
            } else {
                first = false;
            }

            buf.append(quotingStrategy.quotedSourceName(join));
            refBuf.append(quotingStrategy.quotedTargetName(join));
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
    public String createTable(DbEntity ent) {
        // SET SCHEMA <schemaname>
        String sql = super.createTable(ent);
        if (sql != null && sql.toUpperCase().startsWith("CREATE TABLE ")) {
            sql = "CREATE CACHED TABLE " + sql.substring("CREATE TABLE ".length());
        }

        return sql;
    }

    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        // CAY-1095: if the column is type double, temporarily set the max
        // length to 0 to
        // avoid adding precision information.
        if (column.getType() == Types.DOUBLE && column.getMaxLength() > 0) {
            int len = column.getMaxLength();
            column.setMaxLength(0);
            super.createTableAppendColumn(sqlBuffer, column);
            column.setMaxLength(len);
        } else {
            super.createTableAppendColumn(sqlBuffer, column);
        }

        if (column.isGenerated()) {
            sqlBuffer.append(" GENERATED BY DEFAULT AS IDENTITY (START WITH 1)");
        }
    }
}
