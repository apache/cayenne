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
package org.apache.cayenne.merge;

import static org.apache.cayenne.access.loader.filters.FilterFactory.*;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.loader.DbLoaderConfiguration;
import org.apache.cayenne.access.loader.filters.DbPath;
import org.apache.cayenne.access.loader.filters.EntityFilters;
import org.apache.cayenne.access.loader.filters.FiltersConfig;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public abstract class MergeCase extends ServerCase {

	private Log logger = LogFactory.getLog(MergeCase.class);

	@Inject
	private DBHelper dbHelper;

	@Inject
	private ServerRuntime runtime;

	@Inject
	private UnitDbAdapter accessStackAdapter;

	@Inject
	private ServerCaseDataSourceFactory dataSourceFactory;

	@Inject
	protected EntityResolver resolver;

	@Inject
	protected DataNode node;

	protected DataMap map;

	@Override
	public void cleanUpDB() throws Exception {
		dbHelper.update("ARTGROUP").set("PARENT_GROUP_ID", null, Types.INTEGER).execute();
		super.cleanUpDB();
	}

	@Before
	public void setUp() throws Exception {

		// this map can't be safely modified in this test, as it is reset by DI
		// container
		// on every test
		map = runtime.getDataDomain().getDataMap("testmap");

		filterDataMap();

		List<MergerToken> tokens = createMergeTokens();
		execute(tokens);

		assertTokensAndExecute(0, 0);
	}

	protected DbMerger createMerger(MergerFactory mergerFactory) {
		return createMerger(mergerFactory, null);
	}

	protected DbMerger createMerger(MergerFactory mergerFactory, ValueForNullProvider valueForNullProvider) {
		return new DbMerger(mergerFactory, valueForNullProvider);
	}

    protected List<MergerToken> createMergeTokens() {
        DbLoaderConfiguration loaderConfiguration = new DbLoaderConfiguration();
        loaderConfiguration.setFiltersConfig(new FiltersConfig(new EntityFilters(DbPath.EMPTY, include("ARTIST|GALLERY|PAINTING|NEW_TABLE2?"), TRUE, TRUE)));

        return createMerger(node.getAdapter().mergerFactory()).createMergeTokens(node, map, loaderConfiguration);
    }

	/**
	 * Remote binary pk {@link DbEntity} for {@link DbAdapter} not supporting
	 * that and so on.
	 */
	private void filterDataMap() {
		// copied from AbstractAccessStack.dbEntitiesInInsertOrder
		boolean excludeBinPK = accessStackAdapter.supportsBinaryPK();

		if (!excludeBinPK) {
			return;
		}

		List<DbEntity> entitiesToRemove = new ArrayList<DbEntity>();

		for (DbEntity ent : map.getDbEntities()) {
			for (DbAttribute attr : ent.getAttributes()) {
				// check for BIN PK or FK to BIN Pk
				if (attr.getType() == Types.BINARY || attr.getType() == Types.VARBINARY
						|| attr.getType() == Types.LONGVARBINARY) {

					if (attr.isPrimaryKey() || attr.isForeignKey()) {
						entitiesToRemove.add(ent);
						break;
					}
				}
			}
		}

		for (DbEntity e : entitiesToRemove) {
			map.removeDbEntity(e.getName(), true);
		}
	}

	protected void execute(List<MergerToken> tokens) {
		MergerContext mergerContext = new ExecutingMergerContext(map, node);
		for (MergerToken tok : tokens) {
			tok.execute(mergerContext);
		}
	}

	protected void execute(MergerToken token) throws Exception {
		MergerContext mergerContext = new ExecutingMergerContext(map, node);
		token.execute(mergerContext);
	}

	private void executeSql(String sql) throws Exception {
		Connection conn = dataSourceFactory.getSharedDataSource().getConnection();

		try {
			Statement st = conn.createStatement();

			try {
				st.execute(sql);
			} finally {
				st.close();
			}
		}

		finally {
			conn.close();
		}
	}

	protected void assertTokens(List<MergerToken> tokens, int expectedToDb, int expectedToModel) {
		int actualToDb = 0;
		int actualToModel = 0;
		for (MergerToken token : tokens) {
			if (token.getDirection().isToDb()) {
				actualToDb++;
			} else if (token.getDirection().isToModel()) {
				actualToModel++;
			}
		}

		assertEquals("tokens to db", expectedToDb, actualToDb);
		assertEquals("tokens to model", expectedToModel, actualToModel);
	}

	protected void assertTokensAndExecute(int expectedToDb, int expectedToModel) {
		List<MergerToken> tokens = createMergeTokens();
		assertTokens(tokens, expectedToDb, expectedToModel);
		execute(tokens);
	}

	protected MergerFactory mergerFactory() {
		return node.getAdapter().mergerFactory();
	}

	protected void dropTableIfPresent(String tableName) throws Exception {

		// must have a dummy datamap for the dummy table for the downstream code
		// to work
		DataMap map = new DataMap("dummy");
		map.setQuotingSQLIdentifiers(map.isQuotingSQLIdentifiers());
		DbEntity entity = new DbEntity(tableName);
		map.addDbEntity(entity);

		AbstractToDbToken t = (AbstractToDbToken) mergerFactory().createDropTableToDb(entity);

		for (String sql : t.createSql(node.getAdapter())) {

			try {
				executeSql(sql);
			} catch (Exception e) {
				logger.info("Exception dropping table " + tableName + ", probably abscent..");
			}
		}
	}
}
