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
package org.apache.cayenne.dbsync.reverse.db;

import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DetectedDbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @since 4.0
 */
public class DbTableLoader {

	private static final Log LOGGER = LogFactory.getLog(DbTableLoader.class);

	private static final String WILDCARD = "%";

	private final String catalog;
	private final String schema;

	private final DatabaseMetaData metaData;
	private final DbLoaderDelegate delegate;

	private final DbAttributesLoader attributesLoader;

	public DbTableLoader(String catalog, String schema, DatabaseMetaData metaData, DbLoaderDelegate delegate,
			DbAttributesLoader attributesLoader) {
		this.catalog = catalog;
		this.schema = schema;
		this.metaData = metaData;
		this.delegate = delegate;

		this.attributesLoader = attributesLoader;
	}

	/**
	 * Returns all tables for given combination of the criteria. Tables returned
	 * as DbEntities without any attributes or relationships.
	 *
	 * @param types
	 *            The types of table names to retrieve, null returns all types.
	 * @since 4.0
	 */
	public List<DetectedDbEntity> getDbEntities(TableFilter filters, String[] types) throws SQLException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Read tables: catalog=" + catalog + ", schema=" + schema + ", types=" + Arrays.toString(types));
		}

		List<DetectedDbEntity> tables = new LinkedList<DetectedDbEntity>();
		try (ResultSet rs = metaData.getTables(catalog, schema, WILDCARD, types);) {
			while (rs.next()) {
				// Oracle 9i and newer has a nifty recycle bin feature... but we
				// don't
				// want dropped tables to be included here; in fact they may
				// even result
				// in errors on reverse engineering as their names have special
				// chars like
				// "/", etc. So skip them all together

				String name = rs.getString("TABLE_NAME");
				if (name == null) {
					continue;
				}

				DetectedDbEntity table = new DetectedDbEntity(name);

				String catalog = rs.getString("TABLE_CAT");
				table.setCatalog(catalog);

				String schema = rs.getString("TABLE_SCHEM");
				table.setSchema(schema);
				if (!(this.catalog == null || this.catalog.equals(catalog))
						|| !(this.schema == null || this.schema.equals(schema))) {

					LOGGER.error(catalog + "." + schema + "." + name + " wrongly loaded for catalog/schema : "
							+ this.catalog + "." + this.schema);

					continue;
				}

				PatternFilter includeTable = filters.isIncludeTable(table.getName());
				if (includeTable != null) {
					tables.add(table);
				}
			}
		}
		return tables;
	}

	/**
	 * Loads dbEntities for the specified tables.
	 * 
	 * @param config
	 * @param types
	 */
	public List<DbEntity> loadDbEntities(DataMap map, DbLoaderConfiguration config, String[] types) throws SQLException {
		/** List of db entities to process. */

		List<DetectedDbEntity> tables = getDbEntities(config.getFiltersConfig().tableFilter(catalog, schema), types);

		List<DbEntity> dbEntities = new ArrayList<>();
		for (DbEntity dbEntity : tables) {
			DbEntity oldEnt = map.getDbEntity(dbEntity.getName());
			if (oldEnt != null) {
				Collection<ObjEntity> oldObjEnt = map.getMappedEntities(oldEnt);
				if (!oldObjEnt.isEmpty()) {
					for (ObjEntity objEntity : oldObjEnt) {
						LOGGER.debug("Delete ObjEntity: " + objEntity.getName());
						map.removeObjEntity(objEntity.getName(), true);
						delegate.objEntityRemoved(objEntity);
					}
				}

				LOGGER.debug("Overwrite DbEntity: " + oldEnt.getName());
				map.removeDbEntity(oldEnt.getName(), true);
				delegate.dbEntityRemoved(oldEnt);
			}

			map.addDbEntity(dbEntity);

			delegate.dbEntityAdded(dbEntity);

			// delegate might have thrown this entity out... so check if it is
			// still
			// around before continuing processing
			if (map.getDbEntity(dbEntity.getName()) == dbEntity) {
				dbEntities.add(dbEntity);
				attributesLoader.loadDbAttributes(dbEntity);
				if (!config.isSkipPrimaryKeyLoading()) {
					loadPrimaryKey(dbEntity);
				}
			}
		}

		return dbEntities;
	}

	private void loadPrimaryKey(DbEntity dbEntity) throws SQLException {

		try (ResultSet rs = metaData.getPrimaryKeys(dbEntity.getCatalog(), dbEntity.getSchema(), dbEntity.getName());) {
			while (rs.next()) {
				String columnName = rs.getString("COLUMN_NAME");
				DbAttribute attribute = dbEntity.getAttribute(columnName);

				if (attribute != null) {
					attribute.setPrimaryKey(true);
				} else {
					// why an attribute might be null is not quiet clear
					// but there is a bug report 731406 indicating that it is
					// possible
					// so just print the warning, and ignore
					LOGGER.warn("Can't locate attribute for primary key: " + columnName);
				}

				String pkName = rs.getString("PK_NAME");
				if (pkName != null && dbEntity instanceof DetectedDbEntity) {
					((DetectedDbEntity) dbEntity).setPrimaryKeyName(pkName);
				}

			}
		}
	}
}
