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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.cayenne.datasource.PoolAwareConnection;
import org.apache.cayenne.datasource.UnmanagedPoolingDataSource;
import org.apache.cayenne.datasource.PoolingDataSourceParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class PoolingDataSourceTest {

	private DataSource nonPooling;
	private PoolingDataSourceParameters params;

	@Before
	public void before() throws SQLException {
		nonPooling = mock(DataSource.class);
		when(nonPooling.getConnection()).thenAnswer(new Answer<Connection>() {
			@Override
			public Connection answer(InvocationOnMock invocation) throws Throwable {
				return mock(Connection.class);
			}
		});
		params = new PoolingDataSourceParameters();
	}

	@Test
	public void testManagePool_High() throws SQLException {

		int max = 5;

		params.setMinConnections(1);
		params.setMaxConnections(max);
		UnmanagedPoolingDataSource ds = new UnmanagedPoolingDataSource(nonPooling, params);

		// opening and closing 'max' connections should fill the pool to the
		// top...
		Connection[] open = new Connection[max];
		for (int i = 0; i < max; i++) {
			open[i] = ds.getConnection();
		}

		for (Connection c : open) {
			c.close();
		}

		// now we can start calling 'managePool', and it would close connections
		// one at a time until we reach a threshold on idle
		assertEquals(max, ds.poolSize());
		ds.managePool();
		assertEquals(max - 1, ds.poolSize());
		ds.managePool();
		assertEquals(max - 2, ds.poolSize());

		// pool equilibrium was reached. subsequent calls should not open or
		// close connections
		ds.managePool();
		assertEquals(max - 2, ds.poolSize());
		ds.managePool();
		assertEquals(max - 2, ds.poolSize());
	}

	@Test
	public void testManagePool_Low() throws SQLException {

		int min = 2;

		params.setMinConnections(min);
		params.setMaxConnections(min + 5);
		UnmanagedPoolingDataSource ds = new UnmanagedPoolingDataSource(nonPooling, params);

		// we start with a min number of connections
		assertEquals(min, ds.poolSize());

		// now lets evict a bunch of connections before we can start growing the
		// pool again
		for (int i = 0; i < min; i++) {
			ds.retire(ds.uncheckNonBlocking(false));
		}

		// now we can start calling 'managePool', and it would open connections
		// one at a time
		assertEquals(0, ds.poolSize());
		ds.managePool();
		assertEquals(1, ds.poolSize());
		ds.managePool();
		assertEquals(2, ds.poolSize());

		// pool equilibrium was reached. subsequent calls should not open or
		// close connections
		ds.managePool();
		assertEquals(2, ds.poolSize());
		ds.managePool();
		assertEquals(2, ds.poolSize());
	}

	@Test
	public void testManagePool_Empty() throws SQLException {

		int max = 5;

		params.setMinConnections(1);
		params.setMaxConnections(max);
		UnmanagedPoolingDataSource ds = new UnmanagedPoolingDataSource(nonPooling, params);

		// opening and closing 'max' connections should fill the pool to the
		// top...
		Connection[] open = new Connection[max];
		for (int i = 0; i < max; i++) {
			open[i] = ds.getConnection();
		}

		// all connections are in use, so managePool should do nothing
		assertEquals(max, ds.poolSize());
		ds.managePool();
		assertEquals(max, ds.poolSize());
	}

	@Test
	public void testValidateUnchecked() {

		final PoolAwareConnection[] connections = validConnections(4);

		params.setMinConnections(4);
		params.setMaxConnections(10);

		UnmanagedPoolingDataSource ds = new UnmanagedPoolingDataSource(nonPooling, params) {

			int i;

			@Override
			PoolAwareConnection createWrapped() throws SQLException {
				return connections[i++];
			}
		};

		// now that the pool is created, invalidate a few leading connections
		when(connections[0].validate()).thenReturn(false);
		when(connections[1].validate()).thenReturn(false);

		Connection faceHeadConnection = mock(Connection.class);
		PoolAwareConnection fakeHead = mock(PoolAwareConnection.class);
		when(fakeHead.getConnection()).thenReturn(faceHeadConnection);

		assertSame(connections[2], ds.validateUnchecked(fakeHead));
	}

	@Test
	public void testGetConnection_UpperCap() throws SQLException {
		int max = 5;
		params.setMaxConnections(max);
		params.setMaxQueueWaitTime(1000);
		UnmanagedPoolingDataSource ds = new UnmanagedPoolingDataSource(nonPooling, params);

		Connection[] unchecked = new Connection[max];

		for (int i = 0; i < max; i++) {
			unchecked[i] = ds.getConnection();
		}

		try {
			ds.getConnection();
			fail("Pool overflow not checked");
		} catch (SQLException e) {
			// expected ... all connections are taken
		}

		// return one connection ... it should become immediately available
		unchecked[0].close();

		Connection c = ds.getConnection();
		assertNotNull(c);
	}

	PoolAwareConnection[] validConnections(int size) {
		PoolAwareConnection[] connections = new PoolAwareConnection[size];
		for (int i = 0; i < size; i++) {
			Connection c = mock(Connection.class);

			connections[i] = mock(PoolAwareConnection.class);
			when(connections[i].getConnection()).thenReturn(c);
			when(connections[i].validate()).thenReturn(true);
		}
		return connections;
	}

}
