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

/**
 * A thread that manages the state of a {@link UnmanagedPoolingDataSource} instance,
 * performing periodic expansion/contraction of pooled connections, and
 * orchestrating shutdown.
 * 
 * @since 4.0
 */
class PoolingDataSourceManager extends Thread {

	private volatile boolean shouldStop;
	private UnmanagedPoolingDataSource dataSource;
	private long managerWakeTime;

	PoolingDataSourceManager(UnmanagedPoolingDataSource dataSource, long managerWakeTime) {
		setName("PoolingDataSourceManager-" + dataSource.hashCode());
		setDaemon(true);

		this.dataSource = dataSource;
		this.shouldStop = false;
		this.managerWakeTime = managerWakeTime;
	}

	void shutdown() {
		shouldStop = true;
		dataSource.close();
		interrupt();
	}

	UnmanagedPoolingDataSource getDataSource() {
		return dataSource;
	}

	boolean isStopped() {
		return shouldStop;
	}

	@Override
	public void run() {
		while (true) {

			try {
				Thread.sleep(managerWakeTime);
			} catch (InterruptedException iex) {
				// ignore...
			}

			if (shouldStop) {
				break;
			}

			dataSource.managePool();
		}
	}
}