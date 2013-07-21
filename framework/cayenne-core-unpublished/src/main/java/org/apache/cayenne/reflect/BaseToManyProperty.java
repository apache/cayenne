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

package org.apache.cayenne.reflect;

import java.util.Collection;

import org.apache.cayenne.Fault;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.util.PersistentObjectCollection;

/**
 * A generic superclass of CollectionProperty implementations.
 * 
 * @since 1.2
 */
public abstract class BaseToManyProperty extends BaseArcProperty implements
        ToManyProperty {

    public BaseToManyProperty(ClassDescriptor owner, ClassDescriptor targetDescriptor,
            Accessor accessor, String reverseName) {
        super(owner, targetDescriptor, accessor, reverseName);
    }

    @Override
    public Object readProperty(Object object) throws PropertyException {
        return ensureCollectionValueHolderSet(object);
    }

    /**
     * Wraps list in a value holder that performs lazy faulting.
     */
    @Override
    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyException {

        if (newValue instanceof Fault) {
            super.writePropertyDirectly(object, null, newValue);
        }
        else {
            // must resolve value holder...
            ValueHolder holder = (ValueHolder) readProperty(object);
            holder.setValueDirectly(newValue);
        }
    }

    public void addTarget(Object source, Object target, boolean setReverse) {
        if (target == null) {
            throw new NullPointerException("Attempt to add null object.");
        }

        // TODO, Andrus, 2/9/2006 - CayenneDataObject differences:
        // * invokes "willConnect"
        // * has a callback to ObjectStore to handle flattened
        // * has a callback to ObjectStore to retain snapshot
        // * changes object state to modified

        // Now do the rest of the normal handling (regardless of whether it was
        // flattened or not)
        Collection<Object> collection = (Collection<Object>) readProperty(source);
        collection.add(target);

        if (setReverse) {
            setReverse(source, null, target);
        }
    }
    
    public void addTargetDirectly(Object source, Object target) throws PropertyException {
        ((PersistentObjectCollection) readProperty(source)).addDirectly(target);
    }
    
    public void removeTargetDirectly(Object source, Object target) throws PropertyException {
        ((PersistentObjectCollection) readProperty(source)).removeDirectly(target);
    }

    public void removeTarget(Object source, Object target, boolean setReverse) {

        // TODO, Andrus, 2/9/2006 - CayenneDataObject differences:
        // * has a callback to ObjectStore to handle flattened
        // * changes object state to modified

        // Now do the rest of the normal handling (regardless of whether it was
        // flattened or not)
        Collection<Object> collection = (Collection<Object>) readProperty(source);
        collection.remove(target);

        if (target != null && setReverse) {
            setReverse(source, target, null);
        }
    }

    @Override
    public boolean visit(PropertyVisitor visitor) {
        return visitor.visitToMany(this);
    }

    /**
     * Injects a List in the object if it hasn't been done yet.
     */
    @Override
    public void injectValueHolder(Object object) throws PropertyException {
        ensureCollectionValueHolderSet(object);
    }

    /**
     * Checks that an object's List field described by this property is set, injecting a
     * List if needed.
     */
    protected ValueHolder ensureCollectionValueHolderSet(Object object)
            throws PropertyException {

        Object value = accessor.getValue(object);

        if (value == null || value instanceof Fault) {
            value = createCollectionValueHolder(object);
            accessor.setValue(object, value);
        }

        return (ValueHolder) value;
    }

    /**
     * Creates a Collection for an object.
     */
    protected abstract ValueHolder createCollectionValueHolder(Object object)
            throws PropertyException;
    
}
