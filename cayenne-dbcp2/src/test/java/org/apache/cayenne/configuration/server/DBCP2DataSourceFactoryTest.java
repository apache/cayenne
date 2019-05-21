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

package org.apache.cayenne.configuration.server;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.resource.URLResource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DBCP2DataSourceFactoryTest {

	@Test
	public void testGetDataSource() throws Exception {

		String baseUrl = getClass().getPackage().getName().replace('.', '/');
		URL url = getClass().getClassLoader().getResource(baseUrl + "/");
		assertNotNull(url);

		DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
		nodeDescriptor.setConfigurationSource(new URLResource(url));
		nodeDescriptor.setParameters("testDBCP2.properties");

		DBCPDataSourceFactory factory = new DBCPDataSourceFactory();
		DataSource dataSource = factory.getDataSource(nodeDescriptor);
		assertNotNull(dataSource);

		assertTrue(dataSource instanceof BasicDataSource);
		try (BasicDataSource basicDataSource = (BasicDataSource) dataSource;) {
			assertEquals("com.example.jdbc.Driver", basicDataSource.getDriverClassName());
			assertEquals("jdbc:somedb://localhost/cayenne", basicDataSource.getUrl());
			assertEquals("john", basicDataSource.getUsername());
			assertEquals("secret", basicDataSource.getPassword());
			assertEquals(20, basicDataSource.getMaxTotal());
			assertEquals(5, basicDataSource.getMinIdle());
			assertEquals(8, basicDataSource.getMaxIdle());
			assertEquals(10000, basicDataSource.getMaxWaitMillis());
			assertEquals("select 1 from xyz;", basicDataSource.getValidationQuery());
		}
	}

	@Test
	public void testGetDataSource_InvalidLocation() throws Exception {

		String baseUrl = getClass().getPackage().getName().replace('.', '/');
		URL url = getClass().getClassLoader().getResource(baseUrl + "/");
		assertNotNull(url);

		DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
		nodeDescriptor.setConfigurationSource(new URLResource(url));
		nodeDescriptor.setParameters("testDBCP2.properties.nosuchfile");

		DBCPDataSourceFactory factory = new DBCPDataSourceFactory();

		try {
			factory.getDataSource(nodeDescriptor);
			fail("didn't throw on absent config file");
		} catch (IOException ex) {
			// expected
		}
	}

}
