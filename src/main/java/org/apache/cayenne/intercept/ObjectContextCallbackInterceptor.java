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
package org.apache.cayenne.intercept;

import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.DeleteDenyException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.CallbackMap;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.LifecycleCallbackRegistry;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Implements JPA-compliant "PrePersist", "PreRemove" callbacks for the ObjectContext
 * operations. <p/>Depending on how callbacks are registered, they are invoked either on
 * the persistent object instances themselves or on an instance of an arbitrary listener
 * class. Signature of a method of a persistent object is <code>"void method()"</code>,
 * while for a non-persistent listener it is <code>"void
 * method(Object)"</code>.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class ObjectContextCallbackInterceptor extends ObjectContextDecorator {

    protected LifecycleCallbackRegistry callbackRegistry;

    public void setContext(ObjectContext context) {
        super.setContext(context);

        callbackRegistry = (context != null) ? context
                .getEntityResolver()
                .getCallbackRegistry() : null;
    }

    /**
     * Creates a new object, applying "PrePersist" callbacks to it.
     */
    public Persistent newObject(Class persistentClass) {
        Persistent object = super.newObject(persistentClass);
        callbackRegistry.performCallbacks(CallbackMap.PRE_PERSIST, object);
        return object;
    }

    /**
     * Registers a new object and performs a "PrePersist" callback on it.
     */
    public void registerNewObject(Object object) {
        super.registerNewObject(object);
        callbackRegistry.performCallbacks(CallbackMap.PRE_PERSIST, object);
    }

    /**
     * Deletes an object, applying "PreRemove" callbacks to it and all its cascaded
     * dependencies.
     */
    public void deleteObject(Persistent object) throws DeleteDenyException {
        applyPreRemoveCallbacks(object);
        super.deleteObject(object);
    }

    /**
     * Recursively applies PreRemove callbacks to an object and objects that will be
     * cascaded
     */
    void applyPreRemoveCallbacks(Persistent object) {

        if (object.getPersistenceState() != PersistenceState.NEW) {
            callbackRegistry.performCallbacks(CallbackMap.PRE_REMOVE, object);
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(object);
        ClassDescriptor descriptor = getEntityResolver().getClassDescriptor(
                entity.getName());

        Iterator it = entity.getRelationships().iterator();
        while (it.hasNext()) {

            ObjRelationship relationship = (ObjRelationship) it.next();
            if (relationship.getDeleteRule() == DeleteRule.CASCADE) {

                Object related = descriptor
                        .getProperty(relationship.getName())
                        .readProperty(object);

                if (related == null) {
                    // do nothing
                }
                else if (related instanceof Collection) {
                    Iterator relatedObjects = ((Collection) related).iterator();
                    while (relatedObjects.hasNext()) {
                        applyPreRemoveCallbacks((Persistent) relatedObjects.next());
                    }
                }
                else {
                    applyPreRemoveCallbacks((Persistent) related);
                }
            }
        }
    }
}
