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

package org.apache.cayenne.conf;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.cayenne.access.DataNode;

/**
 * A DataSource wrapper around Cayenne DataNode. Helpful for lazy initialization, i.e.
 * when node's DataSource is needed to initialize some other object BEFORE it is
 * initialized itself.
 * 
 * @since 1.2
 */
class NodeDataSource implements DataSource {

    DataNode node;

    NodeDataSource(DataNode node) {
        this.node = node;
    }

    public Connection getConnection() throws SQLException {
        return node.getDataSource().getConnection();
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return node.getDataSource().getConnection(username, password);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return node.getDataSource().getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        node.getDataSource().setLogWriter(out);
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        node.getDataSource().setLoginTimeout(seconds);
    }

    public int getLoginTimeout() throws SQLException {
        return node.getDataSource().getLoginTimeout();
    }
    
    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }
}
