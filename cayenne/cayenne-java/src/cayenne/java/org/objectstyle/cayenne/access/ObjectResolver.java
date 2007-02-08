/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.EntityInheritanceTree;
import org.objectstyle.cayenne.map.ObjEntity;

/**
 * DataRows-to-objects converter for a specific ObjEntity.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ObjectResolver {

    DataContext context;
    ObjEntity entity;
    List primaryKey;

    EntityInheritanceTree inheritanceTree;
    boolean refreshObjects;
    boolean resolveInheritance;
    DataRowStore cache;

    ObjectResolver(DataContext context, ObjEntity entity, boolean refresh,
            boolean resolveInheritanceHierarchy) {
        init(context, entity, refresh, resolveInheritanceHierarchy);
    }

    void init(
            DataContext context,
            ObjEntity entity,
            boolean refresh,
            boolean resolveInheritanceHierarchy) {
        // sanity check
        DbEntity dbEntity = entity.getDbEntity();
        if (dbEntity == null) {
            throw new CayenneRuntimeException("ObjEntity '"
                    + entity.getName()
                    + "' has no DbEntity.");
        }

        this.primaryKey = dbEntity.getPrimaryKey();
        if (primaryKey.size() == 0) {
            throw new CayenneRuntimeException("Won't be able to create ObjectId for '"
                    + entity.getName()
                    + "'. Reason: DbEntity '"
                    + dbEntity.getName()
                    + "' has no Primary Key defined.");
        }

        this.context = context;
        this.cache = context.getObjectStore().getDataRowCache();
        this.refreshObjects = refresh;
        this.entity = entity;
        this.inheritanceTree = context.getEntityResolver().lookupInheritanceTree(entity);
        this.resolveInheritance = (inheritanceTree != null)
                ? resolveInheritanceHierarchy
                : false;
    }

    /**
     * Properly synchronized version of 'objectsFromDataRows'.
     */
    List synchronizedObjectsFromDataRows(List rows) {
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
    List objectsFromDataRows(List rows) {
        if (rows == null || rows.size() == 0) {
            return new ArrayList(1);
        }

        List results = new ArrayList(rows.size());
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
    List relatedObjectsFromDataRows(List rows, PrefetchProcessorNode node) {
        if (rows == null || rows.size() == 0) {
            return new ArrayList(1);
        }

        ObjEntity sourceObjEntity = (ObjEntity) node.getIncoming().getSourceEntity();
        String relatedIdPrefix = node.getIncoming().getReverseDbRelationshipPath() + ".";

        List results = new ArrayList(rows.size());
        Iterator it = rows.iterator();

        while (it.hasNext()) {

            DataRow row = (DataRow) it.next();
            DataObject object = objectFromDataRow(row);
            results.add(object);

            // link with parent

            // The algorithm below of building an ID doesn't take inheritance into
            // account, so there maybe a miss...
            ObjectId id = createObjectId(row, sourceObjEntity, relatedIdPrefix);
            DataObject parentObject = (DataObject) context.getObjectStore().getNode(id);

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
    DataObject objectFromDataRow(DataRow row) {

        // determine entity to use
        ObjEntity objectEntity;

        if (resolveInheritance) {
            objectEntity = inheritanceTree.entityMatchingRow(row);

            // still null.... looks like inheritance qualifiers are messed up
            if (objectEntity == null) {
                objectEntity = entity;
            }
        }
        else {
            objectEntity = entity;
        }

        // not using DataRow.createObjectId for performance reasons - ObjectResolver has
        // all needed metadata already cached.
        ObjectId anId = createObjectId(row, objectEntity, null);

        // this will create a HOLLOW object if it is not registered yet
        DataObject object = (DataObject) context.localObject(anId, null);

        // deal with object state
        int state = object.getPersistenceState();
        switch (state) {
            case PersistenceState.COMMITTED:
            case PersistenceState.MODIFIED:
            case PersistenceState.DELETED:
                // process the above only if refresh is requested...
                if (refreshObjects) {
                    DataRowUtils.mergeObjectWithSnapshot(objectEntity, object, row);
                    object.setSnapshotVersion(row.getVersion());
                }
                break;
            case PersistenceState.HOLLOW:
                if(!refreshObjects) {
                    DataRow cachedRow = cache.getCachedSnapshot(anId);
                    if(cachedRow != null) {
                        row = cachedRow;
                    }
                }
                DataRowUtils.mergeObjectWithSnapshot(objectEntity, object, row);
                object.setSnapshotVersion(row.getVersion());
                break;
            default:
                break;
        }

        object.fetchFinished();
        return object;
    }

    ObjEntity getEntity() {
        return entity;
    }

    ObjectId createObjectId(DataRow dataRow, ObjEntity objEntity, String namePrefix) {

        List pk = objEntity == this.entity ? this.primaryKey : objEntity
                .getDbEntity()
                .getPrimaryKey();

        boolean prefix = namePrefix != null && namePrefix.length() > 0;

        // ... handle special case - PK.size == 1
        // use some not-so-significant optimizations...

        if (pk.size() == 1) {
            DbAttribute attribute = (DbAttribute) pk.get(0);

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

        Map idMap = new HashMap(pk.size() * 2);
        Iterator it = pk.iterator();
        while (it.hasNext()) {
            DbAttribute attribute = (DbAttribute) it.next();

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
