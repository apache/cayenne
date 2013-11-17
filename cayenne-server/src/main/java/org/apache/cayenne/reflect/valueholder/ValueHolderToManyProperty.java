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

import org.apache.cayenne.Fault;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.BaseToManyProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyException;

/**
 * @since 3.0
 */
abstract class ValueHolderToManyProperty extends BaseToManyProperty {

    ValueHolderToManyProperty(ClassDescriptor owner, ClassDescriptor targetDescriptor,
            Accessor accessor, String reverseName) {
        super(owner, targetDescriptor, accessor, reverseName);
    }

    @Override
    protected abstract ValueHolder createCollectionValueHolder(Object object)
            throws PropertyException;

    @Override
    public boolean isFault(Object source) {
        Object target = accessor.getValue(source);
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
