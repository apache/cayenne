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

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.compiled.CompiledFlattenedTable;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataRows-to-objects converter for a specific ObjEntity.
 * 
 * @since 1.2
 */
class ObjectResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectResolver.class);

	DataContext context;
	CompiledEntity entity;
	Collection<CompiledColumn> primaryKey;

	boolean refreshObjects;
	DataRowStore cache;
	EntityResolutionStrategy entityResolutionStrategy;

	// the plan of the rows being converted, for the root entity, and per subentity when there is
	// inheritance. A plan follows the columns of the rows it is applied to, so it is built once per resolver
	private MergePlan plan;
	private Map<CompiledEntity, MergePlan> subclassPlans;

	ObjectResolver(DataContext context, CompiledEntity entity, boolean refresh) {

		// sanity check
		if (entity == null) {
			// possible cause: query that is not expected to have result set somehow got it..
			throw new CayenneRuntimeException("No ObjEntity. Maybe DataRows should be fetched instead of objects.");
		}

		CompiledTable table = entity.table();
		if (table == null) {
			throw new CayenneRuntimeException("ObjEntity '%s' has no DbEntity.", entity.name());
		}

		this.primaryKey = table.primaryKey();
		if (primaryKey.isEmpty()) {
			throw new CayenneRuntimeException("Won't be able to create ObjectId for '%s'. Reason: DbEntity " +
					"'%s' has no Primary Key defined.", entity.name(), table.name());
		}

		this.context = context;
		this.cache = context.getObjectStore().getDataRowCache();
		this.refreshObjects = refresh;
		this.entity = entity;

		this.entityResolutionStrategy = entity.hasSubEntities() ? new InheritanceStrategy()
				: new NoInheritanceStrategy();
	}

	PrefetchProcessorNode rootResultNodeFromDataRows(List<? extends DataRow> rows) {
		PrefetchProcessorNode rootNode = new PrefetchProcessorNode(null, null);
		rootNode.setObjects(objectsFromDataRows(rows));
		return rootNode;
	}

	/**
	 * Converts rows to objects. Each object is merged under its own lock, so concurrent resolvers can run in
	 * parallel, and a row never overwrites an object that already reflects a newer snapshot.
	 */
	List<Persistent> objectsFromDataRows(List<? extends DataRow> rows) {
		if (rows == null || rows.isEmpty()) {
			return new ArrayList<>(1);
		}

		List<Persistent> results = new ArrayList<>(rows.size());
		for (DataRow row : rows) {
			// nulls are possible here, e.g. for an outer join in a SQLTemplate that resolves to no object
			results.add(objectFromDataRow(row));
		}

		// now deal with snapshots
		cache.snapshotsUpdatedForObjects(results, rows, refreshObjects);
		return results;
	}

	Persistent objectFromDataRow(DataRow row) {
		// determine entity to use
		CompiledEntity rowEntity = entityResolutionStrategy.entityForRow(row);
		MergePlan plan = planFor(rowEntity);
		ObjectId anId = createObjectId(row, rowEntity, plan);
		return objectFromDataRow(row, anId, rowEntity, plan);
	}

	/**
	 * Returns the plan of the columns of the given entity, built on the first call for the entity.
	 */
	MergePlan planFor(CompiledEntity rowEntity) {
		if (rowEntity == entity) {
			if (plan == null) {
				plan = MergePlan.of(rowEntity);
			}
			return plan;
		}

		if (subclassPlans == null) {
			subclassPlans = new HashMap<>();
		}
		return subclassPlans.computeIfAbsent(rowEntity, MergePlan::of);
	}

	/**
	 * Returns the id of the row's object, or null if the row has a null PK, which is possible when processing left
	 * outer joint prefetches.
	 *
	 * @param plan the plan of the entity, as returned by {@link #planFor(CompiledEntity)}
	 */
	ObjectId createObjectId(DataRow row, CompiledEntity rowEntity, MergePlan plan) {
		String name = rowEntity.name();
		String[] pkNames = plan.pkNames;
		int[] pkColumns = plan.pkColumns;
		DataRowLayout.Reader reader = plan.reader;

		// PK i is column i of the plan

		if (pkNames.length == 1) {
			Object val = reader.get(row, pkColumns[0]);
			if (val == null) {
				if (!reader.has(row, pkColumns[0])) {
					throw new CayenneRuntimeException("No PK column '%s' found in data row.", pkNames[0]);
				}
				return null;
			}
			return ObjectId.of(name, pkNames[0], val);
		}

		Map<String, Object> idMap = HashMap.newHashMap(pkNames.length);
		for (int i = 0; i < pkNames.length; i++) {
			Object val = reader.get(row, pkColumns[i]);
			if (val == null) {
				if (!reader.has(row, pkColumns[i])) {
					throw new CayenneRuntimeException("No PK column '%s' found in data row.", pkNames[i]);
				}
				return null;
			}
			idMap.put(pkNames[i], val);
		}
		return ObjectId.of(name, idMap);
	}

	/**
	 * @param plan the plan of the entity, as returned by {@link #planFor(CompiledEntity)}
	 */
	Persistent objectFromDataRow(DataRow row, ObjectId anId, CompiledEntity rowEntity, MergePlan plan) {

		// this condition is valid - see comments on 'createObjectId' for
		// details
		if (anId == null) {
			return null;
		}

		// this will create a HOLLOW object if it is not registered yet
		Persistent object = context.findOrCreateObject(anId);

		// resolve additional Object IDs for flattened attributes
		resolveAdditionalIds(row, object, rowEntity);

		DataContextObjectStore store = context.getObjectStore();

		// a merge into a MODIFIED or DELETED object may record changes, so it needs the changes lock, taken before
		// the object lock. The state is peeked at without a lock to pick the locks, and is checked again under them
		if (!isDirty(object)) {
			boolean merged = store.withObjectLock(anId, () -> {
				if (isDirty(object)) {
					return false;
				}
				merge(object, row, plan);
				return true;
			});

			if (merged) {
				return object;
			}
		}

		return store.withChangesLock(() -> store.withObjectLock(anId, () -> {
			merge(object, row, plan);
			return object;
		}));
	}

	private static boolean isDirty(Persistent object) {
		int state = object.getPersistenceState();
		return state == PersistenceState.MODIFIED || state == PersistenceState.DELETED;
	}

	/**
	 * Merges the row into the object under the object lock held by the caller, unless the object already reflects a
	 * newer snapshot than the row.
	 */
	private void merge(Persistent object, DataRow row, MergePlan plan) {
		switch (object.getPersistenceState()) {
		case PersistenceState.COMMITTED:
		case PersistenceState.MODIFIED:
		case PersistenceState.DELETED:
			// process the above only if refresh is requested...
			if (refreshObjects && row.getVersion() >= object.getSnapshotVersion()) {
				DataRowUtils.mergeObjectWithSnapshot(context, object, row, plan);
				object.setSnapshotVersion(row.getVersion());
			}
			break;
		case PersistenceState.HOLLOW:
			// a HOLLOW object has no state to protect, so any snapshot is better than none
			if (!refreshObjects) {
				DataRow cachedRow = cache.getCachedSnapshot(object.getObjectId());
				if (cachedRow != null) {
					// the cached row may be of other columns, which the plan's columns resolve again
					row = cachedRow;
				}
			}
			DataRowUtils.mergeObjectWithSnapshot(context, object, row, plan);
			object.setSnapshotVersion(row.getVersion());
			break;
		default:
			break;
		}
	}

    private void resolveAdditionalIds(DataRow row, Persistent object, CompiledEntity rowEntity) {
	    if(rowEntity.flattenedTables().isEmpty()) {
	        return;
        }

	    for(Map.Entry<CayennePath, CompiledFlattenedTable> entry : rowEntity.flattenedTables().entrySet()) {
            CompiledTable table = entry.getValue().table();
            CayennePath path = entry.getKey();
            ObjectId objectId = createObjectId(row, "db:" + table.name(), table.primaryKey(), path, false);
            if(objectId != null) {
				context.getObjectStore().markFlattenedPath(object.getObjectId(), path, objectId);
            }
        }
    }

    CompiledEntity getEntity() {
		return entity;
	}

	ObjectId createObjectId(DataRow dataRow, CompiledEntity entity, CayennePath namePrefix) {
        Collection<CompiledColumn> pk = entity == this.entity
                ? this.primaryKey
                : entity.table().primaryKey();
        return createObjectId(dataRow, entity.name(), pk, namePrefix, true);
    }

    ObjectId createObjectId(DataRow dataRow, String name, Collection<CompiledColumn> pk, CayennePath namePrefix,
							boolean strict) {
		// ... handle special case - PK.size == 1
		// use some not-so-significant optimizations...

		if (pk.size() == 1) {
			CompiledColumn column = pk.iterator().next();
			String key = namePrefix.dot(column.name()).value();
			Object val = dataRow.get(key);

			// this is possible when processing left outer joint prefetches
			if (val == null) {
				if(strict && !dataRow.containsKey(key)) {
					throw new CayenneRuntimeException("No PK column '%s' found in data row.", key);
				}
				return null;
			}

			// PUT without a prefix
			return ObjectId.of(name, column.name(), val);
		}

		// ... handle generic case - PK.size > 1

		Map<String, Object> idMap = HashMap.newHashMap(pk.size());
		for (final CompiledColumn column : pk) {
			String key = namePrefix.dot(column.name()).value();
			Object val = dataRow.get(key);

			// this is possible when processing left outer joint prefetches
			if (val == null) {
				if(strict && !dataRow.containsKey(key)) {
					throw new CayenneRuntimeException("No PK column '%s' found in data row.", key);
				}
				return null;
			}

			// PUT without a prefix
			idMap.put(column.name(), val);
		}

		return ObjectId.of(name, idMap);
	}

	interface EntityResolutionStrategy {

		CompiledEntity entityForRow(DataRow row);
	}

	class NoInheritanceStrategy implements EntityResolutionStrategy {

		public final CompiledEntity entityForRow(DataRow row) {
			return entity;
		}
	}

	class InheritanceStrategy implements EntityResolutionStrategy {

		public final CompiledEntity entityForRow(DataRow row) {
			String entityName = row.getEntityName();

			// null either means a bug in Cayenne (e.g. CAY-2101) or the inheritance qualifiers are messed up
			if(entityName == null) {
				LOGGER.warn("** Null entity name for DataRow, can't resolve sub entity for {}", entity.name());
				return entity;
			}

			return context.getMapping().entity(entityName);
		}
	}

}
