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

package org.apache.cayenne.reflect.valueholder;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.BaseToOneProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.util.PersistentObjectHolder;

/**
 * Provides access to a property implemented as a {@link ValueHolder} Field. This
 * implementation hides the fact of the ValueHolder existence. I.e. it never returns it
 * from 'readPropertyDirectly', returning held value instead.
 * 
 * @since 3.0
 */
class ValueHolderProperty extends BaseToOneProperty {

    ValueHolderProperty(ClassDescriptor owner, ClassDescriptor targetDescriptor,
            Accessor accessor, String reverseName) {
        super(owner, targetDescriptor, accessor, reverseName);
    }

    /**
     * Returns true if a property ValueHolder is not initialized or is itself a fault.
     */
    @Override
    public boolean isFault(Object object) {
        ValueHolder holder = (ValueHolder) accessor.getValue(object);
        return holder == null || holder.isFault();
    }

    public void invalidate(Object object) {
        ValueHolder holder = (ValueHolder) accessor.getValue(object);
        if (holder != null && !holder.isFault()) {
            holder.invalidate();
        }
    }

    @Override
    public Object readPropertyDirectly(Object object) throws PropertyException {
        ValueHolder holder = (ValueHolder) accessor.getValue(object);

        // TODO: Andrus, 2/9/2006 ValueHolder will resolve an object in a call to
        // 'getValue'; this is inconsistent with 'readPropertyDirectly' contract
        return (holder != null) ? holder.getValueDirectly() : null;
    }

    @Override
    public Object readProperty(Object object) throws PropertyException {
        return ensureValueHolderSet(object).getValue();
    }

    @Override
    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyException {

        ValueHolder holder = (ValueHolder) accessor.getValue(object);
        if (holder == null) {
            holder = createValueHolder(object);
            accessor.setValue(object, holder);
        }

        holder.setValueDirectly(newValue);
    }

    @Override
    public void writeProperty(Object object, Object oldValue, Object newValue)
            throws PropertyException {
        ensureValueHolderSet(object).setValueDirectly(newValue);
    }

    /**
     * Injects a ValueHolder in the object if it hasn't been done yet.
     */
    @Override
    public void injectValueHolder(Object object) throws PropertyException {
        ensureValueHolderSet(object);
    }

    /**
     * Checks that an object's ValueHolder field described by this property is set,
     * injecting a ValueHolder if needed.
     */
    protected ValueHolder ensureValueHolderSet(Object object) throws PropertyException {

        ValueHolder holder = (ValueHolder) accessor.getValue(object);
        if (holder == null) {
            holder = createValueHolder(object);
            accessor.setValue(object, holder);
        }

        return holder;
    }

    /**
     * Creates a ValueHolder for an object. Default implementation requires that an object
     * implements Persistent interface.
     */
    protected ValueHolder createValueHolder(Object object) throws PropertyException {
        if (!(object instanceof Persistent)) {

            throw new PropertyException(
                    "ValueHolders for non-persistent objects are not supported.",
                    this,
                    object);
        }

        return new PersistentObjectHolder((Persistent) object, getName());
    }
}
