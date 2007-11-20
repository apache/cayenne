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
package org.apache.cayenne;

import java.util.Collection;
import java.util.List;

import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.Property;

/**
 * A common base superclass for Cayenne ObjectContext implementors.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public abstract class BaseContext implements ObjectContext {

    // if we are to pass CayenneContext around, channel should be left alone and
    // reinjected later if needed
    protected transient DataChannel channel;

    public abstract void commitChanges();

    public abstract void commitChangesToParent();

    public abstract void deleteObject(Object object) throws DeleteDenyException;

    public abstract Collection<?> deletedObjects();

    public DataChannel getChannel() {
        return channel;
    }

    public abstract EntityResolver getEntityResolver();

    public abstract GraphManager getGraphManager();

    public abstract Persistent localObject(ObjectId id, Object prototype);

    public abstract Collection<?> modifiedObjects();

    public abstract <T> T newObject(Class<T> persistentClass);

    public abstract void registerNewObject(Object object);

    public abstract Collection<?> newObjects();

    public abstract QueryResponse performGenericQuery(Query query);

    public abstract List<?> performQuery(Query query);

    /**
     * @deprecated since 3.0 this method is replaced by
     *             {@link #prepareForAccess(Persistent, String, boolean)}.
     */
    public void prepareForAccess(Persistent object, String property) {
        prepareForAccess(object, property, false);
    }

    public void prepareForAccess(Persistent object, String property, boolean lazyFaulting) {
        if (object.getPersistenceState() == PersistenceState.HOLLOW) {

            ObjectId oid = object.getObjectId();
            List<?> objects = performQuery(new ObjectIdQuery(oid, false, ObjectIdQuery.CACHE));

            if (objects.size() == 0) {
                throw new FaultFailureException(
                        "Error resolving fault, no matching row exists in the database for ObjectId: "
                                + oid);
            }
            else if (objects.size() > 1) {
                throw new FaultFailureException(
                        "Error resolving fault, more than one row exists in the database for ObjectId: "
                                + oid);
            }

            // sanity check...
            if (object.getPersistenceState() != PersistenceState.COMMITTED) {

                String state = PersistenceState.persistenceStateName(object
                        .getPersistenceState());

                // TODO: andrus 4/13/2006, modified and deleted states are possible due to
                // a race condition, should we handle them here?

                throw new FaultFailureException(
                        "Error resolving fault for ObjectId: "
                                + oid
                                + " and state ("
                                + state
                                + "). Possible cause - matching row is missing from the database.");
            }
        }

        // resolve relationship fault
        if (lazyFaulting && property != null) {
            Property propertyDescriptor = getEntityResolver().getClassDescriptor(
                    object.getObjectId().getEntityName()).getProperty(property);

            // this should trigger fault resolving
            propertyDescriptor.readProperty(object);
        }
    }

    public abstract void propertyChanged(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue);

    public abstract void rollbackChanges();

    public abstract void rollbackChangesLocally();

    public abstract Collection<?> uncommittedObjects();
}
