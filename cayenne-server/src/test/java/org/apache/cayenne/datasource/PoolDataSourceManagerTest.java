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
package org.apache.cayenne.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class PoolDataSourceManagerTest {

	private UnmanagedPoolingDataSource mockPoolingDataSource;
	private PoolingDataSourceManager dataSourceManager;

	@Before
	public void before() throws SQLException {
		this.mockPoolingDataSource = mock(UnmanagedPoolingDataSource.class);
		this.dataSourceManager = new PoolingDataSourceManager(mockPoolingDataSource, 100);
	}

	@After
	public void after() {
		dataSourceManager.shutdown();
	}

	@Test
	public void testRun_Manage() throws InterruptedException {

		final int[] counter = new int[1];

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				counter[0]++;
				return null;
			}
		}).when(mockPoolingDataSource).managePool();

		dataSourceManager.start();

		// we can't predict the number of 'managePool' invocations, but it
		// should be incrementing as the time goes

		int c0 = counter[0];
		assertEquals(0, c0);
		Thread.sleep(300);

		int c1 = counter[0];
		assertTrue(c1 > c0);

		Thread.sleep(300);

		int c2 = counter[0];
		assertTrue(c2 > c1);
	}
}
