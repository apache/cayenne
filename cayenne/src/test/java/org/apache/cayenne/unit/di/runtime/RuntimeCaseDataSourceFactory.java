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
package org.apache.cayenne.unit.di.runtime;

import java.sql.Driver;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.datasource.DataSourceBuilder;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;

public class RuntimeCaseDataSourceFactory {

	private DataSource sharedDataSource;
	private DataSourceDescriptor dataSourceInfo;
	private Map<String, DataSource> dataSources;
	private Set<String> mapsWithDedicatedDataSource;
	private AdhocObjectFactory objectFactory;

	public RuntimeCaseDataSourceFactory(@Inject DataSourceDescriptor dataSourceInfo, @Inject AdhocObjectFactory objectFactory) {

		this.objectFactory = objectFactory;
		this.dataSourceInfo = dataSourceInfo;
		this.dataSources = new HashMap<>();
		this.mapsWithDedicatedDataSource = new HashSet<>(Arrays.asList("map-db1", "map-db2"));

		this.sharedDataSource = createDataSource();
	}

	public DataSource getSharedDataSource() {
		return sharedDataSource;
	}

	public DataSource getDataSource(String dataMapName) {
		DataSource ds = dataSources.get(dataMapName);
		if (ds == null) {

			ds = mapsWithDedicatedDataSource.contains(dataMapName) ? createDataSource() : sharedDataSource;

			dataSources.put(dataMapName, ds);
		}

		return ds;
	}

	private DataSource createDataSource() {
		Driver driver = objectFactory.newInstance(Driver.class, dataSourceInfo.getJdbcDriver());

		return DataSourceBuilder.url(dataSourceInfo.getDataSourceUrl()).driver(driver)
				.userName(dataSourceInfo.getUserName()).password(dataSourceInfo.getPassword())
				.pool(dataSourceInfo.getMinConnections(), dataSourceInfo.getMaxConnections()).build();
	}

}
