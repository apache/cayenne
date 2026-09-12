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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.FaultFailureException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;

import java.util.List;

/**
 * Resolves an object by id at the root of the DataChannel stack. Converts a cached snapshot to an object when the
 * snapshot cache has one, and fetches the object from the database otherwise.
 *
 * @since 5.0
 */
class DataDomainIdQueryAction {

    private final DataDomain domain;
    private final DataContext context;
    private final ObjectId id;

    DataDomainIdQueryAction(DataDomain domain, ObjectContext context, ObjectId id) {

        if (!(context instanceof DataContext dataContext)) {
            throw new IllegalArgumentException("DataDomain can only work with DataContext. "
                    + "Unsupported context type: " + context);
        }

        this.domain = domain;
        this.context = dataContext;
        this.id = id;
    }

    Persistent execute() {

        // a temporary id without a replacement can't match anything in the database. Return null instead of failing
        // on query translation, so that the caller can report a fault failure (CAY-1651)
        if (id.isTemporary() && !id.isReplacementIdAttached()) {
            return null;
        }

        DataRow row = context.getObjectStore().getCachedSnapshot(id);
        return row != null ? objectFromCachedRow(row) : resolveFromDB();
    }

    /**
     * Converts a cached snapshot to an object without refreshing the object if it is already registered in the
     * context, as the cache has no newer state than the object itself.
     */
    private Persistent objectFromCachedRow(DataRow row) {

        // the descriptor is that of the requested entity. The snapshot may belong to one of its subentities, and the
        // resolver picks the right one from the snapshot
        ClassDescriptor descriptor = domain.getEntityResolver().getClassDescriptor(id.getEntityName());
        List<Persistent> objects = new ObjectResolver(context, descriptor, false)
                .synchronizedObjectsFromDataRows(List.of(row));

        LifecycleCallbackRegistry callbackRegistry = domain.getEntityResolver().getCallbackRegistry();
        if (!callbackRegistry.isEmpty(LifecycleEvent.POST_LOAD)) {
            callbackRegistry.performCallbacks(LifecycleEvent.POST_LOAD, objects);
        }

        return objects.getFirst();
    }

    private Persistent resolveFromDB() {

        ObjectSelect<Persistent> query = ObjectSelect
                .query(Persistent.class, id.getEntityName())
                .where(ExpressionFactory.matchAllDbExp(id.getIdSnapshot(), Expression.EQUAL_TO));

        List<?> objects = domain.onQuery(context, query, false).firstList();
        return switch (objects.size()) {
            case 0 -> null;
            case 1 -> (Persistent) objects.getFirst();
            default -> throw new FaultFailureException(
                    "Error resolving fault, more than one row exists in the database for ObjectId: " + id);
        };
    }
}
