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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory of property type converters.
 * 
 * @since 1.2
 */
class ConverterFactory {

    static final ConverterFactory factory = new ConverterFactory();

    private Map<String, Converter> converters;
    private EnumConverter enumConveter = new EnumConverter();

    static final Converter noopConverter = new Converter() {

        @Override
        Object convert(Object object, Class<?> type) {
            return object;
        }
    };

    private ConverterFactory() {

        Converter stringConverter = new Converter() {

            @Override
            Object convert(Object object, Class<?> type) {
                return object != null ? object.toString() : null;
            }
        };

        Converter booleanConverter = new Converter() {

            @Override
            Object convert(Object object, Class<?> type) {
                if (object == null) {
                    return type.isPrimitive() ? Boolean.FALSE : null;
                }

                if (object instanceof Boolean) {
                    return object;
                }

                return "true".equalsIgnoreCase(object.toString())
                        ? Boolean.TRUE
                        : Boolean.FALSE;
            }
        };

        Converter intConverter = new Converter() {

            @Override
            Object convert(Object object, Class<?> type) {
                if (object == null) {
                    return type.isPrimitive() ? Integer.valueOf(0) : null;
                }

                if (object instanceof Integer) {
                    return object;
                }

                return new Integer(object.toString());
            }
        };

        Converter byteConverter = new Converter() {

            @Override
            Object convert(Object object, Class<?> type) {
                if (object == null) {
                    return type.isPrimitive() ? Byte.valueOf((byte) 0) : null;
                }

                if (object instanceof Byte) {
                    return object;
                }

                return new Byte(object.toString());
            }
        };

        Converter shortConverter = new Converter() {

            @Override
            Object convert(Object object, Class<?> type) {
                if (object == null) {
                    return type.isPrimitive() ? Short.valueOf((short) 0) : null;
                }

                if (object instanceof Short) {
                    return object;
                }

                return new Short(object.toString());
            }
        };

        Converter charConverter = new Converter() {

            @Override
            Object convert(Object object, Class<?> type) {
                if (object == null) {
                    return type.isPrimitive() ? Character.valueOf((char) 0) : null;
                }

                if (object instanceof Character) {
                    return object;
                }

                String string = object.toString();
                return Character.valueOf(string.length() > 0 ? string.charAt(0) : 0);
            }
        };

        Converter doubleConverter = new Converter() {

            @Override
            Object convert(Object object, Class<?> type) {
                if (object == null) {
                    return type.isPrimitive() ? new Double(0.0d) : null;
                }

                if (object instanceof Double) {
                    return object;
                }

                return new Double(object.toString());
            }
        };

        Converter floatConverter = new Converter() {

            @Override
            Object convert(Object object, Class<?> type) {
                if (object == null) {
                    return type.isPrimitive() ? new Float(0.0f) : null;
                }

                if (object instanceof Float) {
                    return object;
                }

                return new Float(object.toString());
            }
        };

        Converter bigDecimalConverter = new Converter() {

            @Override
            Object convert(Object object, Class<?> type) {
                if (object == null) {
                    return null;
                }

                if (object instanceof BigDecimal) {
                    return object;
                }

                return new BigDecimal(object.toString());
            }
        };

        Converter bigIntegerConverter = new Converter() {

            @Override
            Object convert(Object object, Class<?> type) {
                if (object == null) {
                    return null;
                }

                if (object instanceof BigInteger) {
                    return object;
                }

                return new BigInteger(object.toString());
            }
        };

        // TODO: byte[] converter...

        converters = new HashMap<String, Converter>();

        converters.put(Boolean.class.getName(), booleanConverter);
        converters.put("boolean", booleanConverter);

        converters.put(Short.class.getName(), shortConverter);
        converters.put("short", shortConverter);

        converters.put(Byte.class.getName(), byteConverter);
        converters.put("byte", byteConverter);

        converters.put(Integer.class.getName(), intConverter);
        converters.put("int", intConverter);

        converters.put(Double.class.getName(), doubleConverter);
        converters.put("double", doubleConverter);

        converters.put(Float.class.getName(), floatConverter);
        converters.put("float", floatConverter);

        converters.put(Character.class.getName(), charConverter);
        converters.put("char", charConverter);

        converters.put(BigDecimal.class.getName(), bigDecimalConverter);
        converters.put(BigInteger.class.getName(), bigIntegerConverter);
        converters.put(Number.class.getName(), bigDecimalConverter);
        converters.put(String.class.getName(), stringConverter);
    }

    Converter getConverter(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Null type");
        }

        // check for enum BEFORE super call, as it will return a noop converter
        if (type.isEnum()) {
            return enumConveter;
        }

        Converter c = converters.get(type.getName());
        return c != null ? c : noopConverter;
    }
}
