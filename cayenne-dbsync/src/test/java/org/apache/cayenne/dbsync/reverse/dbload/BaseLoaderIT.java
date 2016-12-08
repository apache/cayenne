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

package org.apache.cayenne.dbsync.reverse.dbload;

import java.sql.Connection;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DetectedDbEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class BaseLoaderIT extends ServerCase {

    static final DbLoaderConfiguration EMPTY_CONFIG = new DbLoaderConfiguration();

    @Inject
    protected DbAdapter adapter;

    @Inject
    protected ServerRuntime runtime;

    @Inject
    protected ServerCaseDataSourceFactory dataSourceFactory;

    @Inject
    protected UnitDbAdapter accessStackAdapter;

    Connection connection;

    DbLoadDataStore store;

    @Before
    public void before() throws Exception {
        store = new DbLoadDataStore();
        assertTrue("Store is not empty", store.getDbEntities().isEmpty());
        this.connection = dataSourceFactory.getSharedDataSource().getConnection();
    }

    @After
    public void after() throws Exception {
        connection.close();
    }

    void createDbEntities() {
        String[] names = {"ARTIST", "BLOB_TEST", "CLOB_TEST", "GENERATED_COLUMN_TEST"};
        for(String name : names) {
            createEntity(name);
        }
    }

    void createEntity(String name) {
        store.addDbEntity(new DetectedDbEntity(name));
    }

    DbEntity getDbEntity(String name) {
        DbEntity de = store.getDbEntity(name);
        // sometimes table names get converted to lowercase
        if (de == null) {
            de = store.getDbEntity(name.toLowerCase());
        }
        return de;
    }
}
