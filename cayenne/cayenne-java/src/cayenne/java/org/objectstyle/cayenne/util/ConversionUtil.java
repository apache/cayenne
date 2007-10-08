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

package org.objectstyle.cayenne.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.objectstyle.cayenne.exp.ExpressionException;

/**
 * A collection of static conversion utility methods.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public final class ConversionUtil {

    public static int toInt(Object object, int defaultValue) {
        if (object == null) {
            return defaultValue;
        }
        else if (object instanceof Number) {
            return ((Number) object).intValue();
        }
        else if (object instanceof String) {
            try {
                return Integer.parseInt((String) object);
            }
            catch (NumberFormatException ex) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    public static boolean toBoolean(Object object) {
        if (object instanceof Boolean) {
            return ((Boolean) object).booleanValue();
        }

        if (object instanceof Number) {
            return ((Number) object).intValue() != 0;
        }

        return object != null;
    }

    public static BigDecimal toBigDecimal(Object object) {

        if (object == null) {
            return null;
        }
        else if (object instanceof BigDecimal) {
            return (BigDecimal) object;
        }
        else if (object instanceof BigInteger) {
            return new BigDecimal((BigInteger) object);
        }
        else if (object instanceof Number) {
            return new BigDecimal(((Number) object).doubleValue());
        }

        throw new ExpressionException("Can't convert to BigDecimal: " + object);
    }

    /**
     * Attempts to convert an object to Comparable instance.
     */
    public static Comparable toComparable(Object object) {
        if (object == null) {
            return null;
        }
        else if (object instanceof Comparable) {
            return (Comparable) object;
        }
        else if (object instanceof StringBuffer) {
            return object.toString();
        }
        else if (object instanceof char[]) {
            return new String((char[]) object);
        }
        else {
            throw new ClassCastException(
                "Invalid Comparable class:" + object.getClass().getName());
        }
    }

    /**
     * Attempts to convert an object to Comparable instance.
     */
    public static String toString(Object object) {
        if (object == null) {
            return null;
        }
        else if (object instanceof String) {
            return (String) object;
        }
        else if (object instanceof StringBuffer) {
            return object.toString();
        }
        else if (object instanceof char[]) {
            return new String((char[]) object);
        }
        else {
            throw new ClassCastException(
                "Invalid class for String conversion:" + object.getClass().getName());
        }
    }

    /**
     * Attempts to convert an object to an uppercase string.
     */
    public static Object toUpperCase(Object object) {
        if ((object instanceof String) || (object instanceof StringBuffer)) {
            return object.toString().toUpperCase();
        }
        else if (object instanceof char[]) {
            return new String((char[]) object).toUpperCase();
        }
        else {
            return object;
        }
    }

    private ConversionUtil() {
    }
}
