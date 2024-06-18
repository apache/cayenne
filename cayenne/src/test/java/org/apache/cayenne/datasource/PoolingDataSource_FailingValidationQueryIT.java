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

import java.sql.Driver;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class PoolingDataSource_FailingValidationQueryIT extends RuntimeCase {

	@Inject
	private DataSourceDescriptor dataSourceInfo;

	@Inject
	private AdhocObjectFactory objectFactory;

	protected PoolingDataSourceParameters createParameters() {
		PoolingDataSourceParameters poolParameters = new PoolingDataSourceParameters();
		poolParameters.setMinConnections(2);
		poolParameters.setMaxConnections(3);
		poolParameters.setMaxQueueWaitTime(1000);
		poolParameters.setValidationQuery("SELECT count(1) FROM NO_SUCH_TABLE");
		return poolParameters;
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testConstructor() throws Exception {
		Driver driver = objectFactory.newInstance(Driver.class, dataSourceInfo.getJdbcDriver());
		DriverDataSource nonPooling = new DriverDataSource(driver, dataSourceInfo.getDataSourceUrl(),
				dataSourceInfo.getUserName(), dataSourceInfo.getPassword());

		PoolingDataSourceParameters poolParameters = createParameters();
		UnmanagedPoolingDataSource ds = new UnmanagedPoolingDataSource(nonPooling, poolParameters);

		// the exception should happen in the line above... this line is to
		// prevent compiler warnings
		ds.close();
	}
}
