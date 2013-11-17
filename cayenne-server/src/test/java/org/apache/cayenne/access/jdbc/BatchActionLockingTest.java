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

package org.apache.cayenne.access.jdbc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.MockOperationObserver;
import org.apache.cayenne.access.OptimisticLockException;
import org.apache.cayenne.access.trans.DeleteBatchQueryBuilder;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.testdo.locking.SimpleLockingTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

import com.mockrunner.jdbc.PreparedStatementResultSetHandler;
import com.mockrunner.mock.jdbc.MockConnection;

@UseServerRuntime(ServerCase.LOCKING_PROJECT)
public class BatchActionLockingTest extends ServerCase {

    @Inject
    protected ServerRuntime runtime;

    @Inject
    private Injector injector;

    @Inject
    private AdhocObjectFactory objectFactory;

    public void testRunAsIndividualQueriesSuccess() throws Exception {
        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();

        // test with adapter that supports keys...
        JdbcAdapter adapter = buildAdapter(true);

        DbEntity dbEntity = resolver.getObjEntity(SimpleLockingTestEntity.class).getDbEntity();

        List<DbAttribute> qualifierAttributes = Arrays.asList(dbEntity.getAttribute("LOCKING_TEST_ID"),
                dbEntity.getAttribute("NAME"));

        Collection<String> nullAttributeNames = Collections.singleton("NAME");

        Map<String, Object> qualifierSnapshot = new HashMap<String, Object>();
        qualifierSnapshot.put("LOCKING_TEST_ID", new Integer(1));

        DeleteBatchQuery batchQuery = new DeleteBatchQuery(dbEntity, qualifierAttributes, nullAttributeNames, 5);
        batchQuery.setUsingOptimisticLocking(true);
        batchQuery.add(qualifierSnapshot);

        DeleteBatchQueryBuilder batchQueryBuilder = new DeleteBatchQueryBuilder(adapter);

        MockConnection mockConnection = new MockConnection();
        PreparedStatementResultSetHandler preparedStatementResultSetHandler = mockConnection
                .getPreparedStatementResultSetHandler();
        preparedStatementResultSetHandler.setExactMatch(false);
        preparedStatementResultSetHandler.setCaseSensitive(false);
        preparedStatementResultSetHandler.prepareUpdateCount("DELETE", 1);

        boolean generatesKeys = false;

        BatchAction action = new BatchAction(batchQuery, adapter, resolver);
        action.runAsIndividualQueries(mockConnection, batchQueryBuilder, new MockOperationObserver(), generatesKeys);
        assertEquals(0, mockConnection.getNumberCommits());
        assertEquals(0, mockConnection.getNumberRollbacks());
    }

    public void testRunAsIndividualQueriesOptimisticLockingFailure() throws Exception {
        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();

        // test with adapter that supports keys...
        JdbcAdapter adapter = buildAdapter(true);

        DbEntity dbEntity = resolver.getObjEntity(SimpleLockingTestEntity.class).getDbEntity();

        List<DbAttribute> qualifierAttributes = Arrays.asList(dbEntity.getAttribute("LOCKING_TEST_ID"),
                dbEntity.getAttribute("NAME"));

        Collection<String> nullAttributeNames = Collections.singleton("NAME");

        Map<String, Object> qualifierSnapshot = new HashMap<String, Object>();
        qualifierSnapshot.put("LOCKING_TEST_ID", new Integer(1));

        DeleteBatchQuery batchQuery = new DeleteBatchQuery(dbEntity, qualifierAttributes, nullAttributeNames, 5);
        batchQuery.setUsingOptimisticLocking(true);
        batchQuery.add(qualifierSnapshot);

        DeleteBatchQueryBuilder batchQueryBuilder = new DeleteBatchQueryBuilder(adapter);

        MockConnection mockConnection = new MockConnection();
        PreparedStatementResultSetHandler preparedStatementResultSetHandler = mockConnection
                .getPreparedStatementResultSetHandler();
        preparedStatementResultSetHandler.setExactMatch(false);
        preparedStatementResultSetHandler.setCaseSensitive(false);
        preparedStatementResultSetHandler.prepareUpdateCount("DELETE", 0);

        boolean generatesKeys = false;
        BatchAction action = new BatchAction(batchQuery, adapter, resolver);
        try {
            action.runAsIndividualQueries(mockConnection, batchQueryBuilder, new MockOperationObserver(), generatesKeys);
            fail("No OptimisticLockingFailureException thrown.");
        } catch (OptimisticLockException e) {
        }
        assertEquals(0, mockConnection.getNumberCommits());
        assertEquals(0, mockConnection.getNumberRollbacks());
    }

    JdbcAdapter buildAdapter(boolean supportGeneratedKeys) {
        JdbcAdapter adapter = objectFactory.newInstance(JdbcAdapter.class, JdbcAdapter.class.getName());
        adapter.setSupportsGeneratedKeys(supportGeneratedKeys);
        injector.injectMembers(adapter);
        return adapter;
    }
}
