/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/


package org.apache.cayenne.util;

import org.apache.cayenne.exp.ExpressionException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A collection of static conversion utility methods.
 * 
 * @since 1.1
 */
public final class ConversionUtil {

    public static int toInt(Object object, int defaultValue) {
        if (object == null) {
            return defaultValue;
        } else if (object instanceof Number) {
            return ((Number) object).intValue();
        } else if (object instanceof String) {
            try {
                return Integer.parseInt((String) object);
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }

        return defaultValue;
    }
    
    /**
     * @since 3.1
     */
    public static long toLong(Object object, long defaultValue) {
        if (object == null) {
            return defaultValue;
        } else if (object instanceof Number) {
            return ((Number) object).longValue();
        } else if (object instanceof String) {
            try {
                return Long.parseLong((String) object);
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * @since 4.0
     */
    public static double toDouble(Object object, double defaultValue) {
        if (object == null) {
            return defaultValue;
        } else if (object instanceof Number) {
            return ((Number) object).doubleValue();
        } else if (object instanceof String) {
            try {
                return Double.parseDouble((String) object);
            } catch (NumberFormatException ex) {
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
        } else if (object instanceof BigDecimal) {
            return (BigDecimal) object;
        } else if (object instanceof BigInteger) {
            return new BigDecimal((BigInteger) object);
        } else if (object instanceof Number) {
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
        } else if (object instanceof Comparable) {
            return (Comparable) object;
        } else if (object instanceof StringBuilder) {
            return object.toString();
        } else if (object instanceof StringBuffer) {
            return object.toString();
        } else if (object instanceof char[]) {
            return new String((char[]) object);
        } else {
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
        } else if (object instanceof CharSequence) {
            return object.toString();
        } else if (object instanceof char[]) {
            return new String((char[]) object);
        } else {
            throw new ClassCastException(
                "Invalid class for String conversion:" + object.getClass().getName());
        }
    }

    /**
     * Attempts to convert an object to an uppercase string.
     */
    public static Object toUpperCase(Object object) {
        if (object instanceof CharSequence) {
            return object.toString().toUpperCase();
        } else if (object instanceof char[]) {
            return new String((char[]) object).toUpperCase();
        } else {
            return object;
        }
    }

    private ConversionUtil() {
    }
}
