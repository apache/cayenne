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

package org.apache.cayenne.reflect;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * A factory of property type converters.
 * 
 * @since 1.2
 */
public class ConverterFactory {

    static final ConverterFactory factory = new ConverterFactory();

    private final Map<Class<?>, Converter<?>> converters;
    private final EnumConverter<?> enumConverter = new EnumConverter<>();
    private final Converter<Object> toAnyConverter = new ToAnyConverter<>();

    // TODO: this methods uses deprecated Date.parse method that has no direct replacement,
    //       we need to determine date formats we should support and update code accordingly
    @SuppressWarnings("deprecation")
    private ConverterFactory() {

        Converter<String> toStringConverter = (object, type) -> object != null ? object.toString() : null;

        Converter<Boolean> toBooleanConverter = (object, type) -> {
            if (object == null) {
                return type.isPrimitive() ? Boolean.FALSE : null;
            }

            if (object instanceof Boolean) {
                return (Boolean) object;
            } else if (object instanceof Integer || object instanceof Long || object instanceof Short || object instanceof Byte) {
                if (((Number) object).longValue() == 0) {
                    return Boolean.FALSE;
                } else if (((Number) object).longValue() == 1) {
                    return Boolean.TRUE;
                }
            }

            return "true".equalsIgnoreCase(object.toString())
                    ? Boolean.TRUE
                    : Boolean.FALSE;
        };

        Converter<Long> toLongConverter = (object, type) -> {
            if (object == null) {
                return type.isPrimitive() ? 0L : null;
            }

            if (object instanceof Long) {
                return (Long) object;
            }

            return Long.valueOf(object.toString());
        };

        Converter<Integer> toIntConverter = (object, type) -> {
            if (object == null) {
                return type.isPrimitive() ? 0 : null;
            }

            if (object instanceof Integer) {
                return (Integer) object;
            }

            return Integer.valueOf(object.toString());
        };

        Converter<Byte> toByteConverter = (object, type) -> {
            if (object == null) {
                return type.isPrimitive() ? (byte) 0 : null;
            }

            if (object instanceof Byte) {
                return (Byte) object;
            }

            return Byte.valueOf(object.toString());
        };

        Converter<Short> toShortConverter = (object, type) -> {
            if (object == null) {
                return type.isPrimitive() ? (short) 0 : null;
            }

            if (object instanceof Short) {
                return (Short) object;
            }

            return Short.valueOf(object.toString());
        };

        Converter<Character> toCharConverter = (object, type) -> {
            if (object == null) {
                return type.isPrimitive() ? (char) 0 : null;
            }

            if (object instanceof Character) {
                return (Character) object;
            }

            String string = object.toString();
            return string.length() > 0 ? string.charAt(0) : 0;
        };

        Converter<Double> toDoubleConverter = (object, type) -> {
            if (object == null) {
                return type.isPrimitive() ? 0.0d : null;
            }

            if (object instanceof Double) {
                return (Double) object;
            }

            return Double.valueOf(object.toString());
        };

        Converter<Float> toFloatConverter = (object, type) -> {
            if (object == null) {
                return type.isPrimitive() ? 0.0f : null;
            }

            if (object instanceof Float) {
                return (Float) object;
            }

            return Float.valueOf(object.toString());
        };

        Converter<BigDecimal> toBigDecimalConverter = (object, type) -> {
            if (object == null) {
                return null;
            }

            if (object instanceof BigDecimal) {
                return (BigDecimal) object;
            }

            return new BigDecimal(object.toString());
        };

        Converter<BigInteger> toBigIntegerConverter = (object, type) -> {
            if (object == null) {
                return null;
            }

            if (object instanceof BigInteger) {
                return (BigInteger) object;
            }

            return new BigInteger(object.toString());
        };

        Converter<Date> toDateConverter = (value, type) -> {
            if (value == null) {
                return null;
            }
            if (value instanceof Date) {
                return (Date) value;
            }
            if (value instanceof Number) {
                return new Date(((Number) value).longValue());
            }
            return new Date(value.toString());
        };

        Converter<Timestamp> toTimestampConverter = (value, type) -> {
            if (value == null) {
                return null;
            }
            if (value instanceof Timestamp) {
                return (Timestamp) value;
            }
            if (value instanceof Number) {
                return new Timestamp(((Number) value).longValue());
            }
            return new Timestamp(Date.parse(value.toString()));
        };

        // TODO: byte[] converter...

        converters = new HashMap<>();

        _addConverter(Boolean.class, toBooleanConverter);
        _addConverter(boolean.class, toBooleanConverter);

        _addConverter(Short.class, toShortConverter);
        _addConverter(short.class, toShortConverter);

        _addConverter(Byte.class, toByteConverter);
        _addConverter(byte.class, toByteConverter);

        _addConverter(Integer.class, toIntConverter);
        _addConverter(int.class, toIntConverter);

        _addConverter(Long.class, toLongConverter);
        _addConverter(long.class, toLongConverter);
        
        _addConverter(Double.class, toDoubleConverter);
        _addConverter(double.class, toDoubleConverter);

        _addConverter(Float.class, toFloatConverter);
        _addConverter(float.class, toFloatConverter);

        _addConverter(Character.class, toCharConverter);
        _addConverter(char.class, toCharConverter);

        _addConverter(BigDecimal.class, toBigDecimalConverter);
        _addConverter(BigInteger.class, toBigIntegerConverter);
        _addConverter(Number.class, toBigDecimalConverter);
        _addConverter(String.class, toStringConverter);
        _addConverter(Date.class, toDateConverter);
        _addConverter(Timestamp.class, toTimestampConverter);
    }

    /**
     * Converters are used by {@link PropertyUtils#setProperty(Object, String, Object)} to coerce
     * generic Object values into the specific type expected by the named setter.
     *
     * @param type      the Class to convert a value to; the destination type
     * @param converter a converter used to convert the value from Object to T
     * @since 4.0
     */
    public static <T> void addConverter(Class<? super T> type, Converter<T> converter) {
        factory._addConverter(type, converter);
    }

    private <T> void _addConverter(Class<? super T> type, Converter<T> converter) {
        converters.put(type, converter);
    }

    @SuppressWarnings("unchecked")
    <T> Converter<T> getConverter(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Null type");
        }

        // check for enum BEFORE super call, as it will return a noop converter
        if (type.isEnum()) {
            return (Converter<T>) enumConverter;
        }

        Converter<T> c = (Converter<T>) converters.get(type);
        return c != null ? c : (Converter<T>) toAnyConverter;
    }
}
