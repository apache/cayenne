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
package org.apache.cayenne.reflect.pojo;

import org.apache.cayenne.Fault;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.BaseToOneProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyException;

/**
 * A property descriptor for the to-one relationship property of an enhanced pojo. Assumes
 * a class in question has a field called "$cay_faultResolved_propertyName" that stores a
 * boolean flag indicating whether the property in question is not yet resolved.
 * 
 * @since 3.0
 */
class EnhancedPojoToOneProperty extends BaseToOneProperty {

    protected EnhancedPojoPropertyFaultHandler faultHandler;
    protected Fault fault;

    EnhancedPojoToOneProperty(ClassDescriptor owner, ClassDescriptor targetDescriptor,
            Accessor accessor, String reverseName, Fault fault) {
        super(owner, targetDescriptor, accessor, reverseName);
        this.faultHandler = new EnhancedPojoPropertyFaultHandler(
                owner.getObjectClass(),
                getName());
        this.fault = fault;
    }

    @Override
    public boolean isFault(Object source) {
        return faultHandler.isFaultProperty(source);
    }

    public void invalidate(Object object) {
        faultHandler.setFaultProperty(object, true);
    }

    void resolveFault(Object object) {
        if (isFault(object)) {
            Object target = fault.resolveFault((Persistent) object, getName());
            writePropertyDirectly(object, null, target);
            faultHandler.setFaultProperty(object, false);
        }
    }

    @Override
    public Object readProperty(Object object) throws PropertyException {
        resolveFault(object);
        return super.readProperty(object);
    }
}
