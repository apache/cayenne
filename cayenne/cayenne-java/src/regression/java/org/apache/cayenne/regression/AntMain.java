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


package org.apache.cayenne.regression;

import javax.sql.DataSource;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.conn.PoolDataSource;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.dba.AutoAdapter;

/**
 * Runs regression test based on a set of properties
 * 
 * @author Andrei Adamchik
 */
public class AntMain extends Main {

    /**
     * Uses System properties to configure regression tests.
     */
    public static void main(String[] args) {
        TestPreferences prefs;

        Configuration.configureCommonLogging();

        try {
            prefs = new TestPreferences(System.getProperties());
        }
        catch (Exception ex) {
            System.out.println("Fatal Error: " + ex.getMessage());
            System.exit(1);
            return;
        }

        if (new AntMain(prefs).execute()) {
            System.exit(1);
        }

        System.exit(0);
    }

    /**
     * Constructor for AntMain.
     */
    public AntMain(TestPreferences prefs) {
        super(prefs);
    }

    protected DataDomain createDomain() throws Exception {
        ClassLoader loader = new DOStubClassLoader();
        Thread.currentThread().setContextClassLoader(loader);

        DataSourceInfo info = ((TestPreferences) prefs).getConnectionInfo();

        // data source
        PoolDataSource poolDS = new PoolDataSource(info.getJdbcDriver(), info
                .getDataSourceUrl());
        DataSource ds = new PoolManager(poolDS, 1, 1, info.getUserName(), info
                .getPassword());

   
        DataNode node = new DataNode("node");
        node.setAdapter(new AutoAdapter(ds));
        node.setDataSource(ds);

        // domain
        DataDomain domain = new DataDomain("domain");
        domain.addNode(node);
        return domain;
    }
}
