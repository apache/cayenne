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

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class PoolingDataSource_FailingValidationQueryIT extends BasePoolingDataSourceIT {

	@Override
	protected PoolingDataSourceParameters createParameters() {
		PoolingDataSourceParameters params = super.createParameters();
		params.setValidationQuery("SELECT count(1) FROM NO_SUCH_TABLE");
		return params;
	}

	@Test(expected = SQLException.class)
	public void testGetConnection_ValidationQuery() throws Exception {
		Connection c1 = dataSource.getConnection();
		c1.close();
	}
}
