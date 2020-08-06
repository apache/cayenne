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

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @since 3.0
 */
class SchemaAnalyzer {

	private Map<String, String> mapTableInDB;
	private List<String> tableNoInDB;
	private Map<String, Collection<String>> nameSchemaMap;
	private Map<String, Collection<String>> schemaNameMap;
	private Map<Map<String, String>, Collection<DbAttribute>> entityTables;
	private String errorMessage;

	SchemaAnalyzer() {
		errorMessage = null;
		mapTableInDB = new HashMap<>();
		tableNoInDB = new ArrayList<>();
		nameSchemaMap = new HashMap<>();
		schemaNameMap = new HashMap<>();
		entityTables = new HashMap<>();
	}

	public List<String> getTableNoInDB() {
		return tableNoInDB;
	}

	public void compareColumns(DatabaseMetaData md) throws SQLException {

		for (Map.Entry<String, String> map : mapTableInDB.entrySet()) {

			String schema = map.getValue();
			String name = map.getKey();

			try (ResultSet rs = md.getColumns(null, schema, name, null);) {
				Map<String, String> schemaName = new HashMap<>();
				schemaName.put(name, schema);
				Collection<DbAttribute> atribute = entityTables.get(schemaName);
				if (atribute == null) {
					schemaName.remove(name);
					schemaName.put(name, null);
					atribute = entityTables.get(schemaName);
				}
				if (atribute != null && rs.getFetchSize() != 0) {
					int countColumn = 0;
					int isInEntity = 0;
					while (rs.next()) {
						countColumn++;
						String columnName = rs.getString("COLUMN_NAME");
						for (DbAttribute attr : atribute) {

							if (attr.getName().equalsIgnoreCase(columnName)) {
								isInEntity++;
								continue;
							}
						}
					}

					if (countColumn != atribute.size()) {
						errorMessage = "different number of columns in table " + name;
						continue;
					}
					if (countColumn != isInEntity && errorMessage == null) {
						errorMessage = "no columns in table " + name + " or does not match the type of column";
						continue;
					}
				}
			}
		}

	}

	public boolean compareTables(DatabaseMetaData md, Collection<DbEntity> entities) {

		boolean isIncluded = true;
		for (DbEntity ent : entities) {

			String name = ent.getName();
			String schema = ent.getSchema();
			Collection<DbAttribute> atributes = ent.getAttributes();

			if (schema != null) {
				if (schemaNameMap.get(schema) != null) {
					Collection<String> names = schemaNameMap.get(schema);
					if (names.contains(name)) {
						mapTableInDB.put(name, schema);
					} else {
						tableNoInDB.add(name);
					}
				} else {
					isIncluded = false;
					errorMessage = "no schema " + schema + " in db";
					break;
				}
			} else {
				if (nameSchemaMap.get(name) != null
						|| !ent.getDataMap().isQuotingSQLIdentifiers()
						&& (nameSchemaMap.get(name.toLowerCase()) != null || nameSchemaMap.get(name.toUpperCase()) != null)) {
					Collection<String> sc = nameSchemaMap.get(name);
					if (sc == null) {
						if (nameSchemaMap.get(name.toLowerCase()) != null) {
							sc = nameSchemaMap.get(name.toLowerCase());
						} else {
							sc = nameSchemaMap.get(name.toUpperCase());
						}
					}

					if (sc.size() == 1) {
						mapTableInDB.put(name, sc.iterator().next());
					} else {
						errorMessage = " enter the schema. Table found in the schemas: ";
						Iterator<String> it = sc.iterator();
						String names = "";
						while (it.hasNext()) {
							names += it.next() + ", ";
						}
						errorMessage = errorMessage + names;
					}
				} else {
					tableNoInDB.add(name);
				}
			}
			Map<String, String> schemaName = new HashMap<>();
			schemaName.put(name, schema);
			entityTables.put(schemaName, atributes);
		}
		return isIncluded;
	}

	public void analyzeSchemas(List<String> schemas, DatabaseMetaData md) throws SQLException {

		if (schemas.size() == 0) {
			schemas.add("%");
		}
		for (String schema : schemas) {

			Collection<String> tableInSchema = new ArrayList<>();
			try (ResultSet tables = md.getTables(null, schema, null, null);) {
				while (tables.next()) {
					String name = tables.getString("TABLE_NAME");
					if (name == null || name.startsWith("BIN$")) {
						continue;
					}

					tableInSchema.add(name);
					if (nameSchemaMap.get(name) != null) {
						Collection<String> sc = nameSchemaMap.get(name);
						Iterator<String> iSc = sc.iterator();
						boolean inSchema = false;
						while (iSc.hasNext()) {
							if (iSc.next().equals(schema)) {
								inSchema = true;
							}
						}
						if (!inSchema) {
							sc.add(schema);
							nameSchemaMap.remove(name);
							nameSchemaMap.put(name, sc);
						}

					} else {
						Collection<String> sc = new ArrayList<>();
						sc.add(schema);
						nameSchemaMap.put(name, sc);
					}
				}
				schemaNameMap.put(schema, tableInSchema);
			}
		}
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
