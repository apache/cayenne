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
package org.apache.cayenne.itest.jpa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.cayenne.conn.PoolManager;

/**
 * A helper class that manages test DataSource.
 * 
 */
class ItestDataSourceManager {

    private DataSource dataSource;
    private String schemaScriptUrl;
    private String dbName;

    ItestDataSourceManager(String schemaScriptUrl) {
        this.schemaScriptUrl = schemaScriptUrl;

        // create pseudo random DB name
        this.dbName = "d" + System.currentTimeMillis();
    }

    public DataSource getDataSource() {
        if (this.dataSource == null) {
            this.dataSource = createDataSource();
        }

        return dataSource;
    }

    String getSchemaScriptUrl() {
        return schemaScriptUrl;
    }

    /**
     * Creates DataSource and loads local schema.
     */
    private DataSource createDataSource() {
        DataSource dataSource;
        try {
            dataSource = new PoolManager("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:"
                    + dbName, 1, 2, "sa", null);
        }
        catch (SQLException e) {
            throw new RuntimeException("Error creating DataSource", e);
        }

        Connection c = null;

        try {
            c = dataSource.getConnection();
            loadSchema(c, schemaScriptUrl);
        }
        catch (SQLException e) {
            throw new RuntimeException("Error loading schema", e);
        }
        finally {
            if (c != null) {
                try {
                    c.close();
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return dataSource;
    }

    private void loadSchema(Connection c, String schemaFile) throws SQLException {
        InputStream in = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream(schemaFile);

        if (in == null) {
            throw new SQLException("No SQL script found in classpath: " + schemaFile);
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {

            String line;
            while ((line = reader.readLine()) != null) {

                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }

                Statement st = c.createStatement();

                try {
                    st.executeUpdate(line.trim());
                }
                finally {
                    st.close();
                }
            }
        }
        catch (IOException ex) {
            throw new SQLException("Error reading SQL input: " + ex);
        }
        finally {
            try {
                reader.close();
            }
            catch (IOException e) {
                // ignore
            }
        }
    }

}
