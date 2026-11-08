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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.unit.DataNodes;
import org.apache.cayenne.access.OptimisticLockException;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.StatementContexts;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.TranslatedBatch;
import org.apache.cayenne.access.translator.batch.DeleteBatchTranslator;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.query.resolved.BatchRow;
import org.apache.cayenne.query.resolved.DeleteBatch;
import org.apache.cayenne.testdo.locking.SimpleLockingTestEntity;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.unit.jdbc.TestConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class DataNodeBatchActionLockingIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.LOCKING_PROJECT);

	private DeleteBatch lockingDelete(CompiledMapping mapping) {
		CompiledTable table = mapping.entity(SimpleLockingTestEntity.class).table();
		List<CompiledColumn> qualifierColumns = Arrays.asList(table.column("LOCKING_TEST_ID"),
				table.column("NAME"));
		Collection<String> nullAttributeNames = Collections.singleton("NAME");

		Map<String, Object> qualifierSnapshot = new HashMap<>();
		qualifierSnapshot.put("LOCKING_TEST_ID", 1);

		return new DeleteBatch(table, qualifierColumns, nullAttributeNames, true,
				List.of(new BatchRow(null, qualifierSnapshot, null)));
	}

	@Test
	public void runAsIndividualQueriesSuccess() throws Exception {
		CompiledMapping mapping = env.runtime().getDataDomain().getMapping();

		// test with adapter that supports keys...
		JdbcAdapter adapter = buildAdapter(true);
		DeleteBatch batchQuery = lockingDelete(mapping);
		TranslatedBatch translated = new DeleteBatchTranslator().translate(batchQuery, adapter);

		TestConnection mockConnection = new TestConnection();
		mockConnection.prepareUpdateCount("DELETE", 1);

		boolean generatesKeys = false;

		DataNode node = DataNodes.withAdapter(adapter, mock(RowReaderFactory.class));
		DataNodeBatchAction action = new DataNodeBatchAction(translated, node, false);
		StatementContext statementContext = StatementContexts.newRun(node, mapping, batchQuery);
		statementContext.setTranslatedStatement(translated);
		action.runAsIndividualQueries(mockConnection, statementContext, generatesKeys);

		assertEquals(0, mockConnection.getNumberCommits());
		assertEquals(0, mockConnection.getNumberRollbacks());
	}

	@Test
	public void runAsIndividualQueriesOptimisticLockingFailure() throws Exception {
		CompiledMapping mapping = env.runtime().getDataDomain().getMapping();

		// test with adapter that supports keys...
		JdbcAdapter adapter = buildAdapter(true);
		DeleteBatch batchQuery = lockingDelete(mapping);
		TranslatedBatch translated = new DeleteBatchTranslator().translate(batchQuery, adapter);

		TestConnection mockConnection = new TestConnection();
		mockConnection.prepareUpdateCount("DELETE", 0);

		boolean generatesKeys = false;

		DataNode node = DataNodes.withAdapter(adapter, mock(RowReaderFactory.class));
		DataNodeBatchAction action = new DataNodeBatchAction(translated, node, false);
		StatementContext statementContext = StatementContexts.newRun(node, mapping, batchQuery);
		statementContext.setTranslatedStatement(translated);

		assertThrows(OptimisticLockException.class, () ->
			action.runAsIndividualQueries(mockConnection, statementContext, generatesKeys));

		assertEquals(0, mockConnection.getNumberCommits());
		assertEquals(0, mockConnection.getNumberRollbacks());
	}

	JdbcAdapter buildAdapter(boolean supportGeneratedKeys) {
		JdbcAdapter adapter = env.adhocObjectFactory().newInstance(JdbcAdapter.class, JdbcAdapter.class.getName());
		adapter.setSupportsGeneratedKeys(supportGeneratedKeys);
		return adapter;
	}
}
