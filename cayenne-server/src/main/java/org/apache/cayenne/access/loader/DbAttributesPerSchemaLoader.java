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
package org.apache.cayenne.access.loader;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.access.loader.filters.PatternFilter;
import org.apache.cayenne.access.loader.filters.TableFilter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;

/**
 * Load all attributes for schema and return it for each table
 * */
public class DbAttributesPerSchemaLoader extends DbAttributesBaseLoader {

	private final TableFilter filter;

	private Map<String, List<DbAttribute>> attributes;

	public DbAttributesPerSchemaLoader(String catalog, String schema, DatabaseMetaData metaData, DbAdapter adapter,
			TableFilter filter) {
		super(catalog, schema, metaData, adapter);

		this.filter = filter;
	}

	private Map<String, List<DbAttribute>> loadDbAttributes() throws SQLException {
		Map<String, List<DbAttribute>> attributes = new HashMap<>();

		try (ResultSet rs = getMetaData().getColumns(getCatalog(), getSchema(), "%", "%");) {
			Set<String> columns = new HashSet<String>();

			while (rs.next()) {
				if (columns.isEmpty()) {
					ResultSetMetaData rsMetaData = rs.getMetaData();
					for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
						columns.add(rsMetaData.getColumnLabel(i));
					}
				}

				// for a reason not quiet apparent to me, Oracle sometimes
				// returns duplicate record sets for the same table, messing up
				// table
				// names. E.g. for the system table "WK$_ATTR_MAPPING" columns
				// are
				// returned twice - as "WK$_ATTR_MAPPING" and
				// "WK$$_ATTR_MAPPING"... Go figure
				String tableName = rs.getString("TABLE_NAME");
				String columnName = rs.getString("COLUMN_NAME");

				PatternFilter columnFilter = filter.isIncludeTable(tableName);
				/*
				 * Here is possible optimization if filter will contain
				 * map<tableName, columnFilter> we can replace it after tables
				 * loading since already done pattern matching once and exactly
				 * know all tables that we want to process
				 */
				if (columnFilter == null || !columnFilter.isInclude(columnName)) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Skip column '" + tableName + "." + columnName + "' (Path: " + getCatalog() + "/"
								+ getSchema() + "; Filter: " + columnFilter + ")");
					}
					continue;
				}

				List<DbAttribute> attrs = attributes.get(tableName);
				if (attrs == null) {
					attrs = new LinkedList<DbAttribute>();

					attributes.put(tableName, attrs);
				}

				attrs.add(loadDbAttribute(columns, rs));
			}
		}

		return attributes;
	}

	@Override
	protected List<DbAttribute> loadDbAttributes(String tableName) {
		Map<String, List<DbAttribute>> attributes = getAttributes();
		if (attributes != null) {
			List<DbAttribute> dbAttributes = attributes.get(tableName);
			if (dbAttributes != null) {
				return dbAttributes;
			}
		}

		return new LinkedList<DbAttribute>();
	}

	public Map<String, List<DbAttribute>> getAttributes() {
		if (attributes == null) {
			try {
				attributes = loadDbAttributes();
			} catch (SQLException e) {
				LOGGER.error(e);
				attributes = new HashMap<>();
			}
		}
		return attributes;
	}
}
