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

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * Turns unpooled DataSource to a connection pool. Normally you won't be
 * creating this builder explicitly. Call
 * {@link DataSourceBuilder#pool(int, int)} method instead.
 * 
 * @since 4.0
 */
public class PoolingDataSourceBuilder {

	private DataSourceBuilder nonPoolingBuilder;
	private PoolingDataSourceParameters poolParameters;

	public PoolingDataSourceBuilder(DataSourceBuilder nonPoolingBuilder) {
		this.nonPoolingBuilder = nonPoolingBuilder;
		this.poolParameters = new PoolingDataSourceParameters();

		poolParameters.setMinConnections(1);
		poolParameters.setMaxConnections(1);
		poolParameters.setMaxQueueWaitTime(UnmanagedPoolingDataSource.MAX_QUEUE_WAIT_DEFAULT);
	}

	public PoolingDataSourceBuilder minConnections(int minConnections) {
		poolParameters.setMinConnections(minConnections);
		return this;
	}

	public PoolingDataSourceBuilder maxConnections(int maxConnections) {
		poolParameters.setMaxConnections(maxConnections);
		return this;
	}

	public PoolingDataSourceBuilder maxQueueWaitTime(long maxQueueWaitTime) {
		poolParameters.setMaxQueueWaitTime(maxQueueWaitTime);
		return this;
	}

	public PoolingDataSourceBuilder validationQuery(String validationQuery) {
		poolParameters.setValidationQuery(validationQuery);
		return this;
	}

	/**
	 * Builds a pooling DataSource that needs to be explicitly closed by the
	 * caller when no longer in use.
	 */
	public PoolingDataSource build() {

		// sanity checks...
		if (poolParameters.getMaxConnections() < 0) {
			throw new CayenneRuntimeException("Maximum number of connections can not be negative (%d)."
					, poolParameters.getMaxConnections());
		}

		if (poolParameters.getMinConnections() < 0) {
			throw new CayenneRuntimeException("Minimum number of connections can not be negative (%d)"
					, poolParameters.getMinConnections());
		}

		if (poolParameters.getMinConnections() > poolParameters.getMaxConnections()) {
			throw new CayenneRuntimeException("Minimum number of connections can not be bigger then maximum.");
		}

		DataSource nonPooling = nonPoolingBuilder.build();
		return buildManaged(buildPooling(nonPooling));
	}

	private UnmanagedPoolingDataSource buildPooling(DataSource nonPoolingDataSource) {
		return new UnmanagedPoolingDataSource(nonPoolingDataSource, poolParameters);
	}

	private PoolingDataSource buildManaged(UnmanagedPoolingDataSource dataSource) {
		return new ManagedPoolingDataSource(dataSource);
	}

}
