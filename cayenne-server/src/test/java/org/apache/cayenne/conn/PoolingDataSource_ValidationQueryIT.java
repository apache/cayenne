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

import static org.junit.Assert.assertEquals;

import java.sql.Connection;

import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class PoolingDataSource_ValidationQueryIT extends BasePoolingDataSourceIT {

	@Override
	protected PoolingDataSourceParameters createParameters() {
		PoolingDataSourceParameters params = super.createParameters();
		params.setValidationQuery("SELECT count(1) FROM ARTIST");
		return params;
	}

	@Test
	public void testGetConnection_ValidationQuery() throws Exception {

		assertEquals(0, dataSource.getCurrentlyInUse());
		assertEquals(2, dataSource.getCurrentlyUnused());

		// TODO: we are not testing much here... we really need to mock
		// validation query execution somehow and verify that it is taken into
		// account
		
		Connection c1 = dataSource.getConnection();
		assertEquals(1, dataSource.getCurrentlyInUse());
		assertEquals(1, dataSource.getCurrentlyUnused());

		c1.close();
	}
}
