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

package org.apache.cayenne.access.jdbc;

import com.mockrunner.jdbc.PreparedStatementResultSetHandler;
import com.mockrunner.mock.jdbc.MockConnection;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.MockOperationObserver;
import org.apache.cayenne.access.OptimisticLockException;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.DeleteBatchTranslator;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.locking.SimpleLockingTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@UseCayenneRuntime(CayenneProjects.LOCKING_PROJECT)
public class BatchActionLockingIT extends RuntimeCase {

	@Inject
	protected CayenneRuntime runtime;

	@Inject
	private Injector injector;

	@Inject
	private AdhocObjectFactory objectFactory;

	@Test
	public void testRunAsIndividualQueriesSuccess() throws Exception {
		EntityResolver resolver = runtime.getDataDomain().getEntityResolver();

		// test with adapter that supports keys...
		JdbcAdapter adapter = buildAdapter(true);

		DbEntity dbEntity = resolver.getObjEntity(SimpleLockingTestEntity.class).getDbEntity();

		List<DbAttribute> qualifierAttributes = Arrays.asList(dbEntity.getAttribute("LOCKING_TEST_ID"),
				dbEntity.getAttribute("NAME"));

		Collection<String> nullAttributeNames = Collections.singleton("NAME");

		Map<String, Object> qualifierSnapshot = new HashMap<>();
		qualifierSnapshot.put("LOCKING_TEST_ID", 1);

		DeleteBatchQuery batchQuery = new DeleteBatchQuery(dbEntity, qualifierAttributes, nullAttributeNames, 5);
		batchQuery.setUsingOptimisticLocking(true);
		batchQuery.add(qualifierSnapshot);

		DeleteBatchTranslator batchQueryBuilder = new DeleteBatchTranslator(batchQuery, adapter);

		MockConnection mockConnection = new MockConnection();
		PreparedStatementResultSetHandler preparedStatementResultSetHandler = mockConnection
				.getPreparedStatementResultSetHandler();
		preparedStatementResultSetHandler.setExactMatch(false);
		preparedStatementResultSetHandler.setCaseSensitive(false);
		preparedStatementResultSetHandler.prepareUpdateCount("DELETE", 1);

		boolean generatesKeys = false;

		DataNode node = new DataNode();
		node.setAdapter(adapter);
		node.setEntityResolver(resolver);
		node.setRowReaderFactory(mock(RowReaderFactory.class));
		BatchAction action = new BatchAction(batchQuery, node, false);
		action.runAsIndividualQueries(mockConnection, batchQueryBuilder, new MockOperationObserver(), generatesKeys);
		assertEquals(0, mockConnection.getNumberCommits());
		assertEquals(0, mockConnection.getNumberRollbacks());
	}

	@Test
	public void testRunAsIndividualQueriesOptimisticLockingFailure() throws Exception {
		EntityResolver resolver = runtime.getDataDomain().getEntityResolver();

		// test with adapter that supports keys...
		JdbcAdapter adapter = buildAdapter(true);

		DbEntity dbEntity = resolver.getObjEntity(SimpleLockingTestEntity.class).getDbEntity();

		List<DbAttribute> qualifierAttributes = Arrays.asList(dbEntity.getAttribute("LOCKING_TEST_ID"),
				dbEntity.getAttribute("NAME"));

		Collection<String> nullAttributeNames = Collections.singleton("NAME");

		Map<String, Object> qualifierSnapshot = new HashMap<>();
		qualifierSnapshot.put("LOCKING_TEST_ID", 1);

		DeleteBatchQuery batchQuery = new DeleteBatchQuery(dbEntity, qualifierAttributes, nullAttributeNames, 5);
		batchQuery.setUsingOptimisticLocking(true);
		batchQuery.add(qualifierSnapshot);

		DeleteBatchTranslator batchQueryBuilder = new DeleteBatchTranslator(batchQuery, adapter);

		MockConnection mockConnection = new MockConnection();
		PreparedStatementResultSetHandler preparedStatementResultSetHandler = mockConnection
				.getPreparedStatementResultSetHandler();
		preparedStatementResultSetHandler.setExactMatch(false);
		preparedStatementResultSetHandler.setCaseSensitive(false);
		preparedStatementResultSetHandler.prepareUpdateCount("DELETE", 0);

		boolean generatesKeys = false;
		DataNode node = new DataNode();
		node.setAdapter(adapter);
		node.setEntityResolver(resolver);
		node.setRowReaderFactory(mock(RowReaderFactory.class));
		BatchAction action = new BatchAction(batchQuery, node, false);
		try {
			action.runAsIndividualQueries(mockConnection, batchQueryBuilder, new MockOperationObserver(), generatesKeys);
			fail("No OptimisticLockingFailureException thrown.");
		} catch (OptimisticLockException ignore) {
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
