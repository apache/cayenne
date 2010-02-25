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

import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.Property;
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

    private void deleteNew(Persistent object) {
        object.setPersistenceState(PersistenceState.TRANSIENT);
        processDeleteRules(object, PersistenceState.NEW);
        context.getGraphManager().unregisterNode(object.getObjectId());
    }

    private void deletePersistent(Persistent object) {
        int oldState = object.getPersistenceState();
        object.setPersistenceState(PersistenceState.DELETED);
        processDeleteRules(object, oldState);
        context.getGraphManager().nodeRemoved(object.getObjectId());
    }

    private void processDeleteRules(final Persistent object, final int oldState) {

        String entityName = object.getObjectId().getEntityName();
        final ObjEntity entity = context.getEntityResolver().getObjEntity(entityName);
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(
                entityName);

        descriptor.visitProperties(new PropertyVisitor() {

            public boolean visitToMany(ToManyProperty property) {
                ObjRelationship relationship = (ObjRelationship) entity
                        .getRelationship(property.getName());

                processRules(object, property, relationship.getDeleteRule(), oldState);
                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                ObjRelationship relationship = (ObjRelationship) entity
                        .getRelationship(property.getName());

                processRules(object, property, relationship.getDeleteRule(), oldState);
                return true;
            }

            public boolean visitAttribute(AttributeProperty property) {
                return true;
            }
        });
    }

    private void processRules(
            Persistent object,
            ArcProperty property,
            int deleteRule,
            int oldState) {

        if (deleteRule == DeleteRule.NO_ACTION) {
            return;
        }

        Collection<?> relatedObjects = relatedObjects(object, property);
        if (relatedObjects.isEmpty()) {
            return;
        }

        switch (deleteRule) {

            case DeleteRule.DENY:
                object.setPersistenceState(oldState);
                String message = relatedObjects.size() == 1
                        ? "1 related object"
                        : relatedObjects.size() + " related objects";
                throw new DeleteDenyException(object, property.getName(), message);

            case DeleteRule.NULLIFY:
                ArcProperty reverseArc = property.getComplimentaryReverseArc();

                if (reverseArc != null) {

                    if (reverseArc instanceof ToManyProperty) {
                        for (Object relatedObject : relatedObjects) {
                            ((ToManyProperty) reverseArc).removeTarget(
                                    relatedObject,
                                    object,
                                    true);
                        }
                    }
                    else {
                        for (Object relatedObject : relatedObjects) {
                            ((ToOneProperty) reverseArc).setTarget(
                                    relatedObject,
                                    null,
                                    true);
                        }
                    }
                }

                break;
            case DeleteRule.CASCADE:

                Iterator<?> iterator = relatedObjects.iterator();
                while (iterator.hasNext()) {
                    Persistent relatedObject = (Persistent) iterator.next();

                    // this action object is stateless, so we can use 'performDelete'
                    // recursively.
                    performDelete(relatedObject);
                }

                break;
            default:
                object.setPersistenceState(oldState);
                throw new CayenneRuntimeException("Invalid delete rule: " + deleteRule);
        }
    }

    private Collection<?> relatedObjects(Object object, Property property) {
        Object related = property.readProperty(object);

        if (related == null) {
            return Collections.EMPTY_LIST;
        }
        // return collections by copy, to allow removal of objects from the underlying
        // relationship inside the iterator
        else if (property instanceof ToManyProperty) {
            Collection<?> relatedCollection = (Collection<?>) related;
            return relatedCollection.isEmpty()
                    ? Collections.EMPTY_LIST
                    : new ArrayList<Object>(relatedCollection);
        }
        // TODO: andrus 11/21/2007 - ToManyMapProperty check
        else {
            return Collections.singleton(related);
        }
    }
}
