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
package org.apache.cayenne.dba.sqlite;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

import java.sql.Types;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A SQLite database adapter.
 *
 * @since 3.0
 */
public class SQLiteAdapter extends JdbcAdapter {

    public SQLiteAdapter(
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
        this.setSupportsUniqueConstraints(false);
        this.setSupportsGeneratedKeys(true);
    }

    @Override
    protected Map<Integer, String[]> createExternalTypes() {
        Map<Integer, String[]> types = new HashMap<>();
        types.put(Types.ARRAY, new String[]{"ARRAY"});
        types.put(Types.BIGINT, new String[]{"BIGINT"});
        types.put(Types.BINARY, new String[]{"BINARY"});
        types.put(Types.BIT, new String[]{"INTEGER"});
        types.put(Types.BLOB, new String[]{"BLOB"});
        types.put(Types.BOOLEAN, new String[]{"INTEGER"});
        types.put(Types.CHAR, new String[]{"CHAR"});
        types.put(Types.CLOB, new String[]{"CLOB"});
        types.put(Types.DATALINK, new String[]{"DATALINK"});
        types.put(Types.DATE, new String[]{"DATE"});
        types.put(Types.DECIMAL, new String[]{"DECIMAL"});
        types.put(Types.DOUBLE, new String[]{"DOUBLE"});
        types.put(Types.FLOAT, new String[]{"REAL"});
        types.put(Types.INTEGER, new String[]{"INTEGER"});
        types.put(Types.JAVA_OBJECT, new String[]{"JAVA_OBJECT"});
        types.put(Types.LONGNVARCHAR, new String[]{"LONGNVARCHAR"});
        types.put(Types.LONGVARBINARY, new String[]{"LONGVARBINARY"});
        types.put(Types.LONGVARCHAR, new String[]{"LONGVARCHAR"});
        types.put(Types.NCHAR, new String[]{"NCHAR"});
        types.put(Types.NCLOB, new String[]{"TEXT"});
        types.put(Types.NUMERIC, new String[]{"NUMERIC"});
        types.put(Types.NVARCHAR, new String[]{"NVARCHAR"});
        types.put(Types.OTHER, new String[]{"OTHER"});
        types.put(Types.REAL, new String[]{"REAL"});
        types.put(Types.REF, new String[]{"REF"});
        types.put(Types.SMALLINT, new String[]{"SMALLINT"});
        types.put(Types.SQLXML, new String[]{"TEXT"});
        types.put(Types.STRUCT, new String[]{"STRUCT"});
        types.put(Types.TIME, new String[]{"TIME"});
        types.put(Types.TIMESTAMP, new String[]{"TIMESTAMP"});
        types.put(Types.TINYINT, new String[]{"TINYINT"});
        types.put(Types.VARBINARY, new String[]{"VARBINARY"});
        types.put(Types.VARCHAR, new String[]{"VARCHAR"});
        return types;
    }

    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);
        map.registerType(new SQLiteDateType());
        map.registerType(new SQLiteBigDecimalType());
        map.registerType(new SQLiteFloatType());
        map.registerType(new SQLiteByteArrayType());
        map.registerType(new SQLiteCalendarType<>(GregorianCalendar.class));
        map.registerType(new SQLiteCalendarType<>(Calendar.class));
    }

    /**
     * @since 4.2
     */
    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        return new SQLiteTreeProcessor();
    }

    @Override
    public String createFkConstraint(DbRelationship rel) {
        return null;
    }

    @Override
    public String createUniqueConstraint(DbEntity source, Collection<DbAttribute> columns) {
        // TODO: andrus 10/9/2007 - only ALTER TABLE ADD CONSTRAINT is not supported,
        // presumably there's some other syntax (a part of CREATE TABLE?) that would
        // create a unique constraint.
        return null;
    }

    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new SQLiteActionBuilder(node));
    }

    /**
     * Appends AUTOINCREMENT clause to the column definition for generated columns.
     */
    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        super.createTableAppendColumn(sqlBuffer, column);
        DbEntity entity = column.getEntity();
        if (column.isGenerated()
                && column.isPrimaryKey()
                && entity.getPrimaryKeys().size() == 1) {
            sqlBuffer.append(" PRIMARY KEY AUTOINCREMENT");
        }
    }

    @Override
    protected void createTableAppendPKClause(StringBuffer sqlBuffer, DbEntity entity) {

        // do not append " PRIMARY KEY () " for single column generated primary key
        if (entity.getPrimaryKeys().size() == 1) {
            DbAttribute column = entity.getPrimaryKeys().getFirst();
            if (column.isGenerated()) {
                return;
            }
        }

        super.createTableAppendPKClause(sqlBuffer, entity);
    }
}
