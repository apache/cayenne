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

package org.apache.cayenne.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * Comparator that can compare Java beans based on a value of a property. Bean property
 * must be readable and its type must be an instance of Comparable.
 * 
 * @deprecated unused since 1.2. You may want to check PropertyUtils for quick property
 *             access methods.
 * @author Andrei Adamchik
 */
public class PropertyComparator implements Comparator {

    protected Method getter;
    protected boolean ascending;

    public static String capitalize(String s) {
        if (s.length() == 0) {
            return s;
        }
        char chars[] = s.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    public static Method findReadMethod(String propertyName, Class beanClass) {
        String base = capitalize(propertyName);

        // find non-boolean property
        try {
            return beanClass.getMethod("get" + base, null);
        }
        catch (Exception ex) {
            // ignore, this might be a boolean property
        }

        try {
            return beanClass.getMethod("is" + base, null);
        }
        catch (Exception ex) {
            // ran out of options
            return null;
        }
    }

    /**
     * Method to read a simple one-step property of a JavaBean.
     */
    public static Object readProperty(String propertyName, Object bean)
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        if (bean == null) {
            throw new NullPointerException("Null bean. Property: " + propertyName);
        }

        Method getter = findReadMethod(propertyName, bean.getClass());

        if (getter == null) {
            throw new NoSuchMethodException("No such property '"
                    + propertyName
                    + "' in class "
                    + bean.getClass().getName());
        }

        return getter.invoke(bean, null);
    }

    public PropertyComparator(String propertyName, Class beanClass) {
        this(propertyName, beanClass, true);
    }

    public PropertyComparator(String propertyName, Class beanClass, boolean ascending) {
        getter = findReadMethod(propertyName, beanClass);
        if (getter == null) {
            throw new CayenneRuntimeException("No getter for " + propertyName);
        }

        this.ascending = ascending;
    }

    /**
     * @see java.util.Comparator#compare(Object, Object)
     */
    public int compare(Object o1, Object o2) {
        return (ascending) ? compareAsc(o1, o2) : compareAsc(o2, o1);
    }

    protected int compareAsc(Object o1, Object o2) {

        if ((o1 == null && o2 == null) || o1 == o2) {
            return 0;
        }
        else if (o1 == null && o2 != null) {
            return -1;
        }
        else if (o1 != null && o2 == null) {
            return 1;
        }

        try {
            Comparable p1 = (Comparable) getter.invoke(o1, null);
            Comparable p2 = (Comparable) getter.invoke(o2, null);

            return (p1 == null) ? -1 : p1.compareTo(p2);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error reading property.", ex);
        }
    }
}
