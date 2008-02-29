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
package org.apache.cayenne.util;

import java.io.Serializable;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.Property;

/**
 * A base implementation of a helper class to handle
 * {@link ObjectContext#propertyChanged(org.apache.cayenne.Persistent, String, Object, Object)}
 * processing on behalf of an ObjectContext.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public abstract class ObjectContextGraphAction implements Serializable {

    protected ObjectContext context;

    public ObjectContextGraphAction(ObjectContext context) {
        this.context = context;
    }

    /**
     * Handles property change in a Persistent object, routing to either
     * {@link #handleArcPropertyChange(Persistent, ArcProperty, Object, Object)} or
     * {@link #handleSimplePropertyChange(Persistent, String, Object, Object)}.
     */
    public void handlePropertyChange(
            Persistent object,
            String propertyName,
            Object oldValue,
            Object newValue) {

        // translate ObjectContext generic property change callback to GraphManager terms
        // (simple properties vs. relationships)

        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(
                object.getObjectId().getEntityName());
        Property property = descriptor.getProperty(propertyName);

        if (property instanceof ArcProperty) {
            handleArcPropertyChange(object, (ArcProperty) property, oldValue, newValue);
        }
        else {
            handleSimplePropertyChange(object, propertyName, oldValue, newValue);
        }
    }

    protected abstract void handleArcPropertyChange(
            Persistent object,
            ArcProperty property,
            Object oldValue,
            Object newValue);

    protected abstract void handleSimplePropertyChange(
            Persistent object,
            String propertyName,
            Object oldValue,
            Object newValue);

    /**
     * Changes object state to MODIFIED if needed, returning true if the change has
     * occured, false if not.
     */
    protected boolean markAsDirty(Persistent object) {
        if (object.getPersistenceState() == PersistenceState.COMMITTED) {
            object.setPersistenceState(PersistenceState.MODIFIED);
            return true;
        }

        return false;
    }
}
