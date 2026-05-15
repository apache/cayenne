/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dbsync.reverse.dbload;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.model.DetectedDbEntity;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.unit.dba.TestDbAdapter;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaseLoaderIT {

    @RegisterExtension
    protected static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    static final DbLoaderConfiguration EMPTY_CONFIG = new DbLoaderConfiguration();

    protected DbAdapter adapter;
    protected CayenneRuntime runtime;
    protected TestDbAdapter accessStackAdapter;

    Connection connection;

    DbLoadDataStore store;

    @BeforeEach
    public void before() throws Exception {
        adapter = env.dataNode().getAdapter();
        runtime = env.runtime();
        accessStackAdapter = env.testDbAdapter();
        store = new DbLoadDataStore();
        assertTrue(store.getDbEntities().isEmpty(), "Store is not empty");
        this.connection = CayenneTestsEnv.DATA_SOURCES.sharedDataSource().getConnection();
    }

    @AfterEach
    public void after() throws Exception {
        connection.close();
    }

    void createDbEntities() {
        String[] names = {"ARTIST", "BLOB_TEST", "CLOB_TEST", "GENERATED_COLUMN_TEST"};
        for(String name : names) {
            createEntity(nameForDb(name));
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

    String nameForDb(String name) {
        if(accessStackAdapter.isLowerCaseNames()) {
            return name.toLowerCase();
        } else {
            return name.toUpperCase();
        }
    }
}
