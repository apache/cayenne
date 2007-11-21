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

import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.ObjectContextGraphAction;

/**
 * An action object that processes graph change calls from Persistent object. It handles
 * GraphManager notifications and bi-directional graph consistency.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class CayenneContextGraphAction extends ObjectContextGraphAction {

    ThreadLocal<Boolean> arcChangeInProcess;

    CayenneContextGraphAction(ObjectContext context) {
        super(context);
        this.arcChangeInProcess = new ThreadLocal<Boolean>();
        this.arcChangeInProcess.set(Boolean.FALSE);
    }

    protected void handleArcPropertyChange(
            Persistent object,
            ArcProperty property,
            Object oldValue,
            Object newValue) {

        if (isArchChangeInProcess()) {
            return;
        }

        // prevent reverse actions down the stack
        setArcChangeInProcess(true);

        try {
            if (oldValue instanceof Persistent) {
                context.getGraphManager().arcDeleted(
                        object.getObjectId(),
                        ((Persistent) oldValue).getObjectId(),
                        property.getName());

                unsetReverse(property, object, (Persistent) oldValue);
                markAsDirty(object);
            }

            if (newValue instanceof Persistent) {
                context.getGraphManager().arcCreated(
                        object.getObjectId(),
                        ((Persistent) newValue).getObjectId(),
                        property.getName());

                setReverse(property, object, (Persistent) newValue);
                markAsDirty(object);
            }
        }
        finally {
            setArcChangeInProcess(false);
        }
    }

    protected void handleSimplePropertyChange(
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

    /**
     * Returns true if the current thread is in the process of changing the arc property.
     * This method is used to prevent cycles when setting reverse relationships.
     */
    boolean isArchChangeInProcess() {
        return arcChangeInProcess.get();
    }

    /**
     * Sets the flag indicating whether the current thread is in the process of changing
     * the arc property. This method is used to prevent cycles when setting reverse
     * relationships.
     */
    void setArcChangeInProcess(boolean flag) {
        arcChangeInProcess.set(flag);
    }

    private void setReverse(
            ArcProperty property,
            final Persistent sourceObject,
            final Persistent targetObject) {

        ArcProperty reverseArc = property.getComplimentaryReverseArc();
        if (reverseArc != null) {
            reverseArc.visit(new PropertyVisitor() {

                public boolean visitToMany(ToManyProperty property) {
                    property.addTarget(targetObject, sourceObject, false);
                    return false;
                }

                public boolean visitToOne(ToOneProperty property) {
                    property.setTarget(targetObject, sourceObject, false);
                    return false;
                }

                public boolean visitAttribute(AttributeProperty property) {
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
