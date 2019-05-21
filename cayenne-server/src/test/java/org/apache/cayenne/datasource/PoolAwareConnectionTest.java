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

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

public class PoolAwareConnectionTest {

	private UnmanagedPoolingDataSource parentMock;
	private Connection connectionMock;

	@Before
	public void before() throws SQLException {
		connectionMock = mock(Connection.class);

		parentMock = mock(UnmanagedPoolingDataSource.class);
		when(parentMock.createUnwrapped()).thenReturn(connectionMock);
	}

	@Test
	public void testRecover() throws SQLException {
		PoolAwareConnection paConnection = new PoolAwareConnection(parentMock, connectionMock, null);
		SQLException e = mock(SQLException.class);

		verify(parentMock, times(0)).createUnwrapped();
		paConnection.recover(e);
		verify(parentMock, times(1)).createUnwrapped();

		assertSame(connectionMock, paConnection.getConnection());
	}

	@Test
	public void testPrepareStatement() throws SQLException {
		PreparedStatement firstTry = mock(PreparedStatement.class);
		PreparedStatement secondTry = mock(PreparedStatement.class);

		when(connectionMock.prepareStatement(anyString())).thenReturn(firstTry, secondTry);

		PoolAwareConnection paConnection = new PoolAwareConnection(parentMock, connectionMock, null);
		PreparedStatement st = paConnection.prepareStatement("SELECT 1");
		assertSame(firstTry, st);
	}

	@Test
	public void testPrepareStatement_Recover() throws SQLException {

		PreparedStatement secondTry = mock(PreparedStatement.class);
		when(connectionMock.prepareStatement(anyString())).thenThrow(new SQLException("E1")).thenReturn(secondTry);

		PoolAwareConnection paConnection = new PoolAwareConnection(parentMock, connectionMock, null);
		PreparedStatement st = paConnection.prepareStatement("SELECT 1");
		assertSame(secondTry, st);
	}

	@Test
	public void testPrepareStatement_Recover_Impossible() throws SQLException {

		SQLException original = new SQLException("E1");
		when(connectionMock.prepareStatement(anyString())).thenThrow(original);
		PoolAwareConnection paConnection = new PoolAwareConnection(parentMock, connectionMock, null);

		try {
			paConnection.prepareStatement("SELECT 1");
		} catch (SQLException e) {
			assertSame(original, e);
		}

	}
}
