/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;

import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * Comparator that can compare Java beans based on a 
 * value of a property. Bean property must be readable
 * and its type must be an instance of Comparable. 
 * 
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
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (bean == null) {
            throw new NullPointerException("Null bean. Property: " + propertyName);
        }

        Method getter = findReadMethod(propertyName, bean.getClass());

        if (getter == null) {
            throw new NoSuchMethodException(
                "No such property '"
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
