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
package org.apache.cayenne.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.List;

import javax.sql.DataSource;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.SQLSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("deprecation")
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CayenneRuntimeBuilderIT extends RuntimeCase {

	@Inject
	private DBHelper dbHelper;

	@Inject
	private CayenneRuntime runtime;

	@Inject
	private DataSourceDescriptor dsi;

	private CayenneRuntime localRuntime;
	private DataSource dataSource;

	@After
	public void stopLocalRuntime() {

		// even though we don't supply real configs here, we sometimes access
		// DataDomain, and this starts EventManager threads that need to be
		// shutdown
		if (localRuntime != null) {
			localRuntime.shutdown();
		}
	}

	@Before
	public void setUp() throws Exception {
		TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
		tArtist.insert(33001, "AA1");
		tArtist.insert(33002, "AA2");

		this.dataSource = runtime.getDataSource("testmap");
	}

	@Test
	public void testConfigFree_WithDBParams() {

		localRuntime = new CayenneRuntimeBuilder(null).jdbcDriver(dsi.getJdbcDriver()).url(dsi.getDataSourceUrl())
				.password(dsi.getPassword()).user(dsi.getUserName()).minConnections(1).maxConnections(2).build();

		List<DataRow> result = SQLSelect.dataRowQuery("SELECT * FROM ARTIST").select(localRuntime.newContext());
		assertEquals(2, result.size());
	}

	@Test
	public void tesConfigFree_WithDBParams() {

		localRuntime = new CayenneRuntimeBuilder(null).addConfig(CayenneProjects.TESTMAP_PROJECT)
				.jdbcDriver(dsi.getJdbcDriver()).url(dsi.getDataSourceUrl()).password(dsi.getPassword())
				.user(dsi.getUserName()).minConnections(1).maxConnections(2).build();

		DataMap map = localRuntime.getDataDomain().getDataMap("testmap");
		assertNotNull(map);

		DataNode node = localRuntime.getDataDomain().getDefaultNode();
		assertNotNull(node);
		assertEquals(1, node.getDataMaps().size());

		assertSame(map, node.getDataMap("testmap"));
	}

	@Test
	public void testConfigFree_WithDataSource() {

		localRuntime = new CayenneRuntimeBuilder(null).dataSource(dataSource).build();

		List<DataRow> result = SQLSelect.dataRowQuery("SELECT * FROM ARTIST").select(localRuntime.newContext());
		assertEquals(2, result.size());
	}

	@Test
	public void testNoNodeConfig_WithDataSource() {

		localRuntime = new CayenneRuntimeBuilder(null).addConfig(CayenneProjects.TESTMAP_PROJECT).dataSource(dataSource)
				.build();

		DataMap map = localRuntime.getDataDomain().getDataMap("testmap");
		assertNotNull(map);

		DataNode node = localRuntime.getDataDomain().getDefaultNode();
		assertNotNull(node);
		assertEquals(1, node.getDataMaps().size());

		assertSame(map, node.getDataMap("testmap"));
	}

	@Test
	public void test_UnnamedDomain_MultiLocation() {
		localRuntime = new CayenneRuntimeBuilder(null).addConfigs(CayenneProjects.TESTMAP_PROJECT,
				CayenneProjects.EMBEDDABLE_PROJECT).build();

		assertEquals("cayenne", localRuntime.getDataDomain().getName());
	}

	@Test
	public void test_NamedDomain_MultiLocation() {
		localRuntime = new CayenneRuntimeBuilder("myd").addConfigs(CayenneProjects.TESTMAP_PROJECT,
				CayenneProjects.EMBEDDABLE_PROJECT).build();
		assertEquals("myd", localRuntime.getDataDomain().getName());
	}

	/**
	 * Test case for CAY-2265
	 */
	@Test
	public void test_UnnamedDomain_CustomNameProjectFile() {
		localRuntime = new CayenneRuntimeBuilder(null).addConfigs(CayenneProjects.CUSTOM_NAME_PROJECT).build();
		assertEquals("cayenne", localRuntime.getDataDomain().getName());

		ObjectContext context = localRuntime.newContext();
		assertNotNull(context);
	}
}
