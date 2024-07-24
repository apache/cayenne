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
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.ValueInjector;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

import java.util.Collection;
import java.util.Map;

/**
 * {@link DataContext} delegates creation and registration of new objects to this class
 */
class DataContextObjectCreator {

    final DataContext context;

    DataContextObjectCreator(DataContext context) {
        this.context = context;
    }

    /**
     * Create new object for the given persistent class
     *
     * @param persistentClass to create object from
     * @return a new persistent object
     * @param <T> type of the object
     * @see DataContext#newObject(Class)
     */
    <T> T newObject(Class<T> persistentClass) {
        if (persistentClass == null) {
            throw new NullPointerException("Null 'persistentClass'");
        }

        ObjEntity entity = context.getEntityResolver().getObjEntity(persistentClass);
        if (entity == null) {
            throw new IllegalArgumentException("Class is not mapped with Cayenne: " + persistentClass.getName());
        }

        @SuppressWarnings("unchecked")
        T object = (T) newObject(entity.getName());
        return object;
    }

    /**
     * Create new object for the given entity name
     * @param entityName name of the ObjEntity
     * @return a new persistent object
     * @see DataContext#newObject(String)
     */
    Persistent newObject(String entityName) {
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(entityName);
        if (descriptor == null) {
            throw new IllegalArgumentException("Invalid entity name: " + entityName);
        }

        Persistent object;
        try {
            object = (Persistent) descriptor.createObject();
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        // this will initialize to-many lists
        descriptor.injectValueHolders(object);

        // NOTE: the order of initialization of persistence artifacts below is important - do not change it lightly
        object.setObjectId(ObjectId.of(entityName));

        injectInitialValue(object);

        return object;
    }

    /**
     * Register new object created outside the context
     * @param object to register
     * @see DataContext#registerNewObject(Object)
     */
    void registerNewObject(Object object) {
        if (object == null) {
            throw new NullPointerException("Can't register null object.");
        }

        ObjEntity entity = context.getEntityResolver().getObjEntity((Persistent) object);
        if (entity == null) {
            throw new IllegalArgumentException("Can't find ObjEntity for Persistent class: "
                    + object.getClass().getName() + ", class is likely not mapped.");
        }

        final Persistent persistent = (Persistent) object;

        // sanity check - maybe already registered
        if (persistent.getObjectId() != null) {
            if (persistent.getObjectContext() == context) {
                // already registered, just ignore
                return;
            } else if (persistent.getObjectContext() != null) {
                throw new IllegalStateException("Persistent is already registered with another DataContext. "
                        + "Try using 'localObjects()' instead.");
            }
        } else {
            persistent.setObjectId(ObjectId.of(entity.getName()));
        }

        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(entity.getName());
        if (descriptor == null) {
            throw new IllegalArgumentException("Invalid entity name: " + entity.getName());
        }

        injectInitialValue(object);

        // now we need to find all arc changes, inject missing value holders and
        // pull in all transient connected objects

        descriptor.visitProperties(new PropertyVisitor() {

            public boolean visitToMany(ToManyProperty property) {
                property.injectValueHolder(persistent);

                if (!property.isFault(persistent)) {

                    Object value = property.readProperty(persistent);
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Collection<Map.Entry<?,?>> collection = (value instanceof Map)
                            ? ((Map) value).entrySet()
                            : (Collection<Map.Entry<?, ?>>) value;

                    for (Object target : collection) {
                        if (target instanceof Persistent) {
                            Persistent targetDO = (Persistent) target;

                            // make sure it is registered
                            registerNewObject(targetDO);
                            context.getObjectStore().arcCreated(persistent.getObjectId(), targetDO.getObjectId(), new ArcId(property));
                        }
                    }
                }
                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                Object target = property.readPropertyDirectly(persistent);

                if (target instanceof Persistent) {

                    Persistent targetDO = (Persistent) target;

                    // make sure it is registered
                    registerNewObject(targetDO);
                    context.getObjectStore().arcCreated(persistent.getObjectId(), targetDO.getObjectId(), new ArcId(property));
                }
                return true;
            }

            public boolean visitAttribute(AttributeProperty property) {
                return true;
            }
        });
    }

    /**
     * If ObjEntity qualifier is set, asks it to inject initial value to an object.
     * Also performs all Persistent initialization operations
     */
    protected void injectInitialValue(Object obj) {
        // must follow this exact order of property initialization per CAY-653,
        // i.e. have the id and the context in place BEFORE setPersistence is called

        Persistent object = (Persistent) obj;

        object.setObjectContext(context);
        object.setPersistenceState(PersistenceState.NEW);

        GraphManager graphManager = context.getGraphManager();
        synchronized (graphManager) {
            graphManager.registerNode(object.getObjectId(), object);
            graphManager.nodeCreated(object.getObjectId());
        }

        ObjEntity entity;
        try {
            entity = context.getEntityResolver().getObjEntity(object.getObjectId().getEntityName());
        } catch (CayenneRuntimeException ex) {
            // ObjEntity cannot be fetched, ignored
            entity = null;
        }

        if (entity != null) {
            if (entity.getDeclaredQualifier() instanceof ValueInjector) {
                ((ValueInjector) entity.getDeclaredQualifier()).injectValue(object);
            }
        }

        // invoke callbacks
        context.getEntityResolver()
                .getCallbackRegistry().performCallbacks(LifecycleEvent.POST_ADD, object);
    }
}
