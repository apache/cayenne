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
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.access.lifecycle.LifecycleEventManager;

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

        // only a to-one relationship holding the FK on the source allows to derive the target id from it
        if (!relationship.isForeignKey()) {
            return null;
        }

        // an FK to a unique column that is a 'fake' PK (CAY-1755) is not sufficient to generate the target id
        if (!relationship.isToPrimaryKey()) {
            return null;
        }

        DataContextObjectStore objectStore = context.getObjectStore();
        DataRow sourceRow = objectStore.getCachedSnapshot(sourceId);
        if (sourceRow == null) {
            return null;
        }

        // a null FK means a null target
        ObjectId targetId = ObjectSnapshots.targetId(relationship, sourceRow);
        if (targetId == null) {
            return new ArrayList<>(1);
        }

        DataRow targetRow = objectStore.getCachedSnapshot(targetId);
        if (targetRow != null) {
            return objectsFromCachedRows(arc, List.of(targetRow));
        }

        // check whether a non-null FK is enough to assume non-null target, and if so, create a hollow object without
        // going to the database. Never pass a partial snapshot to ObjectResolver (CAY-724).
        if (DataRowUtils.isTargetDeterminedByFK(relationship, domain.getEntityResolver())) {
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

        OperationContext execContext = new OperationContext(context, query, domain.getEntityResolver(), false);
        @SuppressWarnings("unchecked")
        List<Persistent> related = (List<Persistent>) QueryResults.firstList(domain.onQuery(execContext));
        return related;
    }

    /**
     * Converts cached snapshots to objects without refreshing the objects that are already registered in the
     * context, as the cache has no newer state than the objects themselves.
     */
    private List<Persistent> objectsFromCachedRows(ArcProperty arc, List<DataRow> rows) {

        List<Persistent> objects = new ObjectResolver(context, arc.getTargetDescriptor(), false)
                .objectsFromDataRows(rows);

        LifecycleEventManager postLoad = domain.getLifecycleEventsManager().postLoad();
        if (postLoad.hasListeners()) {
            postLoad.onEvent(objects);
        }

        return objects;
    }
}
