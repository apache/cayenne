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

    private Map<Class<?>, Converter<?>> converters;
    private EnumConverter enumConveter = new EnumConverter();
    private Converter<Object> toAnyConverter = new ToAnyConverter<Object>();
    
    private ConverterFactory() {

        Converter<String> toStringConverter = new Converter<String>() {

            @Override
			protected String convert(Object object, Class<String> type) {
                return object != null ? object.toString() : null;
            }
        };

        Converter<Boolean> toBooleanConverter = new Converter<Boolean>() {

            @Override
            protected Boolean convert(Object object, Class<Boolean> type) {
                if (object == null) {
                    return type.isPrimitive() ? Boolean.FALSE : null;
                }

                if (object instanceof Boolean) {
                    return (Boolean)object;
                } else if (object instanceof Integer || object instanceof Long || object instanceof Short || object instanceof Byte) {
                	if (((Number)object).longValue() == 0)
                		return Boolean.FALSE;
                	else if (((Number)object).longValue() == 1)
                		return Boolean.TRUE;
                }

                return "true".equalsIgnoreCase(object.toString())
                        ? Boolean.TRUE
                        : Boolean.FALSE;
            }
        };

        Converter<Long> toLongConverter = new Converter<Long>() {

            @Override
            protected Long convert(Object object, Class<Long> type) {
                if (object == null) {
                    return type.isPrimitive() ? Long.valueOf(0) : null;
                }

                if (object instanceof Long) {
                    return (Long)object;
                }

                return new Long(object.toString());
            }
        };
        
        Converter<Integer> toIntConverter = new Converter<Integer>() {

            @Override
            protected Integer convert(Object object, Class<Integer> type) {
                if (object == null) {
                    return type.isPrimitive() ? Integer.valueOf(0) : null;
                }

                if (object instanceof Integer) {
                    return (Integer)object;
                }

                return new Integer(object.toString());
            }
        };

        Converter<Byte> toByteConverter = new Converter<Byte>() {

            @Override
            protected Byte convert(Object object, Class<Byte> type) {
                if (object == null) {
                    return type.isPrimitive() ? Byte.valueOf((byte) 0) : null;
                }

                if (object instanceof Byte) {
                    return (Byte)object;
                }

                return new Byte(object.toString());
            }
        };

        Converter<Short> toShortConverter = new Converter<Short>() {

            @Override
            protected Short convert(Object object, Class<Short> type) {
                if (object == null) {
                    return type.isPrimitive() ? Short.valueOf((short) 0) : null;
                }

                if (object instanceof Short) {
                    return (Short)object;
                }

                return new Short(object.toString());
            }
        };

        Converter<Character> toCharConverter = new Converter<Character>() {

            @Override
            protected Character convert(Object object, Class<Character> type) {
                if (object == null) {
                    return type.isPrimitive() ? Character.valueOf((char) 0) : null;
                }

                if (object instanceof Character) {
                    return (Character)object;
                }

                String string = object.toString();
                return Character.valueOf(string.length() > 0 ? string.charAt(0) : 0);
            }
        };

        Converter<Double> toDoubleConverter = new Converter<Double>() {

            @Override
            protected Double convert(Object object, Class<Double> type) {
                if (object == null) {
                    return type.isPrimitive() ? new Double(0.0d) : null;
                }

                if (object instanceof Double) {
                    return (Double)object;
                }

                return new Double(object.toString());
            }
        };

        Converter<Float> toFloatConverter = new Converter<Float>() {

            @Override
            protected Float convert(Object object, Class<Float> type) {
                if (object == null) {
                    return type.isPrimitive() ? new Float(0.0f) : null;
                }

                if (object instanceof Float) {
                    return (Float)object;
                }

                return new Float(object.toString());
            }
        };

        Converter<BigDecimal> toBigDecimalConverter = new Converter<BigDecimal>() {

            @Override
            protected BigDecimal convert(Object object, Class<BigDecimal> type) {
                if (object == null) {
                    return null;
                }

                if (object instanceof BigDecimal) {
                    return (BigDecimal)object;
                }

                return new BigDecimal(object.toString());
            }
        };

        Converter<BigInteger> toBigIntegerConverter = new Converter<BigInteger>() {

            @Override
            protected BigInteger convert(Object object, Class<BigInteger> type) {
                if (object == null) {
                    return null;
                }

                if (object instanceof BigInteger) {
                    return (BigInteger)object;
                }

                return new BigInteger(object.toString());
            }
        };

		Converter<Date> toDateConverter = new Converter<Date>() {
			@Override
			protected Date convert(Object value, Class<Date> type) {
				if (value == null) return null;
				if (value instanceof Date) return (Date) value;
				if (value instanceof Number) return new Date(((Number)value).longValue());
				return new Date(value.toString());
			}
		};
		
		Converter<Timestamp> toTimestampConverter = new Converter<Timestamp>() {
			@Override
			protected Timestamp convert(Object value, Class<Timestamp> type) {
				if (value == null) return null;
				if (value instanceof Timestamp) return (Timestamp) value;
				if (value instanceof Number) return new Timestamp(((Number)value).longValue());
				return new Timestamp(Date.parse(value.toString()));
			}
		};
		
		// TODO: byte[] converter...

        converters = new HashMap<Class<?>, Converter<?>>();

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
     * @param type
     * 		the Class to convert a value to; the destination type
     * @param converter
     * 		a converter used to convert the value from Object to T
     * @since 3.2
     */
    public static <T> void addConverter(Class<? super T> type, Converter<T> converter) {
    	factory._addConverter(type, converter);
    }
    
    private <T> void _addConverter(Class<? super T> type, Converter<T> converter) {
    	converters.put(type, converter);
    }
    
    <T> Converter<T> getConverter(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Null type");
        }

        // check for enum BEFORE super call, as it will return a noop converter
        if (type.isEnum()) {
            return enumConveter;
        }

        Converter<T> c = (Converter<T>) converters.get(type);
        return c != null ? c : (Converter<T>)toAnyConverter;
    }
}
