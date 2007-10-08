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

import org.apache.cayenne.property.ArcProperty;
import org.apache.cayenne.property.ClassDescriptor;
import org.apache.cayenne.property.CollectionProperty;
import org.apache.cayenne.property.Property;
import org.apache.cayenne.property.PropertyVisitor;
import org.apache.cayenne.property.SingleObjectArcProperty;

/**
 * An action object that processes graph change calls from Persistent object. It handles
 * GraphManager notifications and bi-directional graph consistency.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class CayenneContextGraphAction {

    CayenneContext context;
    ThreadLocal arcChangeInProcess;

    CayenneContextGraphAction(CayenneContext context) {
        this.context = context;
        this.arcChangeInProcess = new ThreadLocal();
    }

    /**
     * Handles property change in a Peristent object.
     */
    void handlePropertyChange(
            Persistent object,
            String propertyName,
            Object oldValue,
            Object newValue) {

        // translate ObjectContext generic property change callback to GraphManager terms
        // (simple properties vs. relationships)

        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(
                object.getObjectId().getEntityName());
        Property property = descriptor.getProperty(propertyName);

        // relationship property
        if (property instanceof ArcProperty) {

            try {
                handleArcPropertyChange(
                        object,
                        (ArcProperty) property,
                        oldValue,
                        newValue);
            }
            finally {
                setArcChangeInProcess(false);
            }
        }
        // simple property
        else {
            handleSimplePropertyChange(object, propertyName, oldValue, newValue);
        }
    }

    void handleArcPropertyChange(
            Persistent object,
            ArcProperty property,
            Object oldValue,
            Object newValue) {

        boolean arcChangeInProcess = isArchChangeInProcess();

        // prevent reverse actions down the stack
        setArcChangeInProcess(true);

        if (oldValue instanceof Persistent) {
            context.getGraphManager().arcDeleted(
                    object.getObjectId(),
                    ((Persistent) oldValue).getObjectId(),
                    property.getName());

            if (!arcChangeInProcess) {
                unsetReverse(property, object, (Persistent) oldValue);
            }

            markAsDirty(object);
        }

        if (newValue instanceof Persistent) {
            context.getGraphManager().arcCreated(
                    object.getObjectId(),
                    ((Persistent) newValue).getObjectId(),
                    property.getName());

            if (!arcChangeInProcess) {
                setReverse(property, object, (Persistent) newValue);
            }

            markAsDirty(object);
        }
    }

    void handleSimplePropertyChange(
            Persistent object,
            String propertyName,
            Object oldValue,
            Object newValue) {
        context.getGraphManager().nodePropertyChanged(
                object.getObjectId(),
                propertyName,
                oldValue,
                newValue);
        markAsDirty(object);
    }

    boolean isArchChangeInProcess() {
        return arcChangeInProcess.get() != null;
    }

    void setArcChangeInProcess(boolean flag) {
        arcChangeInProcess.set(flag ? Boolean.TRUE : null);
    }

    /**
     * Changes object state to MODIFIED if needed.
     */
    private void markAsDirty(Persistent object) {
        if (object.getPersistenceState() == PersistenceState.COMMITTED) {
            object.setPersistenceState(PersistenceState.MODIFIED);
        }
    }

    private void setReverse(
            ArcProperty property,
            final Persistent sourceObject,
            final Persistent targetObject) {

        ArcProperty reverseArc = property.getComplimentaryReverseArc();
        if (reverseArc != null) {
            reverseArc.visit(new PropertyVisitor() {

                public boolean visitCollectionArc(CollectionProperty property) {
                    property.addTarget(targetObject, sourceObject, false);
                    return false;
                }

                public boolean visitSingleObjectArc(SingleObjectArcProperty property) {
                    property.setTarget(targetObject, sourceObject, false);
                    return false;
                }

                public boolean visitProperty(Property property) {
                    return false;
                }

            });

            context.getGraphManager().arcCreated(
                    targetObject.getObjectId(),
                    sourceObject.getObjectId(),
                    reverseArc.getName());

            markAsDirty(targetObject);
        }
    }

    private void unsetReverse(
            ArcProperty property,
            Persistent sourceObject,
            Persistent targetObject) {

        ArcProperty reverseArc = property.getComplimentaryReverseArc();
        if (reverseArc != null) {
            reverseArc.writePropertyDirectly(targetObject, sourceObject, null);

            context.getGraphManager().arcDeleted(
                    targetObject.getObjectId(),
                    sourceObject.getObjectId(),
                    reverseArc.getName());

            markAsDirty(targetObject);
        }
    }
}
