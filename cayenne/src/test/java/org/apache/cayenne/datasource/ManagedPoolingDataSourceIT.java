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

import org.apache.cayenne.unit.jdbc.TestConnection;
import org.apache.cayenne.unit.jdbc.TestDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ManagedPoolingDataSourceIT {

	private static final Logger LOGGER = LoggerFactory.getLogger(ManagedPoolingDataSourceIT.class);

	private int poolSize;
	private OnOffDataSourceManager dataSourceManager;
    private ManagedPoolingDataSource managedPool;

	@BeforeEach
	public void before() throws SQLException {

		this.poolSize = 4;
		this.dataSourceManager = new OnOffDataSourceManager();

		PoolingDataSourceParameters parameters = new PoolingDataSourceParameters();
		parameters.setMaxConnections(poolSize);
		parameters.setMinConnections(poolSize / 2);
		parameters.setMaxQueueWaitTime(1000);
		parameters.setValidationQuery("SELECT 1");
        UnmanagedPoolingDataSource unmanagedPool =
                new UnmanagedPoolingDataSource(dataSourceManager.dataSource, parameters);
		this.managedPool = new ManagedPoolingDataSource(unmanagedPool, 10000);
	}

	@AfterEach
	public void after() {
		if (managedPool != null) {
			managedPool.close();
		}
	}

	private Collection<PoolTask> createTasks(int size) {
		Collection<PoolTask> tasks = new ArrayList<>();

		for (int i = 0; i < size; i++) {
			tasks.add(new PoolTask());
		}
		return tasks;
	}

	@Test
	public void getConnection_OnBackendShutdown() throws SQLException, InterruptedException {

		// note that this assertion can only work reliably when the pool is inactive...
		assertEquals(poolSize, managedPool.poolSize() + managedPool.canExpandSize());

		Collection<PoolTask> tasks = createTasks(4);
		ExecutorService executor = Executors.newFixedThreadPool(4);

		for (int j = 0; j < 10; j++) {
			for (PoolTask task : tasks) {
				executor.submit(task);
			}
		}

		dataSourceManager.off();
		Thread.sleep(500);

		for (int j = 0; j < 10; j++) {
			for (PoolTask task : tasks) {
				executor.submit(task);
			}
		}

		Thread.sleep(100);

		dataSourceManager.on();

		for (int j = 0; j < 10; j++) {
			for (PoolTask task : tasks) {
				executor.submit(task);
			}
		}

		executor.shutdown();
		executor.awaitTermination(2, TimeUnit.SECONDS);

		// note that this assertion can only work reliably when the pool is inactive...
		assertEquals(poolSize, managedPool.poolSize() + managedPool.canExpandSize());
	}

	class PoolTask implements Runnable {

		@Override
		public void run() {

			try (Connection c = managedPool.getConnection()) {
				try (Statement ignored = c.createStatement()) {
					try {
						Thread.sleep(40);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			} catch (SQLException e) {
				if (OnOffDataSourceManager.NO_CONNECTIONS_MESSAGE.equals(e.getMessage())) {
					LOGGER.info("db down...");
				} else {
					LOGGER.warn("error getting connection", e);
				}
			}
		}
	}

	static class OnOffDataSourceManager {

		static final String NO_CONNECTIONS_MESSAGE = "no connections at the moment";

		private volatile boolean on = true;

		// opens a new connection on every call, unless the DB is "off"
		private final DataSource dataSource = new TestDataSource() {
			@Override
			public Connection getConnection() throws SQLException {
				if (!on) {
					throw new SQLException(NO_CONNECTIONS_MESSAGE);
				}
				return new TestConnection();
			}
		};

		void off() {
			on = false;
		}

		void on() {
			on = true;
		}
	}
}
