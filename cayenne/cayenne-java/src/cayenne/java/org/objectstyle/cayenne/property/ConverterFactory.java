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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.util.Util;

/**
 * A factory of property type converters.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ConverterFactory {

    private static final String FACTORY_CLASS_JDK15 = "org.objectstyle.cayenne.property.ConverterFactory15";

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
                    return type.isPrimitive() ? new Double(0d) : null;
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
                    return type.isPrimitive() ? new Float(0f) : null;
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
