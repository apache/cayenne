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

package org.apache.cayenne.jpa.reflect;

import java.util.Collection;
import java.util.Map;

import org.apache.cayenne.Fault;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.BaseToManyProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyException;

abstract class JpaToManyProperty extends BaseToManyProperty {

    JpaToManyProperty(ClassDescriptor owner, ClassDescriptor targetDescriptor,
            Accessor accessor, String reverseName) {
        super(owner, targetDescriptor, accessor, reverseName);
    }

    @Override
    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyException {
        accessor.setValue(object, newValue);
    }

    /**
     * Overrides super to replace user-provided Collections with Cayenne-enabled
     * collections.
     */
    @Override
    protected ValueHolder ensureCollectionValueHolderSet(Object object)
            throws PropertyException {

        Object value = accessor.getValue(object);

        if (value == null || value instanceof Fault) {
            value = createCollectionValueHolder(object);
            accessor.setValue(object, value);
        }
        else if (!(value instanceof ValueHolder)) {

            if (value instanceof Collection || value instanceof Map) {
                ValueHolder valueHolder = createCollectionValueHolder(object);
                valueHolder.setValueDirectly(value);

                accessor.setValue(object, valueHolder);
                value = valueHolder;
            }
        }

        return (ValueHolder) value;
    }

    /**
     * Creates a List for an object. Expects an object to be an instance of Persistent.
     */
    protected ValueHolder createCollectionValueHolder(Object object)
            throws PropertyException {
        if (!(object instanceof Persistent)) {
            throw new PropertyException(
                    "ValueHolders for non-persistent objects are not supported.",
                    this,
                    object);
        }

        return createValueHolder((Persistent) object);
    }

    protected abstract ValueHolder createValueHolder(Persistent relationshipOwner);

    public boolean isFault(Object object) {
        Object target = accessor.getValue(object);
        return target == null
                || target instanceof Fault
                || ((ValueHolder) target).isFault();
    }

    public void invalidate(Object object) {
        ValueHolder list = (ValueHolder) readPropertyDirectly(object);
        if (list != null) {
            list.invalidate();
        }
    }
}
