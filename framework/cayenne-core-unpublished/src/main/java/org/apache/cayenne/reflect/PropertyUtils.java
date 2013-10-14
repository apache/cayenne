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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.util.Util;

/**
 * Utility methods to quickly access object properties. This class supports simple and
 * nested properties and also conversion of property values to match property type. No
 * converter customization is provided yet, so only basic converters for Strings, Numbers
 * and primitives are available.
 * 
 * @since 1.2
 */
public class PropertyUtils {

    /**
     * Compiles an accessor that can be used for fast access for the nested property of
     * the objects of a given class.
     * 
     * @since 3.0
     */
    public static Accessor createAccessor(Class<?> objectClass, String nestedPropertyName) {
        if (objectClass == null) {
            throw new IllegalArgumentException("Null class.");
        }

        if (Util.isEmptyString(nestedPropertyName)) {
            throw new IllegalArgumentException("Null or empty property name.");
        }

        StringTokenizer path = new StringTokenizer(
                nestedPropertyName,
                Entity.PATH_SEPARATOR);

        if (path.countTokens() == 1) {
            return new BeanAccessor(objectClass, nestedPropertyName, null);
        }

        NestedBeanAccessor accessor = new NestedBeanAccessor(nestedPropertyName);
        while (path.hasMoreTokens()) {
            String token = path.nextToken();
            accessor.addAccessor(new BeanAccessor(objectClass, token, null));
        }

        return accessor;
    }

    /**
     * Returns object property using JavaBean-compatible introspection with one addition -
     * a property can be a dot-separated property name path.
     */
    public static Object getProperty(Object object, String nestedPropertyName)
            throws CayenneRuntimeException {

        if (object == null) {
            throw new IllegalArgumentException("Null object.");
        }

        if (Util.isEmptyString(nestedPropertyName)) {
            throw new IllegalArgumentException("Null or empty property name.");
        }

        StringTokenizer path = new StringTokenizer(
                nestedPropertyName,
                Entity.PATH_SEPARATOR);
        int len = path.countTokens();

        Object value = object;
        String pathSegment = null;

        try {
            for (int i = 1; i <= len; i++) {
                pathSegment = path.nextToken();

                if (value == null) {
                    // null value in the middle....
                    throw new UnresolvablePathException(
                            "Null value in the middle of the path, failed on "
                                    + nestedPropertyName
                                    + " from "
                                    + object);
                }

                value = getSimpleProperty(value, pathSegment);
            }

            return value;
        }
        catch (Exception e) {
            String objectType = value != null ? value.getClass().getName() : "<null>";
            throw new CayenneRuntimeException("Error reading property segment '"
                    + pathSegment
                    + "' in path '"
                    + nestedPropertyName
                    + "' for type "
                    + objectType, e);
        }
    }

    /**
     * Sets object property using JavaBean-compatible introspection with one addition - a
     * property can be a dot-separated property name path. Before setting a value attempts
     * to convert it to a type compatible with the object property. Automatic conversion
     * is supported between strings and basic types like numbers or primitives.
     */
    public static void setProperty(Object object, String nestedPropertyName, Object value)
            throws CayenneRuntimeException {

        if (object == null) {
            throw new IllegalArgumentException("Null object.");
        }

        if (Util.isEmptyString(nestedPropertyName)) {
            throw new IllegalArgumentException("Null or invalid property name.");
        }

        int dot = nestedPropertyName.lastIndexOf(Entity.PATH_SEPARATOR);
        String lastSegment;
        if (dot > 0) {
            lastSegment = nestedPropertyName.substring(dot + 1);
            String pathSegment = nestedPropertyName.substring(0, dot);
            Object intermediateObject = getProperty(object, pathSegment);

            if (intermediateObject == null) {
                throw new UnresolvablePathException(
                        "Null value in the middle of the path, failed on "
                                + pathSegment
                                + " from "
                                + object);
            } else {
            	object = intermediateObject;
            }
        }
        else {
            lastSegment = nestedPropertyName;
        }

        try {
            setSimpleProperty(object, lastSegment, value);
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error setting property segment '"
                    + lastSegment
                    + "' in path '"
                    + nestedPropertyName
                    + "'"
                    + " to value '"
                    + value
                    + "' for object '"
                    + object
                    + "'", e);
        }

    }

    static Object getSimpleProperty(Object object, String pathSegment)
            throws IntrospectionException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {

        PropertyDescriptor descriptor = getPropertyDescriptor(
                object.getClass(),
                pathSegment);

        if (descriptor != null) {
            Method reader = descriptor.getReadMethod();

            if (reader == null) {
                throw new IntrospectionException("Unreadable property '"
                        + pathSegment
                        + "'");
            }

            return reader.invoke(object, (Object[]) null);
        }
        // note that Map has two traditional bean properties - 'empty' and 'class', so
        // do a check here only after descriptor lookup failed.
        else if (object instanceof Map) {
            return ((Map<?, ?>) object).get(pathSegment);
        }
        else {
            throw new IntrospectionException("No property '"
                    + pathSegment
                    + "' found in class "
                    + object.getClass().getName());
        }
    }

    static void setSimpleProperty(Object object, String pathSegment, Object value)
            throws IntrospectionException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {

        PropertyDescriptor descriptor = getPropertyDescriptor(
                object.getClass(),
                pathSegment);

        if (descriptor != null) {
            Method writer = descriptor.getWriteMethod();

            if (writer == null) {
                throw new IntrospectionException("Unwritable property '"
                        + pathSegment
                        + "'");
            }

            // do basic conversions
            Converter<?> converter = ConverterFactory.factory.getConverter(descriptor.getPropertyType());
            value = (converter != null)
            			? converter.convert(value, (Class)descriptor.getPropertyType()) 
            			: value;

            // set
            writer.invoke(object, value);
        }
        // note that Map has two traditional bean properties - 'empty' and 'class', so
        // do a check here only after descriptor lookup failed.
        else if (object instanceof Map) {
            ((Map<Object, Object>) object).put(pathSegment, value);
        }
        else {
            throw new IntrospectionException("No property '"
                    + pathSegment
                    + "' found in class "
                    + object.getClass().getName());
        }
    }

    static PropertyDescriptor getPropertyDescriptor(
            Class<?> beanClass,
            String propertyName) throws IntrospectionException {
        // bean info is cached by introspector, so this should have reasonable
        // performance...
        BeanInfo info = Introspector.getBeanInfo(beanClass);
        PropertyDescriptor[] descriptors = info.getPropertyDescriptors();

        for (PropertyDescriptor descriptor : descriptors) {
            if (propertyName.equals(descriptor.getName())) {
                return descriptor;
            }
        }

        return null;
    }

    /**
     * "Normalizes" passed type, converting primitive types to their object counterparts.
     */
    static Class<?> normalizeType(Class<?> type) {
        if (type.isPrimitive()) {

            String className = type.getName();
            if ("byte".equals(className)) {
                return Byte.class;
            }
            else if ("int".equals(className)) {
                return Integer.class;
            }
            else if ("long".equals(className)) {
                return Long.class;
            }
            else if ("short".equals(className)) {
                return Short.class;
            }
            else if ("char".equals(className)) {
                return Character.class;
            }
            else if ("double".equals(className)) {
                return Double.class;
            }
            else if ("float".equals(className)) {
                return Float.class;
            }
            else if ("boolean".equals(className)) {
                return Boolean.class;
            }
        }

        return type;
    }

    /**
     * Returns default value that should be used for nulls. For non-primitive types, null
     * is returned. For primitive types a default such as zero or false is returned.
     */
    static Object defaultNullValueForType(Class<?> type) {
        if (type != null && type.isPrimitive()) {

            String className = type.getName();
            if ("byte".equals(className)) {
                return Byte.valueOf((byte) 0);
            }
            else if ("int".equals(className)) {
                return Integer.valueOf(0);
            }
           else if ("long".equals(className)) {
               return Long.valueOf(0);
           }
            else if ("short".equals(className)) {
                return Short.valueOf((short) 0);
            }
            else if ("char".equals(className)) {
                return Character.valueOf((char) 0);
            }
            else if ("double".equals(className)) {
                return new Double(0.0d);
            }
            else if ("float".equals(className)) {
                return new Float(0.0f);
            }
            else if ("boolean".equals(className)) {
                return Boolean.FALSE;
            }
        }

        return null;
    }

    private PropertyUtils() {
        super();
    }

    static final class NestedBeanAccessor implements Accessor {

        private Collection<Accessor> accessors;
        private String name;

        NestedBeanAccessor(String name) {
            accessors = new ArrayList<Accessor>();
            this.name = name;
        }

        void addAccessor(Accessor accessor) {
            accessors.add(accessor);
        }

        public String getName() {
            return name;
        }

        public Object getValue(Object object) throws PropertyException {

            Object value = object;
            for (Accessor accessor : accessors) {
                if (value == null) {
                    throw new IllegalArgumentException(
                            "Null object at the end of the segment '"
                                    + accessor.getName()
                                    + "'");
                }

                value = accessor.getValue(value);
            }

            return value;
        }

        public void setValue(Object object, Object newValue) throws PropertyException {
            Object value = object;
            Iterator<Accessor> accessors = this.accessors.iterator();
            while (accessors.hasNext()) {
                Accessor accessor = accessors.next();

                if (accessors.hasNext()) {
                    value = accessor.getValue(value);
                }
                else {
                    accessor.setValue(value, newValue);
                }
            }
        }
    }
}
