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
package org.apache.cayenne.access.dbsync;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SchemaUpdateStrategyBase extends RuntimeCase {

	@Inject
	protected ObjectContext context;

	@Inject
	protected DataNode node;

	@Inject
	protected DbAdapter adapter;

	@Override
	public void cleanUpDB() {
		DataMap map = node.getEntityResolver().getDataMap("sus-map");
		for (String name : existingTables()) {

			for (String drop : adapter.dropTableStatements(map.getDbEntity(name))) {
				context.performGenericQuery(new SQLTemplate(Object.class, drop));
			}
		}
	}

	protected void setStrategy(Class<? extends SchemaUpdateStrategy> type) throws Exception {
		node.setSchemaUpdateStrategy(type.getDeclaredConstructor().newInstance());
	}

	protected Collection<String> existingTables() {
		Collection<String> present = new ArrayList<>();
		for (Entry<String, Boolean> e : tablesMap().entrySet()) {
			if (e.getValue()) {
				present.add(e.getKey());
			}
		}

		return present;
	}

	protected void createOneTable(String entityName) {
		DataMap map = node.getEntityResolver().getDataMap("sus-map");
		String createTable = adapter.createTable(map.getDbEntity(entityName));
		context.performGenericQuery(new SQLTemplate(Object.class, createTable));
	}

	protected Map<String, Boolean> tablesMap() {
		DataMap map = node.getEntityResolver().getDataMap("sus-map");
		Map<String, String> tables = new HashMap<>();

		// add upper/lower case permutations
		for (String name : map.getDbEntityMap().keySet()) {
			tables.put(name.toLowerCase(), name);
			tables.put(name.toUpperCase(), name);
		}

		Map<String, Boolean> presentInDB = new HashMap<>();
		for (String name : map.getDbEntityMap().keySet()) {
			presentInDB.put(name, false);
		}

		String tableLabel = node.getAdapter().tableTypeForTable();
		try (Connection con = node.getDataSource().getConnection()) {

			try (ResultSet rs = con.getMetaData().getTables(null, null, "%", new String[] { tableLabel })) {
				while (rs.next()) {
					String dbName = rs.getString("TABLE_NAME");

					String name = tables.get(dbName);

					if (name != null) {
						presentInDB.put(name, true);
					}
				}
			}
		} catch (SQLException e) {
			throw new CayenneRuntimeException(e);
		}

		return presentInDB;
	}

}
