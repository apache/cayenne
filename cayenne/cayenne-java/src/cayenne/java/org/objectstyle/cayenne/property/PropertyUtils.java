/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.property;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.StringTokenizer;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.util.Util;

/**
 * Utility methods to quickly access object properties. This class supports simple and
 * nested properties and also conversion of property values to match property type. No
 * converter customization is provided yet, so only basic converters for Strings, Numbers
 * and primitives are available.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class PropertyUtils {

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
                    throw new CayenneRuntimeException(
                            "Null value in the middle of the path");
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
            object = getProperty(object, pathSegment);

            if (object == null) {
                throw new IllegalArgumentException(
                        "Null object at the end of the segment '" + pathSegment + "'");
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

            return reader.invoke(object, null);
        }
        // note that Map has two traditional bean properties - 'empty' and 'class', so
        // do a check here only after descriptor lookup failed.
        else if (object instanceof Map) {
            return ((Map) object).get(pathSegment);
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

            value = ConverterFactory.factory
                    .getConverter(descriptor.getPropertyType())
                    .convert(value, descriptor.getPropertyType());

            // set
            writer.invoke(object, new Object[] {
                value
            });
        }
        // note that Map has two traditional bean properties - 'empty' and 'class', so
        // do a check here only after descriptor lookup failed.
        else if (object instanceof Map) {
            ((Map) object).put(pathSegment, value);
        }
        else {
            throw new IntrospectionException("No property '"
                    + pathSegment
                    + "' found in class "
                    + object.getClass().getName());
        }
    }

    static PropertyDescriptor getPropertyDescriptor(Class beanClass, String propertyName)
            throws IntrospectionException {
        // bean info is cached by introspector, so this should have reasonable
        // performance...
        BeanInfo info = Introspector.getBeanInfo(beanClass);
        PropertyDescriptor[] descriptors = info.getPropertyDescriptors();

        for (int i = 0; i < descriptors.length; i++) {
            if (propertyName.equals(descriptors[i].getName())) {
                return descriptors[i];
            }
        }

        return null;
    }

    /**
     * "Normalizes" passed type, converting primitive types to their object counterparts.
     */
    static Class normalizeType(Class type) {
        if (type.isPrimitive()) {

            String className = type.getName();
            if ("byte".equals(className)) {
                return Byte.class;
            }
            else if ("int".equals(className)) {
                return Integer.class;
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
    static Object defaultNullValueForType(Class type) {
        if (type.isPrimitive()) {

            String className = type.getName();
            if ("byte".equals(className)) {
                return new Byte((byte) 0);
            }
            else if ("int".equals(className)) {
                return new Integer(0);
            }
            else if ("short".equals(className)) {
                return new Short((short) 0);
            }
            else if ("char".equals(className)) {
                return new Character((char) 0);
            }
            else if ("double".equals(className)) {
                return new Double(0d);
            }
            else if ("float".equals(className)) {
                return new Float(0f);
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
}
