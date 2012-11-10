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

import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.FaultFailureException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;

/**
 * A ValueHolder implementation that holds a single Persistent object related to an object
 * used to initialize PersistentObjectHolder. Value is resolved on first access.
 * 
 * @since 1.2
 */
public class PersistentObjectHolder extends RelationshipFault implements ValueHolder {

    protected boolean fault;
    protected Object value;

    // exists for the benefit of manual serialization schemes such as the one in Hessian.
    @SuppressWarnings("unused")
    private PersistentObjectHolder() {
        fault = true;
    }

    public PersistentObjectHolder(Persistent relationshipOwner, String relationshipName) {
        super(relationshipOwner, relationshipName);
        fault = !isTransientParent();
    }

    /**
     * Returns true if this holder is not resolved, meaning its object is not yet known.
     */
    public boolean isFault() {
        return fault;
    }

    public void invalidate() {
        fault = true;
        value = null;
    }

    /**
     * Returns a value resolving it via a query on the first call to this method.
     */
    public Object getValue() throws CayenneRuntimeException {

        if (fault) {
            resolve();
        }

        return value;
    }
    
    public Object getValueDirectly() throws CayenneRuntimeException {
        return value;
    }

    /**
     * Sets an object value, marking this ValueHolder as resolved.
     */
    public synchronized Object setValue(Object value) throws CayenneRuntimeException {

        if (fault) {
            resolve();
        }

        Object oldValue = setValueDirectly(value);
        if (oldValue != value && relationshipOwner.getObjectContext() != null) {
            relationshipOwner.getObjectContext().propertyChanged(relationshipOwner, relationshipName, oldValue, value);
    
            if (oldValue instanceof Persistent) {
                Util.unsetReverse(relationshipOwner, relationshipName, (Persistent) oldValue);
            }
            if (value instanceof Persistent) {
                Util.setReverse(relationshipOwner, relationshipName, (Persistent) value);
            }
        }
        return oldValue;
    }

    public Object setValueDirectly(Object value) throws CayenneRuntimeException {

        // must obtain the value from the local context
        if (value instanceof Persistent) {
            value = connect((Persistent) value);
        }

        Object oldValue = this.value;

        this.value = value;
        this.fault = false;

        return oldValue;
    }

    /**
     * Returns an object that should be stored as a value in this ValueHolder, ensuring
     * that it is registered with the same context.
     */
    protected Object connect(Persistent persistent) {

        if (persistent == null) {
            return null;
        }

        if (relationshipOwner.getObjectContext() != persistent.getObjectContext()) {
            throw new CayenneRuntimeException(
                    "Cannot set object as destination of relationship "
                            + relationshipName
                            + " because it is in a different ObjectContext");
        }

        return persistent;
    }

    /**
     * Reads an object from the database.
     */
    protected synchronized void resolve() {
        if (!fault) {
            return;
        }

        // TODO: should build a HOLLOW object instead of running a query if relationship
        // is required and thus expected to be not null.

        List objects = resolveFromDB();

        if (objects.size() == 0) {
            this.value = null;
        }
        else if (objects.size() == 1) {
            this.value = objects.get(0);
        }
        else {
            throw new FaultFailureException(
                    "Expected either no objects or a single object, instead fault query resolved to "
                            + objects.size()
                            + " objects.");
        }

        fault = false;
    }
    
    @Override
    protected void mergeLocalChanges(List resolved) {
        // noop
    }
}
