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
package org.apache.cayenne.unit.di.server;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.conn.PoolDataSource;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.di.Inject;

public class ServerCaseDataSourceFactory  {

    private DataSource sharedDataSource;
    private DataSourceInfo dataSourceInfo;
    private Map<String, DataSource> dataSources;
    private Set<String> mapsWithDedicatedDataSource;

    public ServerCaseDataSourceFactory(@Inject DataSourceInfo dataSourceInfo) {

        this.dataSourceInfo = dataSourceInfo;
        this.dataSources = new HashMap<String, DataSource>();
        this.mapsWithDedicatedDataSource = new HashSet<String>(Arrays.asList(
                "map-db1",
                "map-db2"));

        this.sharedDataSource = createDataSource();
    }

    public DataSource getSharedDataSource() {
        return sharedDataSource;
    }

    public DataSource getDataSource(String dataMapName) {
        DataSource ds = dataSources.get(dataMapName);
        if (ds == null) {

            ds = mapsWithDedicatedDataSource.contains(dataMapName)
                    ? createDataSource()
                    : sharedDataSource;

            dataSources.put(dataMapName, ds);
        }

        return ds;
    }

    private DataSource createDataSource() {
        try {
            PoolDataSource poolDS = new PoolDataSource(
                    dataSourceInfo.getJdbcDriver(),
                    dataSourceInfo.getDataSourceUrl());
            return new PoolManager(
                    poolDS,
                    dataSourceInfo.getMinConnections(),
                    dataSourceInfo.getMaxConnections(),
                    dataSourceInfo.getUserName(),
                    dataSourceInfo.getPassword(), PoolManager.MAX_QUEUE_WAIT_DEFAULT) {

                @Override
                public void shutdown() throws SQLException {
                    // noop - make sure we are not shutdown by the test scope, but at the
                    // same time PoolManager methods are exposed (so we can't wrap
                    // PoolManager)
                }
            };
        }
        catch (Exception ex) {
            throw new RuntimeException("Can not create shared data source.", ex);
        }
    }

}
