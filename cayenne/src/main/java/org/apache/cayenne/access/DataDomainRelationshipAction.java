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
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves a relationship at the root of the DataChannel stack. Derives the related object from the snapshot cache
 * when the source FK allows it, and fetches related objects from the database otherwise.
 *
 * @since 5.0
 */
class DataDomainRelationshipAction {

    private final DataDomain domain;
    private final DataContext context;
    private final ObjectId sourceId;
    private final String relationshipName;

    DataDomainRelationshipAction(DataDomain domain, ObjectContext context, ObjectId sourceId,
                                 String relationshipName) {

        if (!(context instanceof DataContext dataContext)) {
            throw new IllegalArgumentException("DataDomain can only work with DataContext. "
                    + "Unsupported context type: " + context);
        }

        this.domain = domain;
        this.context = dataContext;
        this.sourceId = sourceId;
        this.relationshipName = relationshipName;
    }

    List<Persistent> execute() {

        if (sourceId.isTemporary() && !sourceId.isReplacementIdAttached()) {
            throw new CayenneRuntimeException("Can't resolve relationship '%s' for temporary id: %s",
                    relationshipName, sourceId);
        }

        ClassDescriptor sourceDescriptor = domain.getEntityResolver().getClassDescriptor(sourceId.getEntityName());
        ArcProperty arc = (ArcProperty) sourceDescriptor.getProperty(relationshipName);
        if (arc == null) {
            throw new CayenneRuntimeException("No relationship named %s found in entity %s; object id: %s",
                    relationshipName, sourceId.getEntityName(), sourceId);
        }

        List<Persistent> cached = resolveFromCache(arc);
        return cached != null ? cached : resolveFromDB(arc);
    }

    /**
     * Attempts to resolve a to-one relationship from the snapshot cache, deriving the target id from the source
     * object FK. Returns null if the relationship or the cache state don't allow that.
     */
    private List<Persistent> resolveFromCache(ArcProperty arc) {

        ObjRelationship relationship = arc.getRelationship();

        // only a to-one relationship pointing to the target PK allows to derive the target id from the FK
        if (relationship.isSourceIndependentFromTargetChange()) {
            return null;
        }

        // we can assume that there is one and only one DbRelationship as we previously checked
        // that "!isSourceIndependentFromTargetChange"
        DbRelationship dbRelationship = relationship.getDbRelationships().getFirst();

        // FK pointing to a unique field that is a 'fake' PK (CAY-1755)... It is not sufficient to generate target
        // ObjectId.
        DbEntity targetEntity = dbRelationship.getTargetEntity();
        if (dbRelationship.getJoins().size() < targetEntity.getPrimaryKeys().size()) {
            return null;
        }

        DataContextObjectStore objectStore = context.getObjectStore();
        DataRow sourceRow = objectStore.getCachedSnapshot(sourceId);
        if (sourceRow == null) {
            return null;
        }

        ObjectId targetId = sourceRow.createTargetObjectId(relationship.getTargetEntityName(), dbRelationship);

        // null id means that FK is null...
        if (targetId == null) {
            return new ArrayList<>(1);
        }

        DataRow targetRow = objectStore.getCachedSnapshot(targetId);
        if (targetRow != null) {
            return objectsFromCachedRows(arc, List.of(targetRow));
        }

        // check whether a non-null FK is enough to assume non-null target, and if so, create a hollow object without
        // going to the database. Never pass a partial snapshot to ObjectResolver (CAY-724).
        if (relationship.isSourceDefiningTargetPrecenseAndType(domain.getEntityResolver())) {
            List<Persistent> result = new ArrayList<>(1);
            result.add(context.findOrCreateObject(targetId));
            return result;
        }

        return null;
    }

    private List<Persistent> resolveFromDB(ArcProperty arc) {

        ObjRelationship relationship = arc.getRelationship();
        Expression qualifier = ExpressionFactory.matchExp(
                ExpressionFactory.dbPathExp(relationship.getReverseDbRelationshipPath()),
                sourceId);

        ObjectSelect<Persistent> query = ObjectSelect
                .query(Persistent.class, relationship.getTargetEntityName())
                .where(qualifier);

        @SuppressWarnings("unchecked")
        List<Persistent> related = (List<Persistent>) QueryResults.firstList(domain.onQuery(context, query, false));
        return related;
    }

    /**
     * Converts cached snapshots to objects without refreshing the objects that are already registered in the
     * context, as the cache has no newer state than the objects themselves.
     */
    private List<Persistent> objectsFromCachedRows(ArcProperty arc, List<DataRow> rows) {

        List<Persistent> objects = new ObjectResolver(context, arc.getTargetDescriptor(), false)
                .synchronizedObjectsFromDataRows(rows);

        LifecycleCallbackRegistry callbackRegistry = domain.getEntityResolver().getCallbackRegistry();
        if (!callbackRegistry.isEmpty(LifecycleEvent.POST_LOAD)) {
            callbackRegistry.performCallbacks(LifecycleEvent.POST_LOAD, objects);
        }

        return objects;
    }
}
