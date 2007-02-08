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

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DeleteDenyException;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * Helper class that implements DataObject deletion strategy.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
class DataContextDeleteAction {

    DataContext dataContext;

    DataContextDeleteAction(DataContext context) {
        this.dataContext = context;
    }

    /**
     * Deletes internal DataObject from its DataContext, processing delete rules.
     */
    boolean performDelete(Persistent object) throws DeleteDenyException {
        int oldState = object.getPersistenceState();
        if (oldState == PersistenceState.DELETED
                || oldState == PersistenceState.TRANSIENT) {

            // Drop out... especially in case of DELETED we might be about to get
            // into a horrible recursive loop due to CASCADE delete rules.
            // Assume that everything must have been done correctly already
            // and *don't* do it again
            return false;
        }

        // TODO: Andrus, 1/14/2006 - temp hack
        if (!(object instanceof DataObject)) {
            throw new IllegalArgumentException(
                    this
                            + ": this implementation of ObjectContext only supports full DataObjects. Object "
                            + object
                            + " is not supported.");
        }

        DataObject dataObject = (DataObject) object;

        if (dataObject.getDataContext() == null) {
            throw new CayenneRuntimeException(
                    "Attempt to delete unregistered non-TRANSIENT object: " + object);
        }

        if (dataObject.getDataContext() != dataContext) {
            throw new CayenneRuntimeException(
                    "Attempt to delete object regsitered in a different DataContext. Object: "
                            + object
                            + ", data context: "
                            + dataContext);
        }

        // must resolve HOLLOW objects before delete... needed
        // to process relationships and optimistic locking...

        dataContext.prepareForAccess(dataObject, null);

        if (oldState == PersistenceState.NEW) {
            deleteNew(dataObject, oldState);
        }
        else {
            deletePersistent(dataObject, oldState);
        }

        return true;
    }

    private void deletePersistent(DataObject object, int oldState)
            throws DeleteDenyException {

        dataContext.getObjectStore().recordObjectDeleted(object);
        processDeleteRules(object, oldState);
    }

    private void deleteNew(DataObject object, int oldState) throws DeleteDenyException {
        object.setPersistenceState(PersistenceState.TRANSIENT);
        processDeleteRules(object, oldState);

        // if an object was NEW, we must throw it out of the ObjectStore

        dataContext.getObjectStore().objectsUnregistered(
                Collections.singletonList(object));
        object.setDataContext(null);
    }

    private void processDeleteRules(DataObject object, int oldState)
            throws DeleteDenyException {
        ObjEntity entity = dataContext.getEntityResolver().lookupObjEntity(object);
        Iterator it = entity.getRelationships().iterator();
        while (it.hasNext()) {
            ObjRelationship relationship = (ObjRelationship) it.next();

            boolean processFlattened = relationship.isFlattened()
                    && relationship.isToDependentEntity();

            // first check for no action... bail out if no flattened processing is needed
            if (relationship.getDeleteRule() == DeleteRule.NO_ACTION && !processFlattened) {
                continue;
            }

            List relatedObjects = Collections.EMPTY_LIST;
            if (relationship.isToMany()) {

                List toMany = (List) object.readNestedProperty(relationship.getName());

                if (toMany.size() > 0) {
                    // Get a copy of the list so that deleting objects doesn't
                    // result in concurrent modification exceptions
                    relatedObjects = new ArrayList(toMany);
                }
            }
            else {
                Object relatedObject = object.readNestedProperty(relationship.getName());

                if (relatedObject != null) {
                    relatedObjects = Collections.singletonList(relatedObject);
                }
            }

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
                ObjectStore objectStore = dataContext.getObjectStore();
                Iterator iterator = relatedObjects.iterator();
                while (iterator.hasNext()) {
                    DataObject relatedObject = (DataObject) iterator.next();
                    objectStore.recordArcDeleted(
                            object,
                            relatedObject.getObjectId(),
                            relationship.getName());
                }
            }

            // process remaining rules
            switch (relationship.getDeleteRule()) {
                case DeleteRule.NO_ACTION:
                    break;
                case DeleteRule.NULLIFY:
                    ObjRelationship inverseRelationship = relationship
                            .getReverseRelationship();

                    if (inverseRelationship == null) {
                        // nothing we can do here
                        break;
                    }

                    if (inverseRelationship.isToMany()) {
                        Iterator iterator = relatedObjects.iterator();
                        while (iterator.hasNext()) {
                            DataObject relatedObject = (DataObject) iterator.next();
                            relatedObject.removeToManyTarget(inverseRelationship
                                    .getName(), object, true);
                        }
                    }
                    else {
                        // Inverse is to-one - find all related objects and
                        // nullify the reverse relationship
                        Iterator iterator = relatedObjects.iterator();
                        while (iterator.hasNext()) {
                            DataObject relatedObject = (DataObject) iterator.next();
                            relatedObject.setToOneTarget(
                                    inverseRelationship.getName(),
                                    null,
                                    true);
                        }
                    }

                    break;
                case DeleteRule.CASCADE:
                    // Delete all related objects
                    Iterator iterator = relatedObjects.iterator();
                    while (iterator.hasNext()) {
                        DataObject relatedObject = (DataObject) iterator.next();
                        new DataContextDeleteAction(this.dataContext)
                                .performDelete(relatedObject);
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
