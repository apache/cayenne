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

import java.lang.reflect.Field;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.util.Util;

/**
 * A PropertyAccessor that performs direct Field access.
 * 
 * @since 1.2
 */
public class FieldAccessor implements Accessor {

    protected String propertyName;
    protected Field field;
    protected Object nullValue;

    public FieldAccessor(Class<?> objectClass, String propertyName, Class<?> propertyType) {
        // sanity check
        if (objectClass == null) {
            throw new IllegalArgumentException("Null objectClass");
        }

        if (propertyName == null) {
            throw new IllegalArgumentException("Null propertyName");
        }

        this.propertyName = propertyName;
        this.field = prepareField(objectClass, propertyName, propertyType);
        this.nullValue = PropertyUtils.defaultNullValueForType(field.getType());
    }

    public String getName() {
        return propertyName;
    }

    public Object getValue(Object object) throws PropertyException {
        try {
            return field.get(object);
        }
        catch (Throwable th) {
            throw new PropertyException(
                    "Error reading field: " + field.getName(),
                    this,
                    object,
                    th);
        }
    }

    /**
     * @since 3.0
     */
    public void setValue(Object object, Object newValue) throws PropertyException {
        // this will take care of primitives.
        if (newValue == null) {
            newValue = this.nullValue;
        }

        try {
            field.set(object, newValue);
        }
        catch (Throwable th) {
            throw new PropertyException(
                    "Error writing field: " + field.getName(),
                    this,
                    object,
                    th);
        }
    }

    /**
     * Finds a field for the property, ensuring that direct access via reflection is
     * possible.
     */
    protected Field prepareField(
            Class<?> beanClass,
            String propertyName,
            Class<?> propertyType) {
        Field field;

        // locate field
        try {
            field = lookupFieldInHierarchy(beanClass, propertyName);
        }
        catch (SecurityException e) {
            throw new CayenneRuntimeException("Error accessing field '"
                    + propertyName
                    + "' in class: "
                    + beanClass.getName(), e);
        }
        catch (NoSuchFieldException e) {
            throw new CayenneRuntimeException("No field '"
                    + propertyName
                    + "' is defined in class: "
                    + beanClass.getName(), e);
        }

        // set accessability
        if (!Util.isAccessible(field)) {
            field.setAccessible(true);
        }

        if (propertyType != null) {

            // check that the field is of expected class...
            if (!field.getType().isAssignableFrom(propertyType)) {

                // allow primitive to object conversions...
                if (!PropertyUtils.normalizeType(field.getType()).isAssignableFrom(
                        PropertyUtils.normalizeType(propertyType))) {
                    throw new CayenneRuntimeException("Expected property type '"
                            + propertyType.getName()
                            + "', got '"
                            + field.getType().getName()
                            + "'. Property: '"
                            + beanClass.getName()
                            + "'.'"
                            + propertyName + "'");
                }
            }
        }

        return field;
    }

    /**
     * Recursively looks for a named field in a class hierarchy.
     */
    protected Field lookupFieldInHierarchy(Class<?> beanClass, String fieldName)
            throws SecurityException, NoSuchFieldException {

        // TODO: support property names following other common naming patterns, such as
        // "_propertyName"

        try {
            return beanClass.getDeclaredField(fieldName);
        }
        catch (NoSuchFieldException e) {

            Class<?> superClass = beanClass.getSuperclass();
            if (superClass == null || superClass.getName().equals(Object.class.getName())) {
                throw e;
            }

            return lookupFieldInHierarchy(superClass, fieldName);
        }
    }

}
