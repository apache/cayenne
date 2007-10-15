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

import org.apache.cayenne.util.Util;

/**
 * A factory of property type converters.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ConverterFactory {

    private static final String FACTORY_CLASS_JDK15 = "org.apache.cayenne.reflect.ConverterFactory15";

    static final ConverterFactory factory = createFactory();
    static Map converters;

    static final Converter noopConverter = new Converter() {

        Object convert(Object object, Class type) {
            return object;
        }
    };

    static {

        Converter stringConverter = new Converter() {

            Object convert(Object object, Class type) {
                return object != null ? object.toString() : null;
            }
        };

        Converter booleanConverter = new Converter() {

            Object convert(Object object, Class type) {
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

            Object convert(Object object, Class type) {
                if (object == null) {
                    return type.isPrimitive() ? new Integer(0) : null;
                }

                if (object instanceof Integer) {
                    return object;
                }

                return new Integer(object.toString());
            }
        };

        Converter byteConverter = new Converter() {

            Object convert(Object object, Class type) {
                if (object == null) {
                    return type.isPrimitive() ? new Byte((byte) 0) : null;
                }

                if (object instanceof Byte) {
                    return object;
                }

                return new Byte(object.toString());
            }
        };

        Converter shortConverter = new Converter() {

            Object convert(Object object, Class type) {
                if (object == null) {
                    return type.isPrimitive() ? new Short((short) 0) : null;
                }

                if (object instanceof Short) {
                    return object;
                }

                return new Short(object.toString());
            }
        };

        Converter charConverter = new Converter() {

            Object convert(Object object, Class type) {
                if (object == null) {
                    return type.isPrimitive() ? new Character((char) 0) : null;
                }

                if (object instanceof Character) {
                    return object;
                }

                String string = object.toString();
                return new Character(string.length() > 0 ? string.charAt(0) : 0);
            }
        };

        Converter doubleConverter = new Converter() {

            Object convert(Object object, Class type) {
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

            Object convert(Object object, Class type) {
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

            Object convert(Object object, Class type) {
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

            Object convert(Object object, Class type) {
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

        converters = new HashMap();

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

    static ConverterFactory createFactory() {
        try {
            // sniff JDK 1.5
            Class.forName("java.lang.StringBuilder");

            Class factoryClass = Util.getJavaClass(FACTORY_CLASS_JDK15);
            return (ConverterFactory) factoryClass.newInstance();
        }
        catch (Throwable th) {
            // .. jdk 1.4
            return new ConverterFactory();
        }
    }

    Converter getConverter(Class type) {
        if (type == null) {
            throw new IllegalArgumentException("Null type");
        }

        Converter c = (Converter) converters.get(type.getName());
        return c != null ? c : noopConverter;
    }
}
