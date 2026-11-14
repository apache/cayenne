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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.cayenne.unit.jdbc.TestConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PoolAwareConnectionTest {

	private TestConnection connection;
	private StubPoolingDataSource parent;

	@BeforeEach
	public void before() {
		connection = new TestConnection();
		parent = new StubPoolingDataSource();
	}

	@Test
	public void recover() throws SQLException {
		TestConnection replacement = new TestConnection();
		parent.reconnects.add(replacement);

		PoolAwareConnection paConnection = new PoolAwareConnection(parent, connection, null);
		paConnection.recover(new SQLException("E1"));

		assertTrue(connection.isClosed());
		assertSame(replacement, paConnection.getConnection());
		assertEquals(1, parent.createUnwrappedCalls.get());
	}

	@Test
	public void prepareStatement() throws SQLException {
		PoolAwareConnection paConnection = new PoolAwareConnection(parent, connection, null);
		PreparedStatement st = paConnection.prepareStatement("SELECT 1");

		assertSame(connection.getPreparedStatements().get(0), st);
		assertEquals(0, parent.createUnwrappedCalls.get());
	}

	@Test
	public void prepareStatement_Recover() throws SQLException {
		connection.failPrepareStatement(new SQLException("E1"));
		TestConnection replacement = new TestConnection();
		parent.reconnects.add(replacement);

		PoolAwareConnection paConnection = new PoolAwareConnection(parent, connection, null);
		PreparedStatement st = paConnection.prepareStatement("SELECT 1");

		// the statement is prepared again over a new connection
		assertSame(replacement.getPreparedStatements().get(0), st);
	}

	@Test
	public void prepareStatement_Recover_Impossible() {
		SQLException original = new SQLException("E1");
		connection.failPrepareStatement(original);

		// no reconnects available
		PoolAwareConnection paConnection = new PoolAwareConnection(parent, connection, null);

		SQLException thrown = assertThrows(SQLException.class, () -> paConnection.prepareStatement("SELECT 1"));
		assertSame(original, thrown);
	}

}
