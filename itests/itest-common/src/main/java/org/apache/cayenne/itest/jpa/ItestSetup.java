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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import javax.transaction.TransactionSynchronizationRegistry;

import junit.framework.Assert;

import org.apache.cayenne.itest.ItestDBUtils;
import org.apache.cayenne.itest.OpenEJBContainer;
import org.apache.openejb.persistence.JtaEntityManager;
import org.apache.openejb.persistence.JtaEntityManagerRegistry;

public class ItestSetup {

    public static final String SCHEMA_SCRIPT_URL = "itest.schema.script";
    public static final String DEFAULT_SCHEMA_SCRIPT = "schema-hsqldb.sql";

    public static final String JPA_UNIT_NAME = "itest.jpa.unit";
    public static final String DEFAULT_UNIT_NAME = "itest";

    private static ItestSetup sharedInstance;

    protected EntityManagerFactory sharedFactory;
    protected ItestDataSourceManager dataSourceManager;
    protected String jpaUnit;
    protected ItestDBUtils dbHelper;
    protected JtaEntityManagerRegistry openEJBEMRegistry;

    public static void initInstance(Properties properties) {
        String schemaScript = properties.getProperty(SCHEMA_SCRIPT_URL);
        if (schemaScript == null) {
            schemaScript = DEFAULT_SCHEMA_SCRIPT;
        }

        String jpaUnit = properties.getProperty(JPA_UNIT_NAME);
        if (jpaUnit == null) {
            jpaUnit = DEFAULT_UNIT_NAME;
        }

        sharedInstance = new ItestSetup(schemaScript, jpaUnit);
    }

    public static ItestSetup getInstance() {
        Assert.assertNotNull(
                "Null shared instance, call 'initInstance' first",
                sharedInstance);

        return sharedInstance;
    }

    protected ItestSetup(String schemaScriptUrl, String jpaUnit) {
        this.jpaUnit = jpaUnit;
        this.dataSourceManager = new ItestDataSourceManager(schemaScriptUrl);
        this.dbHelper = new ItestDBUtils(dataSourceManager.getDataSource());
    }

    protected JtaEntityManagerRegistry getOpenEJBEMRegistry() {
        if (openEJBEMRegistry == null) {
            TransactionSynchronizationRegistry txRegistry = OpenEJBContainer
                    .getContainer()
                    .getTxSyncRegistry();
            openEJBEMRegistry = new JtaEntityManagerRegistry(txRegistry);
        }

        return openEJBEMRegistry;
    }

    protected EntityManagerFactory getSharedFactory() {
        if (sharedFactory == null) {
            sharedFactory = createEntityManagerFactory();
        }

        return sharedFactory;
    }

    public DataSource getDataSource() {
        return dataSourceManager.getDataSource();
    }

    public EntityManager createEntityManager() {
        return getSharedFactory().createEntityManager();
    }

    public EntityManager createContainerManagedEntityManager() {
        return new JtaEntityManager(
                getOpenEJBEMRegistry(),
                getSharedFactory(),
                new Properties(),
                false);
    }

    public EntityManagerFactory createEntityManagerFactory() {
        Map properties = new HashMap();
        properties.put(
                "org.apache.cayenne.jpa.jpaDataSourceFactory",
                ItestJpaDataSourceFactory.class.getName());

        return Persistence.createEntityManagerFactory(jpaUnit, properties);
    }

    public ItestDBUtils getDbHelper() {
        return dbHelper;
    }
}
