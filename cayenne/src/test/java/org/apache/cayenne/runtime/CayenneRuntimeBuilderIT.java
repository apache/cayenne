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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import javax.sql.DataSource;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.SQLSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@SuppressWarnings("deprecation")
public class CayenneRuntimeBuilderIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

	private DBHelper dbHelper;
	private CayenneRuntime runtime;
	private DataSourceDescriptor dsi;

	private CayenneRuntime localRuntime;
	private DataSource dataSource;

	@AfterEach
	public void stopLocalRuntime() {

		// even though we don't supply real configs here, we sometimes access
		// DataDomain, and this starts EventManager threads that need to be
		// shutdown
		if (localRuntime != null) {
			localRuntime.shutdown();
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
		dbHelper = env.dbHelper();
		runtime = env.runtime();
		dsi = env.getInstance(DataSourceDescriptor.class);
		TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
		tArtist.insert(33001, "AA1");
		tArtist.insert(33002, "AA2");

		this.dataSource = runtime.getDataSource("testmap");
	}

	@Test
	public void configFree_WithDBParams() {

		localRuntime = new CayenneRuntimeBuilder(null).jdbcDriver(dsi.getJdbcDriver()).url(dsi.getDataSourceUrl())
				.password(dsi.getPassword()).user(dsi.getUserName()).minConnections(1).maxConnections(2).build();

		List<DataRow> result = SQLSelect.dataRowQuery("SELECT * FROM ARTIST").select(localRuntime.newContext());
		assertEquals(2, result.size());
	}

	@Test
	public void configFree_WithDBParams_WithProject() {

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
	public void configFree_WithDataSource() {

		localRuntime = new CayenneRuntimeBuilder(null).dataSource(dataSource).build();

		List<DataRow> result = SQLSelect.dataRowQuery("SELECT * FROM ARTIST").select(localRuntime.newContext());
		assertEquals(2, result.size());
	}

	@Test
	public void noNodeConfig_WithDataSource() {

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
	public void unnamedDomain_MultiLocation() {
		localRuntime = new CayenneRuntimeBuilder(null).addConfigs(CayenneProjects.TESTMAP_PROJECT,
				CayenneProjects.EMBEDDABLE_PROJECT).build();

		assertEquals("cayenne", localRuntime.getDataDomain().getName());
	}

	@Test
	public void namedDomain_MultiLocation() {
		localRuntime = new CayenneRuntimeBuilder("myd").addConfigs(CayenneProjects.TESTMAP_PROJECT,
				CayenneProjects.EMBEDDABLE_PROJECT).build();
		assertEquals("myd", localRuntime.getDataDomain().getName());
	}

	/**
	 * Test case for CAY-2265
	 */
	@Test
	public void unnamedDomain_CustomNameProjectFile() {
		localRuntime = new CayenneRuntimeBuilder(null).addConfigs(CayenneProjects.CUSTOM_NAME_PROJECT).build();
		assertEquals("cayenne", localRuntime.getDataDomain().getName());

		ObjectContext context = localRuntime.newContext();
		assertNotNull(context);
	}
}
