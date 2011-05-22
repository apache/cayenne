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

package org.apache.cayenne.conn;

import java.sql.Connection;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class PoolManagerTest extends ServerCase {

    @Inject
    private DataSourceInfo dataSourceInfo;

    public void testDataSourceUrl() throws Exception {
        String driverName = dataSourceInfo.getJdbcDriver();
        String url = dataSourceInfo.getDataSourceUrl();

        PoolManager pm = new PoolManager(driverName, url, 0, 3, "", "") {

            @Override
            protected void startMaintenanceThread() {
            }
        };
        assertEquals(url, pm.getDataSourceUrl());
        assertEquals(driverName, pm.getJdbcDriver());
    }

    public void testPassword() throws Exception {
        PoolManager pm = new PoolManager(null, 0, 3, "", "b") {

            @Override
            protected void startMaintenanceThread() {
            }
        };
        assertEquals("b", pm.getPassword());
    }

    public void testUserName() throws Exception {
        PoolManager pm = new PoolManager(null, 0, 3, "a", "") {

            @Override
            protected void startMaintenanceThread() {
            }
        };
        assertEquals("a", pm.getUserName());
    }

    public void testMinConnections() throws Exception {
        PoolManager pm = new PoolManager(null, 0, 3, "", "") {

            @Override
            protected void startMaintenanceThread() {
            }
        };
        assertEquals(0, pm.getMinConnections());
    }

    public void testMaxConnections() throws Exception {
        PoolManager pm = new PoolManager(null, 0, 3, "", "") {

            @Override
            protected void startMaintenanceThread() {
            }
        };
        assertEquals(3, pm.getMaxConnections());
    }

    public void testPooling() throws Exception {

        PoolManager pm = new PoolManager(dataSourceInfo.getJdbcDriver(), dataSourceInfo
                .getDataSourceUrl(), 2, 3, dataSourceInfo.getUserName(), dataSourceInfo
                .getPassword());

        try {
            assertEquals(0, pm.getCurrentlyInUse());
            assertEquals(2, pm.getCurrentlyUnused());

            Connection c1 = pm.getConnection();
            assertEquals(1, pm.getCurrentlyInUse());
            assertEquals(1, pm.getCurrentlyUnused());

            Connection c2 = pm.getConnection();
            assertEquals(2, pm.getCurrentlyInUse());
            assertEquals(0, pm.getCurrentlyUnused());

            c1.close();
            assertEquals(1, pm.getCurrentlyInUse());
            assertEquals(1, pm.getCurrentlyUnused());

            c2.close();
            assertEquals(0, pm.getCurrentlyInUse());
            assertEquals(2, pm.getCurrentlyUnused());
        }
        finally {
            pm.shutdown();
        }
    }
}
