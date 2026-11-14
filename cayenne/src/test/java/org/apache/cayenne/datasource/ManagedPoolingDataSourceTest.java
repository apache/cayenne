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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.cayenne.unit.jdbc.TestConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ManagedPoolingDataSourceTest {

	private Connection[] connections;
	private StubPoolingDataSource poolingDataSource;
	private ManagedPoolingDataSource dataSource;

	@BeforeEach
	public void before() {

		this.connections = new Connection[4];
		for (int i = 0; i < connections.length; i++) {
			connections[i] = new TestConnection();
		}

		this.poolingDataSource = new StubPoolingDataSource(connections);
		this.dataSource = new ManagedPoolingDataSource(poolingDataSource);
	}

	@AfterEach
	public void after() {
		dataSource.beforeScopeEnd();
	}

	@Test
	public void getConnection() throws SQLException {
		assertSame(connections[0], dataSource.getConnection());
		assertSame(connections[1], dataSource.getConnection());
		assertSame(connections[2], dataSource.getConnection());
		assertSame(connections[3], dataSource.getConnection());
	}

	@Test
	public void close() throws SQLException, InterruptedException {
		assertNotNull(dataSource.getConnection());

		// state before shutdown
		assertEquals(0, poolingDataSource.closeCalls.get());
		assertFalse(dataSource.getDataSourceManager().isStopped());
		assertTrue(dataSource.getDataSourceManager().isAlive());

		dataSource.close();

		// state after shutdown
		assertEquals(1, poolingDataSource.closeCalls.get());
		assertTrue(dataSource.getDataSourceManager().isStopped());

		// give the thread some time to process interrupt and die
		Thread.sleep(200);
		assertFalse(dataSource.getDataSourceManager().isAlive());

		try {
			dataSource.getConnection();
		} catch (SQLException e) {
			// expected , DataSource should not give out connections any longer
		}
	}

}
