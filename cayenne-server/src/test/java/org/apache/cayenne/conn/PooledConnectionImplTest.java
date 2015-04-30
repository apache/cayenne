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

package org.apache.cayenne.conn;

import static org.mockito.Mockito.mock;

import java.sql.SQLException;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

public class PooledConnectionImplTest {

	private PoolingDataSource dataSource;

	@Before
	public void before() throws SQLException {
		PoolingDataSourceParameters poolParameters = new PoolingDataSourceParameters();
		poolParameters.setMinConnections(0);
		poolParameters.setMaxConnections(0);
		poolParameters.setMaxQueueWaitTime(1000);

		this.dataSource = new PoolingDataSource(mock(ConnectionPoolDataSource.class), poolParameters);
	}

	@Test
	public void testConnectionErrorNotificationConcurrency() throws Exception {
		// test a case when error notification is sent to connection
		// that has been removed from the pool, but when pool is still a
		// listener for its events.

		PooledConnectionImpl con = new PooledConnectionImpl(mock(DataSource.class), "un", "password");
		con.addConnectionEventListener(dataSource);
		con.connectionErrorNotification(new java.sql.SQLException("Bad SQL Exception.."));
	}

	@Test
	public void testConnectionClosedNotificationConcurrency() throws Exception {
		// test a case when closed notification is sent to connection
		// that has been removed from the pool, but when pool is still a
		// listener for its events.
		PooledConnectionImpl con = new PooledConnectionImpl(mock(DataSource.class), "un", "password");
		con.addConnectionEventListener(dataSource);
		con.connectionClosedNotification();
	}

}
