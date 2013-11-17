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

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * DataRows-to-objects converter for a specific ObjEntity.
 * 
 * @since 1.2
 */
class ObjectResolver {

    DataContext context;
    ClassDescriptor descriptor;
    Collection<DbAttribute> primaryKey;

    boolean refreshObjects;
    DataRowStore cache;
    DescriptorResolutionStrategy descriptorResolutionStrategy;

    ObjectResolver(DataContext context, ClassDescriptor descriptor, boolean refresh) {

        // sanity check
        if (descriptor == null || descriptor.getEntity() == null) {
            throw new CayenneRuntimeException(
                    "Set up Object entity or use rowFetchingQuery");
        }

        DbEntity dbEntity = descriptor.getEntity().getDbEntity();
        if (dbEntity == null) {
            throw new CayenneRuntimeException("ObjEntity '"
                    + descriptor.getEntity().getName()
                    + "' has no DbEntity.");
        }

        this.primaryKey = dbEntity.getPrimaryKeys();
        if (primaryKey.size() == 0) {
            throw new CayenneRuntimeException("Won't be able to create ObjectId for '"
                    + descriptor.getEntity().getName()
                    + "'. Reason: DbEntity '"
                    + dbEntity.getName()
                    + "' has no Primary Key defined.");
        }

        this.context = context;
        this.cache = context.getObjectStore().getDataRowCache();
        this.refreshObjects = refresh;
        this.descriptor = descriptor;

        this.descriptorResolutionStrategy = descriptor.hasSubclasses()
                ? new InheritanceStrategy()
                : new NoInheritanceStrategy();
    }

    PrefetchProcessorNode synchronizedRootResultNodeFromDataRows(
            List<? extends DataRow> rows) {
        PrefetchProcessorNode rootNode = new PrefetchProcessorNode(null, null);
        rootNode.setObjects(synchronizedObjectsFromDataRows(rows));
        return rootNode;
    }

    /**
     * Properly synchronized version of 'objectsFromDataRows'.
     */
    List<Persistent> synchronizedObjectsFromDataRows(List<? extends DataRow> rows) {
        synchronized (context.getObjectStore()) {
            return objectsFromDataRows(rows);
        }
    }

    /**
     * Converts rows to objects.
     * <p>
     * Synchronization note. This method requires EXTERNAL synchronization on ObjectStore
     * and DataRowStore.
     * </p>
     */
    List<Persistent> objectsFromDataRows(List<? extends DataRow> rows) {
        if (rows == null || rows.size() == 0) {
            return new ArrayList<Persistent>(1);
        }

        List<Persistent> results = new ArrayList<Persistent>(rows.size());
        for (DataRow row : rows) {
            // nulls are possible here since 3.0 for soem varieties of EJBQL
            results.add(objectFromDataRow(row));
        }

        // now deal with snapshots
        cache.snapshotsUpdatedForObjects(results, rows, refreshObjects);
        return results;
    }

    Persistent objectFromDataRow(DataRow row) {
        // determine entity to use
        ClassDescriptor classDescriptor = descriptorResolutionStrategy
                .descriptorForRow(row);

        // not using DataRow.createObjectId for performance reasons - ObjectResolver
        // has all needed metadata already cached.
        ObjectId anId = createObjectId(row, classDescriptor.getEntity(), null);
        return objectFromDataRow(row, anId, classDescriptor);
    }

    Persistent objectFromDataRow(
            DataRow row,
            ObjectId anId,
            ClassDescriptor classDescriptor) {

        // this condition is valid - see comments on 'createObjectId' for details
        if (anId == null) {
            return null;
        }

        // this will create a HOLLOW object if it is not registered yet
        Persistent object = context.findOrCreateObject(anId);

        // deal with object state
        int state = object.getPersistenceState();
        switch (state) {
            case PersistenceState.COMMITTED:
            case PersistenceState.MODIFIED:
            case PersistenceState.DELETED:
                // process the above only if refresh is requested...
                if (refreshObjects) {
                    DataRowUtils.mergeObjectWithSnapshot(
                            context,
                            classDescriptor,
                            object,
                            row);

                    if (object instanceof DataObject) {
                        ((DataObject) object).setSnapshotVersion(row.getVersion());
                    }
                }
                break;
            case PersistenceState.HOLLOW:
                if (!refreshObjects) {
                    DataRow cachedRow = cache.getCachedSnapshot(anId);
                    if (cachedRow != null) {
                        row = cachedRow;
                    }
                }
                DataRowUtils.mergeObjectWithSnapshot(
                        context,
                        classDescriptor,
                        object,
                        row);
                if (object instanceof DataObject) {
                    ((DataObject) object).setSnapshotVersion(row.getVersion());
                }
                break;
            default:
                break;
        }

        return object;
    }

    ObjEntity getEntity() {
        return descriptor.getEntity();
    }

    ClassDescriptor getDescriptor() {
        return descriptor;
    }

    EntityResolver getEntityResolver() {
        return context.getEntityResolver();
    }

    ObjectContext getContext() {
        return context;
    }

    ObjectId createObjectId(DataRow dataRow, ObjEntity objEntity, String namePrefix) {

        Collection<DbAttribute> pk = objEntity == this.descriptor.getEntity()
                ? this.primaryKey
                : objEntity.getDbEntity().getPrimaryKeys();

        boolean prefix = namePrefix != null && namePrefix.length() > 0;

        // ... handle special case - PK.size == 1
        // use some not-so-significant optimizations...

        if (pk.size() == 1) {
            DbAttribute attribute = pk.iterator().next();

            String key = (prefix) ? namePrefix + attribute.getName() : attribute
                    .getName();

            Object val = dataRow.get(key);

            // this is possible when processing left outer joint prefetches
            if (val == null) {
                return null;
            }

            // PUT without a prefix
            return new ObjectId(objEntity.getName(), attribute.getName(), val);
        }

        // ... handle generic case - PK.size > 1

        Map<String, Object> idMap = new HashMap<String, Object>(pk.size() * 2);
        for (final DbAttribute attribute : pk) {

            String key = (prefix) ? namePrefix + attribute.getName() : attribute
                    .getName();

            Object val = dataRow.get(key);

            // this is possible when processing left outer joint prefetches
            if (val == null) {
                return null;
            }

            // PUT without a prefix
            idMap.put(attribute.getName(), val);
        }

        return new ObjectId(objEntity.getName(), idMap);
    }

    interface DescriptorResolutionStrategy {

        ClassDescriptor descriptorForRow(DataRow row);
    }

    class NoInheritanceStrategy implements DescriptorResolutionStrategy {

        public final ClassDescriptor descriptorForRow(DataRow row) {
            return descriptor;
        }
    }

    class InheritanceStrategy implements DescriptorResolutionStrategy {

        public final ClassDescriptor descriptorForRow(DataRow row) {
            String entityName = row.getEntityName();

            // null probably means that inheritance qualifiers are messed up
            return (entityName != null) ? context.getEntityResolver().getClassDescriptor(
                    entityName) : descriptor;
        }
    }
}
