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

import junit.framework.TestCase;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.UnitTestDomain;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;

/**
 * Superclass of Cayenne test cases. Provides access to shared connection resources.
 */
public abstract class CayenneCase extends TestCase {

    public static final String TEST_ACCESS_STACK = "TestStack";
    public static final String MULTI_TIER_ACCESS_STACK = "MultiTierStack";

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

    protected DBHelper getDbHelper() {
        return new DBHelper(getNode().getDataSource());
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

    protected DataNode getNode() {
        return getDomain().getDataNodes().iterator().next();
    }

    protected DataContext createDataContext() {
        return createDataContextWithSharedCache(true);
    }

    /**
     * Creates a DataContext that uses shared snapshot cache and is based on default test
     * domain.
     */
    protected DataContext createDataContextWithSharedCache(boolean clearCache) {
        // remove listeners for snapshot events
        if (clearCache) {
            getDomain().getEventManager().removeAllListeners(
                    getDomain().getSharedSnapshotCache().getSnapshotEventSubject());

            // clear cache...
            getDomain().getSharedSnapshotCache().clear();

            if (getDomain().getQueryCache() != null) {
                getDomain().getQueryCache().clear();
            }
        }
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
