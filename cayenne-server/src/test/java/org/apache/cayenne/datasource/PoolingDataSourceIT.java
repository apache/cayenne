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

package org.apache.cayenne.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class PoolingDataSourceIT extends BasePoolingDataSourceIT {

	@Test(expected = UnsupportedOperationException.class)
	public void testGetConnectionWithUserName() throws Exception {
		dataSource.getConnection("user", "password");
	}

	@Test
	public void testGetConnection_AutoCommit() throws Exception {

		assertTrue(dataSource.getMaxConnections() > 0);

		List<Connection> connections = new ArrayList<Connection>();
		try {

			for (int i = 0; i < dataSource.getMaxConnections(); i++) {
				Connection c = dataSource.getConnection();
				assertTrue("Failed to reset connection state", c.getAutoCommit());
				connections.add(c);
			}

			for (Connection c : connections) {
				c.setAutoCommit(false);
				c.close();
			}

			for (int i = 0; i < dataSource.getMaxConnections(); i++) {
				Connection c = dataSource.getConnection();

				// presumably this pass through the pool should return existing
				// connections
				assertTrue(connections.contains(c));
				assertTrue("Failed to reset connection state for reused connection", c.getAutoCommit());
			}

		} finally {
			for (Connection c : connections) {
				try {
					c.close();
				} catch (SQLException e) {

				}
			}
		}
	}

	@Test
	public void testGetConnection_FailOnFull() throws Exception {

		assertTrue(dataSource.getMaxConnections() > 0);

		List<Connection> connections = new ArrayList<Connection>();
		try {

			for (int i = 0; i < dataSource.getMaxConnections(); i++) {
				connections.add(dataSource.getConnection());
			}

			long t0 = System.currentTimeMillis();
			try {

				dataSource.getConnection();
				fail("Opening more connections than the pool allows succeeeded");
			} catch (SQLException e) {
				// expected, but check if we waited sufficiently

				long t1 = System.currentTimeMillis();
				assertTrue(t1 - t0 >= QUEUE_WAIT_TIME);
			}

		} finally {
			for (Connection c : connections) {
				try {
					c.close();
				} catch (SQLException e) {

				}
			}
		}
	}

	@Test
	public void testGetConnection() throws Exception {

		assertEquals(2, dataSource.poolSize());
		assertEquals(2, dataSource.availableSize());

		Connection c1 = dataSource.getConnection();
		assertEquals(2, dataSource.poolSize());
		assertEquals(1, dataSource.availableSize());

		Connection c2 = dataSource.getConnection();
		assertEquals(2, dataSource.poolSize());
		assertEquals(0, dataSource.availableSize());

		Connection c3 = dataSource.getConnection();
		assertEquals(3, dataSource.poolSize());
		assertEquals(0, dataSource.availableSize());

		c1.close();
		assertEquals(3, dataSource.poolSize());
		assertEquals(1, dataSource.availableSize());

		c2.close();
		assertEquals(3, dataSource.poolSize());
		assertEquals(2, dataSource.availableSize());

		c3.close();
		assertEquals(3, dataSource.poolSize());
		assertEquals(3, dataSource.availableSize());
	}

	@Test
	public void testGetConnection_BeforeScopeEnd() throws Exception {

		assertEquals(2, dataSource.poolSize());
		assertEquals(2, dataSource.availableSize());

		dataSource.close();

		assertEquals(0, dataSource.poolSize());
		assertEquals(0, dataSource.availableSize());
	}

	@Test
	public void testGetConnection_Concurrent() {

		PoolTask[] tasks = new PoolTask[2];
		for (int i = 0; i < tasks.length; i++) {
			tasks[i] = new PoolTask();
		}

		ExecutorService executor = Executors.newFixedThreadPool(tasks.length);

		for (int j = 0; j < 100; j++) {
			for (int i = 0; i < tasks.length; i++) {
				executor.submit(tasks[i]);
			}
		}

		// check for completion or deadlock
		executor.shutdown();
		try {
			// normally this completes in less than 2 seconds. If it takes 30
			// then it failed.
			boolean didFinish = executor.awaitTermination(30, TimeUnit.SECONDS);
			if (!didFinish) {
				fail("Connection pool either deadlocked or contended over the lock too long.");
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		for (int i = 0; i < tasks.length; i++) {
			assertEquals(100, tasks[i].i.get());
		}
	}

	class PoolTask implements Runnable {

		AtomicInteger i = new AtomicInteger();

		@Override
		public void run() {

			i.incrementAndGet();
			try {
				Connection c = dataSource.getConnection();
				try {

					Statement st = c.createStatement();
					try {

						ResultSet rs = st.executeQuery("SELECT ARTIST_ID FROM ARTIST");

						try {
							rs.next();

						} finally {
							rs.close();
						}
					} finally {
						st.close();
					}

				} finally {
					c.close();
				}

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}
}
