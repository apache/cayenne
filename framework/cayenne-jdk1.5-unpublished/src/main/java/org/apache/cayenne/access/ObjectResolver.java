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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * DataRows-to-objects converter for a specific ObjEntity.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ObjectResolver {

    DataContext context;
    ClassDescriptor descriptor;
    Collection<DbAttribute> primaryKey;

    EntityInheritanceTree inheritanceTree;
    boolean refreshObjects;
    boolean resolveInheritance;
    DataRowStore cache;

    ObjectResolver(DataContext context, ClassDescriptor descriptor, boolean refresh,
            boolean resolveInheritanceHierarchy) {
        init(context, descriptor, refresh, resolveInheritanceHierarchy);
    }

    void init(
            DataContext context,
            ClassDescriptor descriptor,
            boolean refresh,
            boolean resolveInheritanceHierarchy) {
        // sanity check
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
        this.inheritanceTree = context.getEntityResolver().lookupInheritanceTree(
                descriptor.getEntity());
        this.resolveInheritance = (inheritanceTree != null)
                ? resolveInheritanceHierarchy
                : false;
    }

    /**
     * Properly synchronized version of 'objectsFromDataRows'.
     */
    List<Persistent> synchronizedObjectsFromDataRows(List rows) {
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
    List<Persistent> objectsFromDataRows(List rows) {
        if (rows == null || rows.size() == 0) {
            return new ArrayList<Persistent>(1);
        }

        List<Persistent> results = new ArrayList<Persistent>(rows.size());
        Iterator it = rows.iterator();

        while (it.hasNext()) {
            results.add(objectFromDataRow((DataRow) it.next()));
        }

        // now deal with snapshots
        cache.snapshotsUpdatedForObjects(results, rows, refreshObjects);

        return results;
    }

    /**
     * Processes a list of rows for a result set that has objects related to a set of
     * parent objects via some relationship defined in PrefetchProcessorNode parameter.
     * Relationships are linked in this method, assuming that parent PK columns are
     * included in each row and are prefixed with DB relationship name.
     * <p>
     * Synchronization note. This method requires EXTERNAL synchronization on ObjectStore
     * and DataRowStore.
     * </p>
     */
    List<Persistent> relatedObjectsFromDataRows(List rows, PrefetchProcessorNode node) {
        if (rows == null || rows.size() == 0) {
            return new ArrayList<Persistent>(1);
        }

        ObjEntity sourceObjEntity = (ObjEntity) node
                .getIncoming()
                .getRelationship()
                .getSourceEntity();
        String relatedIdPrefix = node
                .getIncoming()
                .getRelationship()
                .getReverseDbRelationshipPath()
                + ".";

        List<Persistent> results = new ArrayList<Persistent>(rows.size());
        Iterator it = rows.iterator();

        while (it.hasNext()) {

            DataRow row = (DataRow) it.next();
            Persistent object = objectFromDataRow(row);
            results.add(object);

            // link with parent

            // The algorithm below of building an ID doesn't take inheritance into
            // account, so there maybe a miss...
            ObjectId id = createObjectId(row, sourceObjEntity, relatedIdPrefix);
            Persistent parentObject = (Persistent) context.getObjectStore().getNode(id);

            // don't attach to hollow objects
            if (parentObject != null
                    && parentObject.getPersistenceState() != PersistenceState.HOLLOW) {
                node.linkToParent(object, parentObject);
            }
        }

        // now deal with snapshots
        cache.snapshotsUpdatedForObjects(results, rows, refreshObjects);

        return results;
    }

    /**
     * Processes a single row. This method does not synchronize on ObjectStore and doesn't
     * send snapshot updates. These are responsibilities of the caller.
     */
    Persistent objectFromDataRow(DataRow row) {

        // determine entity to use
        ClassDescriptor classDescriptor;

        if (resolveInheritance) {
            ObjEntity objectEntity = inheritanceTree.entityMatchingRow(row);

            // null probably means that inheritance qualifiers are messed up
            classDescriptor = (objectEntity != null) ? context
                    .getEntityResolver()
                    .getClassDescriptor(objectEntity.getName()) : descriptor;
        }
        else {
            classDescriptor = descriptor;
        }

        // not using DataRow.createObjectId for performance reasons - ObjectResolver has
        // all needed metadata already cached.
        ObjectId anId = createObjectId(row, classDescriptor.getEntity(), null);

        // this will create a HOLLOW object if it is not registered yet
        Persistent object = context.localObject(anId, null);

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
            if (val == null) {
                throw new CayenneRuntimeException("Null value for '"
                        + key
                        + "'. Snapshot: "
                        + dataRow
                        + ". Prefix: "
                        + namePrefix);
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
            if (val == null) {
                throw new CayenneRuntimeException("Null value for '"
                        + key
                        + "'. Snapshot: "
                        + dataRow
                        + ". Prefix: "
                        + namePrefix);
            }

            // PUT without a prefix
            idMap.put(attribute.getName(), val);
        }

        return new ObjectId(objEntity.getName(), idMap);
    }
}
