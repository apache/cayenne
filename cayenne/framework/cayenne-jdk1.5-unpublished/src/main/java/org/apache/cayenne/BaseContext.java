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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.cache.MapQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.Property;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * A common base superclass for Cayenne ObjectContext implementors.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public abstract class BaseContext implements ObjectContext {

    // if we are to pass the context around, channel should be left alone and
    // reinjected later if needed
    protected transient DataChannel channel;
    protected QueryCache queryCache;

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

    @SuppressWarnings("unchecked")
    public abstract List performQuery(Query query);

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
            List<?> objects = performQuery(new ObjectIdQuery(
                    oid,
                    false,
                    ObjectIdQuery.CACHE));

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
            ClassDescriptor classDescriptor = getEntityResolver().getClassDescriptor(
                    object.getObjectId().getEntityName());
            Property propertyDescriptor = classDescriptor.getProperty(property);

            // If we don't have a property descriptor, there's not much we can do.
            // Let the caller know that the specified property could not be found and list
            // all of the properties that could be so the caller knows what can be used.
            if (propertyDescriptor == null) {
                final StringBuilder errorMessage = new StringBuilder();

                errorMessage.append(String.format(
                        "Property '%s' is not declared for entity '%s'.",
                        property,
                        object.getObjectId().getEntityName()));

                errorMessage.append(" Declared properties are: ");

                // Grab each of the declared properties.
                final List<String> properties = new ArrayList<String>();
                classDescriptor.visitProperties(new PropertyVisitor() {

                    public boolean visitAttribute(final AttributeProperty property) {
                        properties.add(property.getName());

                        return true;
                    }

                    public boolean visitToOne(final ToOneProperty property) {
                        properties.add(property.getName());

                        return true;
                    }

                    public boolean visitToMany(final ToManyProperty property) {
                        properties.add(property.getName());

                        return true;
                    }
                });

                // Now add the declared property names to the error message.
                boolean first = true;
                for (String declaredProperty : properties) {
                    if (first) {
                        errorMessage.append(String.format("'%s'", declaredProperty));

                        first = false;
                    }
                    else {
                        errorMessage.append(String.format(", '%s'", declaredProperty));
                    }
                }

                errorMessage.append(".");

                throw new CayenneRuntimeException(errorMessage.toString());
            }

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

    /**
     * Returns {@link QueryCache}, creating it on the fly if needed.
     */
    public synchronized QueryCache getQueryCache() {
        if (queryCache == null) {
            synchronized (this) {
                if (queryCache == null) {
                    // TODO: andrus, 7/27/2006 - figure out the factory stuff like we have
                    // in DataContext
                    queryCache = new MapQueryCache();
                }
            }
        }
        
        return queryCache;
    }

    /**
     * Sets a QueryCache to be used for storing cached query results.
     */
    public synchronized void setQueryCache(QueryCache queryCache) {
        this.queryCache = queryCache;
    }
}
