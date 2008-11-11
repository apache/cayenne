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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * A property accessor that uses set/get methods following JavaBean naming conventions.
 * 
 * @since 1.2
 */
public class BeanAccessor implements Accessor {

    protected String propertyName;
    protected Method readMethod;
    protected Method writeMethod;
    protected Object nullValue;

    public BeanAccessor(Class<?> objectClass, String propertyName, Class<?> propertyType) {
        if (objectClass == null) {
            throw new IllegalArgumentException("Null objectClass");
        }

        if (propertyName == null) {
            throw new IllegalArgumentException("Null propertyName");
        }

        this.propertyName = propertyName;
        this.nullValue = PropertyUtils.defaultNullValueForType(propertyType);

        try {
            PropertyDescriptor descriptor = new PropertyDescriptor(
                    propertyName,
                    objectClass);

            this.readMethod = descriptor.getReadMethod();
            this.writeMethod = descriptor.getWriteMethod();
        }
        catch (IntrospectionException e) {
            throw new PropertyException("Invalid bean property: " + propertyName, this, e);
        }
    }

    public String getName() {
        return propertyName;
    }

    /**
     * @since 3.0
     */
    public Object getValue(Object object) throws PropertyException {
        if (readMethod == null) {
            throw new PropertyException(
                    "Property '" + propertyName + "' is not readable",
                    this,
                    object);
        }

        try {
            return readMethod.invoke(object, (Object[]) null);
        }
        catch (Throwable th) {
            throw new PropertyException(
                    "Error reading property: " + propertyName,
                    this,
                    object,
                    th);
        }
    }

    /**
     * @since 3.0
     */
    public void setValue(Object object, Object newValue) throws PropertyException {

        if (writeMethod == null) {
            throw new PropertyException(
                    "Property '" + propertyName + "' is not writable",
                    this,
                    object);
        }

        // this will take care of primitives.
        if (newValue == null) {
            newValue = this.nullValue;
        }

        try {
            writeMethod.invoke(object, newValue);
        }
        catch (Throwable th) {
            throw new PropertyException(
                    "Error reading property: " + propertyName,
                    this,
                    object,
                    th);
        }
    }
}
