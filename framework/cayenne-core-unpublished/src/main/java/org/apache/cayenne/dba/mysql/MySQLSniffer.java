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

package org.apache.cayenne.dba.mysql;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.cayenne.configuration.server.DbAdapterDetector;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;

/**
 * Detects MySQL database from JDBC metadata.
 * 
 * @since 1.2
 */
public class MySQLSniffer implements DbAdapterDetector {

    protected AdhocObjectFactory objectFactory;

    public MySQLSniffer(@Inject AdhocObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    public DbAdapter createAdapter(DatabaseMetaData md) throws SQLException {
        String dbName = md.getDatabaseProductName();
        if (dbName == null || !dbName.toUpperCase().contains("MYSQL")) {
            return null;
        }

        // if InnoDB is used as a default engine, allow PK
        Statement statement = md.getConnection().createStatement();
        boolean supportFK = false;
        String adapterStorageEngine = MySQLAdapter.DEFAULT_STORAGE_ENGINE;

        try {
            // http://dev.mysql.com/doc/refman/5.0/en/storage-engines.html
            // per link above "table type" concept is deprecated in favor of "storage
            // engine". Not sure if we should check "storage_engine" variable and in what
            // version of MySQL it got introduced...
            ResultSet rs = statement.executeQuery("SHOW VARIABLES LIKE 'table_type'");
            try {
                if (rs.next()) {
                    String storageEngine = rs.getString(2);
                    if (storageEngine != null) {
                        adapterStorageEngine = storageEngine;
                        supportFK = storageEngine.toUpperCase().equals("INNODB");
                    }
                }
            }
            finally {
                rs.close();
            }
        }
        finally {
            statement.close();
        }

        MySQLAdapter adapter = objectFactory.newInstance(
                MySQLAdapter.class,
                MySQLAdapter.class.getName());
        adapter.setSupportsFkConstraints(supportFK);
        adapter.setStorageEngine(adapterStorageEngine);
        return adapter;
    }
}
