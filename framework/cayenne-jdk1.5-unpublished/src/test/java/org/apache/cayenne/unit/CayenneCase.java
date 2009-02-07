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

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.UnitTestDomain;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.DefaultConfiguration;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;

/**
 * Superclass of Cayenne test cases. Provides access to shared connection resources.
 * 
 */
public abstract class CayenneCase extends BasicCase {

    public static final String TEST_ACCESS_STACK = "TestStack";
    public static final String MULTI_TIER_ACCESS_STACK = "MultiTierStack";
    public static final String QUALIFIED_ACCESS_STACK = "QualifiedStack";
    public static final String QUOTEMAP_ACCESS_STACK = "QuoteMapStack";

    static {
        // create dummy shared config
        Configuration config = new DefaultConfiguration() {

            @Override
            public void initialize() {
            }
        };

        Configuration.initializeSharedConfiguration(config);

    }

    protected AccessStack accessStack;

    public CayenneCase() {
        // make sure CayenneTestResources shared instance is loaded
        CayenneResources.getResources();

        this.accessStack = buildAccessStack();
    }

    /**
     * A helper method that allows to block any query passing through the DataDomain, thus
     * allowing to check for stray fault firings during the test case. Must be paired with
     * <em>unblockQueries</em>.
     */
    protected void blockQueries() {
        getDomain().setBlockingQueries(true);
    }

    protected void unblockQueries() {
        getDomain().setBlockingQueries(false);
    }

    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(TEST_ACCESS_STACK);
    }

    protected AccessStackAdapter getAccessStackAdapter() {
        return accessStack.getAdapter(getNode());
    }

    protected DataSourceInfo getConnectionInfo() throws Exception {
        return CayenneResources.getResources().getConnectionInfo();
    }

    protected Connection getConnection() throws Exception {
        return getNode().getDataSource().getConnection();
    }

    protected UnitTestDomain getDomain() {
        return accessStack.getDataDomain();
    }

    protected SQLTemplateCustomizer getSQLTemplateBuilder() {
        SQLTemplateCustomizer customizer = CayenneResources
                .getResources()
                .getSQLTemplateCustomizer();

        // make sure adapter is correct
        customizer.setAdapter(getAccessStackAdapter().getAdapter());
        return customizer;
    }

    /**
     * Creates test data via a mechanism preconfigured in the access stack. Default
     * mechanism is loading test data DML from XML file.
     */
    protected void createTestData(String testName) throws Exception {
        accessStack.createTestData(this.getClass(), testName, Collections.EMPTY_MAP);
    }

    protected void createTestData(String testName, Map parameters) throws Exception {
        accessStack.createTestData(this.getClass(), testName, parameters);
    }

    protected DataNode getNode() {
        return getDomain().getDataNodes().iterator().next();
    }

    protected DataContext createDataContext() {
        return createDataContextWithSharedCache();
    }

    /**
     * Creates a DataContext that uses shared snapshot cache and is based on default test
     * domain.
     */
    protected DataContext createDataContextWithSharedCache() {
        // remove listeners for snapshot events
        getDomain().getEventManager().removeAllListeners(
                getDomain().getSharedSnapshotCache().getSnapshotEventSubject());

        // clear cache...
        getDomain().getSharedSnapshotCache().clear();
        getDomain().getQueryCache().clear();
        DataContext context = getDomain().createDataContext(true);

        assertSame(getDomain().getSharedSnapshotCache(), context
                .getObjectStore()
                .getDataRowCache());

        return context;
    }

    /**
     * Creates a DataContext that uses local snapshot cache and is based on default test
     * domain.
     */
    protected DataContext createDataContextWithDedicatedCache() {
        DataContext context = getDomain().createDataContext(false);

        assertNotSame(getDomain().getSharedSnapshotCache(), context
                .getObjectStore()
                .getDataRowCache());

        return context;
    }

    /**
     * Returns AccessStack.
     */
    protected AccessStack getAccessStack() {
        return accessStack;
    }

    protected void deleteTestData() throws Exception {
        accessStack.deleteTestData();
    }

    protected DbEntity getDbEntity(String dbEntityName) {
        // retrieve DbEntity the hard way, bypassing the resolver...

        for (DataMap map : getDomain().getDataMaps()) {
            for (DbEntity e : map.getDbEntities()) {
                if (dbEntityName.equals(e.getName())) {
                    return e;
                }
            }
        }

        throw new CayenneRuntimeException("No DbEntity found: " + dbEntityName);
    }

    protected ObjEntity getObjEntity(String objEntityName) {
        // retrieve ObjEntity the hard way, bypassing the resolver...
        for (DataMap map : getDomain().getDataMaps()) {
            for (ObjEntity e : map.getObjEntities()) {
                if (objEntityName.equals(e.getName())) {
                    return e;
                }
            }
        }

        throw new CayenneRuntimeException("No ObjEntity found: " + objEntityName);
    }
}
