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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ManagedPoolingDataSourceTest {

	private Connection[] mockConnections;
	private UnmanagedPoolingDataSource mockPoolingDataSource;
	private ManagedPoolingDataSource dataSource;

	@Before
	public void before() throws SQLException {

		this.mockConnections = new Connection[4];
		for (int i = 0; i < mockConnections.length; i++) {
			mockConnections[i] = mock(Connection.class);
		}

		this.mockPoolingDataSource = mock(UnmanagedPoolingDataSource.class);
		when(mockPoolingDataSource.getConnection()).thenReturn(mockConnections[0], mockConnections[1],
				mockConnections[2], mockConnections[3]);

		this.dataSource = new ManagedPoolingDataSource(mockPoolingDataSource);
	}

	@After
	public void after() {
		dataSource.beforeScopeEnd();
	}

	@Test
	public void testGetConnection() throws SQLException {
		assertSame(mockConnections[0], dataSource.getConnection());
		assertSame(mockConnections[1], dataSource.getConnection());
		assertSame(mockConnections[2], dataSource.getConnection());
		assertSame(mockConnections[3], dataSource.getConnection());
	}

	@Test
	public void testClose() throws SQLException, InterruptedException {
		assertNotNull(dataSource.getConnection());

		// state before shutdown
		verify(mockPoolingDataSource, times(0)).close();
		assertFalse(dataSource.getDataSourceManager().isStopped());
		assertTrue(dataSource.getDataSourceManager().isAlive());

		dataSource.close();

		// state after shutdown
		verify(mockPoolingDataSource, times(1)).close();
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
