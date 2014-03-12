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
package org.apache.cayenne.dba.sqlite;

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.resource.ResourceLocator;

/**
 * A SQLite database adapter that works with Zentus JDBC driver. See
 * http://www.zentus.com/sqlitejdbc/ for the driver information. Also look at
 * http://www.xerial.org/trac/Xerial/wiki/SQLiteJDBC for another adaptor option.
 * 
 * <pre>
 *      sqlite.jdbc.url = jdbc:sqlite:sqlitetest.db
 *      sqlite.jdbc.driver = org.sqlite.JDBC
 * </pre>
 * 
 * @since 3.0
 */
// check http://cwiki.apache.org/CAY/sqliteadapter.html for current limitations.
public class SQLiteAdapter extends JdbcAdapter {

    public SQLiteAdapter(
            @Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject ResourceLocator resourceLocator) {
        super(
                runtimeProperties,
                defaultExtendedTypes,
                userExtendedTypes,
                extendedTypeFactories,
                resourceLocator);
        this.setSupportsUniqueConstraints(false);
        this.setSupportsGeneratedKeys(true);
    }

    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);
        map.registerType(new SQLiteDateType());
        map.registerType(new SQLiteBigDecimalType());
        map.registerType(new SQLiteFloatType());
        map.registerType(new SQLiteByteArrayType());
        map.registerType(new SQLiteCalendarType(GregorianCalendar.class));
        map.registerType(new SQLiteCalendarType(Calendar.class));
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
        DbEntity entity = (DbEntity) column.getEntity();
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
            DbAttribute column = entity.getPrimaryKeys().iterator().next();
            if (column.isGenerated()) {
                return;
            }
        }

        super.createTableAppendPKClause(sqlBuffer, entity);
    }
}
