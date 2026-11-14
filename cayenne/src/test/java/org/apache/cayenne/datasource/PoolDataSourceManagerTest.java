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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PoolDataSourceManagerTest {

	private StubPoolingDataSource poolingDataSource;
	private PoolingDataSourceManager dataSourceManager;

	@BeforeEach
	public void before() {
		this.poolingDataSource = new StubPoolingDataSource();
		this.dataSourceManager = new PoolingDataSourceManager(poolingDataSource, 100);
	}

	@AfterEach
	public void after() {
		dataSourceManager.shutdown();
	}

	@Test
	public void run_Manage() throws InterruptedException {

		dataSourceManager.start();

		// we can't predict the number of 'managePool' invocations, but it
		// should be incrementing as the time goes

		int c0 = poolingDataSource.managePoolCalls.get();
		assertEquals(0, c0);
		Thread.sleep(300);

		int c1 = poolingDataSource.managePoolCalls.get();
		assertTrue(c1 > c0);

		Thread.sleep(300);

		int c2 = poolingDataSource.managePoolCalls.get();
		assertTrue(c2 > c1);
	}
}
