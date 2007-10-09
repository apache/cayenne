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

import java.util.Collection;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

/**
 * A SQLite database adapter that works with Zentus JDBC driver. See
 * http://www.zentus.com/sqlitejdbc/ for the driver information.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
// check http://cwiki.apache.org/CAY/sqliteadapter.html for current limitations.
public class SQLiteAdapter extends JdbcAdapter {

    public SQLiteAdapter() {
        setSupportsFkConstraints(false);
        this.setSupportsUniqueConstraints(false);
        this.setSupportsGeneratedKeys(true);
    }

    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);
        map.registerType(new SQLiteDateType());
        map.registerType(new SQLiteBigDecimalType());
        map.registerType(new SQLiteFloatType());
        map.registerType(new SQLiteByteArrayType());
    }

    public String createFkConstraint(DbRelationship rel) {
        return null;
    }

    public String createUniqueConstraint(DbEntity source, Collection columns) {
        // TODO: andrus 10/9/2007 - only ALTER TABLE ADD CONSTRAINT is not supported,
        // presumably there's some other syntax (a part of CREATE TABLE?) that would
        // create a unique constraint.
        return null;
    }

    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new SQLiteActionBuilder(this, node
                .getEntityResolver()));
    }
}
