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

package org.apache.cayenne.dbsync.reverse.configuration;

import java.sql.Driver;

import javax.sql.DataSource;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.configuration.runtime.DataSourceFactory;
import org.apache.cayenne.datasource.DriverDataSource;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;

/**
 * @since 4.0
 */
public class DriverDataSourceFactory implements DataSourceFactory {

	private AdhocObjectFactory objectFactory;

	public DriverDataSourceFactory(@Inject AdhocObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}

	public DataSource getDataSource(DataNodeDescriptor nodeDescriptor) throws Exception {
		DataSourceDescriptor dataSourceDescriptor = nodeDescriptor.getDataSourceDescriptor();
		if (dataSourceDescriptor == null) {
			throw new IllegalArgumentException("'nodeDescriptor' contains no datasource descriptor");
		}

		Driver driver = objectFactory.<Driver>getJavaClass(dataSourceDescriptor.getJdbcDriver()).getDeclaredConstructor().newInstance();
		return new DriverDataSource(driver, dataSourceDescriptor.getDataSourceUrl(), dataSourceDescriptor.getUserName(),
				dataSourceDescriptor.getPassword());
	}
}
