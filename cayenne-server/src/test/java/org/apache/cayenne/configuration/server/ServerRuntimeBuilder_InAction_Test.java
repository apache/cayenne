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
package org.apache.cayenne.configuration.server;

import java.util.List;

import javax.sql.DataSource;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SQLSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class ServerRuntimeBuilder_InAction_Test extends ServerCase {

    @Inject
    private DBHelper dbHelper;

    @Inject
    private ServerRuntime runtime;

    @Inject
    private DataSourceInfo dsi;

    private DataSource dataSource;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
        dbHelper.deleteAll("EXHIBIT");
        dbHelper.deleteAll("GALLERY");

        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
        tArtist.insert(33001, "AA1");
        tArtist.insert(33002, "AA2");

        this.dataSource = runtime.getDataSource("tstmap");
    }

    public void testConfigFree_WithDataSource() {

        ServerRuntime localRuntime = new ServerRuntimeBuilder().dataSource(dataSource).build();

        try {
            List<DataRow> result = SQLSelect.dataRowQuery("SELECT * FROM ARTIST").select(localRuntime.newContext());
            assertEquals(2, result.size());
        } finally {
            localRuntime.shutdown();
        }
    }

    public void testConfigFree_WithDBParams() {

        ServerRuntime localRuntime = new ServerRuntimeBuilder().jdbcDriver(dsi.getJdbcDriver())
                .url(dsi.getDataSourceUrl()).password(dsi.getPassword()).user(dsi.getUserName()).minConnections(1)
                .maxConnections(2).build();

        try {
            List<DataRow> result = SQLSelect.dataRowQuery("SELECT * FROM ARTIST").select(localRuntime.newContext());
            assertEquals(2, result.size());
        } finally {
            localRuntime.shutdown();
        }
    }
}
