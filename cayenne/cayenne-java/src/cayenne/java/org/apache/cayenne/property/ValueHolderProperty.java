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

package org.apache.cayenne.property;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.util.PersistentObjectHolder;

/**
 * Provides access to a property implemented as a ValueHolder Field. This implementation
 * hides the fact of the ValueHolder existence. I.e. it never returns it from
 * 'readPropertyDirectly', returning held value instead.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ValueHolderProperty extends AbstractSingleObjectArcProperty {

    public ValueHolderProperty(ClassDescriptor owner, ClassDescriptor targetDescriptor,
            PropertyAccessor accessor, String reverseName) {
        super(owner, targetDescriptor, accessor, reverseName);
    }

    /**
     * Returns true if a property ValueHolder is not initialized or is itself a fault.
     */
    public boolean isFault(Object object) {
        ValueHolder holder = (ValueHolder) accessor.readPropertyDirectly(object);
        return holder == null || holder.isFault();
    }

    public Object readPropertyDirectly(Object object) throws PropertyAccessException {
        ValueHolder holder = (ValueHolder) accessor.readPropertyDirectly(object);

        // TODO: Andrus, 2/9/2006 ValueHolder will resolve an object in a call to
        // 'getValue'; this is inconsistent with 'readPropertyDirectly' contract
        return (holder != null) ? holder.getValueDirectly() : null;
    }

    public Object readProperty(Object object) throws PropertyAccessException {
        return ensureValueHolderSet(object).getValue();
    }

    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {

        ValueHolder holder = (ValueHolder) accessor.readPropertyDirectly(object);
        if (holder == null) {
            holder = createValueHolder(object);
            accessor.writePropertyDirectly(object, null, holder);
        }

        holder.setValueDirectly(newValue);
    }

    public void writeProperty(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {
        ensureValueHolderSet(object).setValueDirectly(newValue);
    }

    public void shallowMerge(Object from, Object to) throws PropertyAccessException {
        // noop
    }

    /**
     * Injects a ValueHolder in the object if it hasn't been done yet.
     */
    public void injectValueHolder(Object object) throws PropertyAccessException {
        ensureValueHolderSet(object);
    }

    /**
     * Checks that an object's ValueHolder field described by this property is set,
     * injecting a ValueHolder if needed.
     */
    protected ValueHolder ensureValueHolderSet(Object object)
            throws PropertyAccessException {

        ValueHolder holder = (ValueHolder) accessor.readPropertyDirectly(object);
        if (holder == null) {
            holder = createValueHolder(object);
            accessor.writePropertyDirectly(object, null, holder);
        }

        return holder;
    }

    /**
     * Creates a ValueHolder for an object. Default implementation requires that an object
     * implements Persistent interface.
     */
    protected ValueHolder createValueHolder(Object object) throws PropertyAccessException {
        if (!(object instanceof Persistent)) {

            throw new PropertyAccessException(
                    "ValueHolders for non-persistent objects are not supported.",
                    this,
                    object);
        }

        return new PersistentObjectHolder((Persistent) object, getName());
    }
}
