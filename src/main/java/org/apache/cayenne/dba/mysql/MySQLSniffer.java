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

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.DbAdapterFactory;

/**
 * Detects MySQL database from JDBC metadata.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class MySQLSniffer implements DbAdapterFactory {

    public DbAdapter createAdapter(DatabaseMetaData md) throws SQLException {
        String dbName = md.getDatabaseProductName();
        if (dbName == null || !dbName.toUpperCase().contains("MYSQL")) {
            return null;
        }

        // if InnoDB is used as a default engine, allow PK
        Statement statement = md.getConnection().createStatement();
        boolean supportFK = false;

        try {
            ResultSet rs = statement.executeQuery("SHOW VARIABLES LIKE 'table_type'");
            try {
                if (rs.next()) {
                    String tableType = rs.getString(2);
                    supportFK = tableType != null
                            && tableType.toUpperCase().equals("INNODB");
                }
            }
            finally {
                rs.close();
            }
        }
        finally {
            statement.close();
        }

        MySQLAdapter adapter = new MySQLAdapter();
        adapter.setSupportsFkConstraints(supportFK);
        return adapter;
    }
}
