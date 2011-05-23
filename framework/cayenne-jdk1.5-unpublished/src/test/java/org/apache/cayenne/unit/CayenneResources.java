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

package org.apache.cayenne.unit;

import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.BatchQueryBuilderFactory;
import org.apache.cayenne.access.jdbc.DefaultBatchQueryBuilderFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.conn.PoolDataSource;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Initializes connections for Cayenne unit tests.
 */
public class CayenneResources {

    private static Log logger = LogFactory.getLog(CayenneResources.class);

    public static final String SQL_TEMPLATE_CUSTOMIZER = "SQLTemplateCustomizer";

    protected DataSourceInfo connectionInfo;
    protected DataSource dataSource;
    protected Map<String, AccessStackAdapter> adapterMap;

    public CayenneResources(Map<String, AccessStackAdapter> adapterMap) {
        this.adapterMap = adapterMap;

        // kludge until we stop using Spring for unit tests and use Cayenne DI
        BatchQueryBuilderFactory factory = new DefaultBatchQueryBuilderFactory();
        for (AccessStackAdapter adapter : adapterMap.values()) {
            ((JdbcAdapter) adapter.getAdapter()).setBatchQueryBuilderFactory(factory);
        }

    }

    public void setConnectionInfo(DataSourceInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
        this.dataSource = createDataSource();
    }

    /**
     * Returns DB-specific testing adapter.
     */
    public AccessStackAdapter getAccessStackAdapter(String adapterClassName) {
        AccessStackAdapter stackAdapter = adapterMap.get(adapterClassName);

        if (stackAdapter == null) {
            throw new RuntimeException("No AccessStackAdapter for DbAdapter class: "
                    + adapterClassName);
        }

        return stackAdapter;
    }

    /**
     * Returns shared DataSource.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Creates new DataNode.
     */
    public DataNode newDataNode(String name) throws Exception {
        AccessStackAdapter adapter = getAccessStackAdapter(connectionInfo
                .getAdapterClassName());

        DataNode node = new DataNode(name);
        node.setDataSource(dataSource);
        node.setAdapter(adapter.getAdapter());
        return node;
    }

    public DataSource createDataSource() {

        try {
            PoolDataSource poolDS = new PoolDataSource(
                    connectionInfo.getJdbcDriver(),
                    connectionInfo.getDataSourceUrl());
            return new PoolManager(
                    poolDS,
                    1,
                    1,
                    connectionInfo.getUserName(),
                    connectionInfo.getPassword()) {

                @Override
                public void shutdown() throws SQLException {
                    // noop - make sure we are not shutdown by the test scope, but at the
                    // same time PoolManager methods are exposed (so we can't wrap
                    // PoolManager)
                }
            };
        }
        catch (Exception ex) {
            logger.error("Can not create shared data source.", ex);
            throw new RuntimeException("Can not create shared data source.", ex);
        }
    }

}
