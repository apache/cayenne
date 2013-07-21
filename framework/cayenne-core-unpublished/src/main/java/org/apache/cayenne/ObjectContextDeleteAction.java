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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * A CayenneContext helper that processes object deletion.
 * 
 * @since 1.2
 */
class ObjectContextDeleteAction {

    private ObjectContext context;

    ObjectContextDeleteAction(ObjectContext context) {
        this.context = context;
    }

    boolean performDelete(Persistent object) throws DeleteDenyException {

        int oldState = object.getPersistenceState();

        if (oldState == PersistenceState.TRANSIENT
                || oldState == PersistenceState.DELETED) {
            // Drop out... especially in case of DELETED we might be about to get
            // into a horrible recursive loop due to CASCADE delete rules.
            // Assume that everything must have been done correctly already
            // and *don't* do it again
            return false;
        }

        if (object.getObjectContext() == null) {
            throw new CayenneRuntimeException(
                    "Attempt to delete unregistered non-TRANSIENT object: " + object);
        }

        if (object.getObjectContext() != context) {
            throw new CayenneRuntimeException(
                    "Attempt to delete object regsitered in a different ObjectContext. Object: "
                            + object
                            + ", context: "
                            + context);
        }

        // must resolve HOLLOW objects before delete... needed
        // to process relationships and optimistic locking...

        context.prepareForAccess(object, null, false);
        
        if (oldState == PersistenceState.NEW) {
            deleteNew(object);
        }
        else {
            deletePersistent(object);
        }

        return true;
    }

    private void deleteNew(Persistent object) throws DeleteDenyException {
        object.setPersistenceState(PersistenceState.TRANSIENT);
        processDeleteRules(object, PersistenceState.NEW);
        
        // if an object was NEW, we must throw it out
        context.getGraphManager().unregisterNode(object.getObjectId());
    }

    private void deletePersistent(Persistent object) throws DeleteDenyException {
        context.getEntityResolver().getCallbackRegistry().performCallbacks(
                LifecycleEvent.PRE_REMOVE,
                object);
        
        int oldState = object.getPersistenceState();
        object.setPersistenceState(PersistenceState.DELETED);
        processDeleteRules(object, oldState);
        context.getGraphManager().nodeRemoved(object.getObjectId());
    }

    private Collection toCollection(Object object) {

        if (object == null) {
            return Collections.EMPTY_LIST;
        }

        // create copies of collections to avoid iterator exceptions
        if (object instanceof Collection) {
            return new ArrayList((Collection) object);
        }
        else if (object instanceof Map) {
            return new ArrayList(((Map) object).values());
        }
        else {
            return Collections.singleton(object);
        }
    }

    private void processDeleteRules(final Persistent object, int oldState)
            throws DeleteDenyException {

        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(
                object.getObjectId().getEntityName());

        for (final ObjRelationship relationship : descriptor.getEntity().getRelationships()) {

            boolean processFlattened = relationship.isFlattened()
                    && relationship.isToDependentEntity()
                    && !relationship.isReadOnly();

            // first check for no action... bail out if no flattened processing is needed
            if (relationship.getDeleteRule() == DeleteRule.NO_ACTION && !processFlattened) {
                continue;
            }

            ArcProperty property = (ArcProperty) descriptor.getProperty(relationship
                    .getName());
            Collection relatedObjects = toCollection(property.readProperty(object));

            // no related object, bail out
            if (relatedObjects.size() == 0) {
                continue;
            }

            // process DENY rule first...
            if (relationship.getDeleteRule() == DeleteRule.DENY) {
                object.setPersistenceState(oldState);

                String message = relatedObjects.size() == 1
                        ? "1 related object"
                        : relatedObjects.size() + " related objects";
                throw new DeleteDenyException(object, relationship.getName(), message);
            }

            // process flattened with dependent join tables...
            // joins must be removed even if they are non-existent or ignored in the
            // object graph
            if (processFlattened) {
                Iterator iterator = relatedObjects.iterator();
                while (iterator.hasNext()) {
                    Persistent relatedObject = (Persistent) iterator.next();
                    context.getGraphManager().arcDeleted(object.getObjectId(), relatedObject
                            .getObjectId(), relationship.getName());
                }
            }

            // process remaining rules
            switch (relationship.getDeleteRule()) {
                case DeleteRule.NO_ACTION:
                    break;
                case DeleteRule.NULLIFY:
                    ArcProperty reverseArc = property.getComplimentaryReverseArc();

                    if (reverseArc == null) {
                        // nothing we can do here
                        break;
                    }

                    final Collection finalRelatedObjects = relatedObjects;

                    reverseArc.visit(new PropertyVisitor() {

                        public boolean visitAttribute(AttributeProperty property) {
                            return false;
                        }

                        public boolean visitToMany(ToManyProperty property) {
                            Iterator iterator = finalRelatedObjects.iterator();
                            while (iterator.hasNext()) {
                                Object relatedObject = iterator.next();
                                property.removeTarget(relatedObject, object, true);
                            }

                            return false;
                        }

                        public boolean visitToOne(ToOneProperty property) {
                            // Inverse is to-one - find all related objects and
                            // nullify the reverse relationship
                            Iterator iterator = finalRelatedObjects.iterator();
                            while (iterator.hasNext()) {
                                Object relatedObject = iterator.next();
                                property.setTarget(relatedObject, null, true);
                            }
                            return false;
                        }
                    });

                    break;
                case DeleteRule.CASCADE:
                    // Delete all related objects
                    Iterator iterator = relatedObjects.iterator();
                    while (iterator.hasNext()) {
                        Persistent relatedObject = (Persistent) iterator.next();
                        performDelete(relatedObject);
                    }

                    break;
                default:
                    object.setPersistenceState(oldState);
                    throw new CayenneRuntimeException("Invalid delete rule "
                            + relationship.getDeleteRule());
            }
        }
    }
}
