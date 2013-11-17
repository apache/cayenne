package org.apache.cayenne.reflect;

import java.lang.reflect.Constructor;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * Can convert to any class that has a constructor that takes a 
 * single Object or a single String parameter.
 */
public class ToAnyConverter<T> extends Converter<T> {
	@Override
	protected T convert(Object value, Class<T> type) {
		if (value == null) return null;
		if (type.isAssignableFrom(value.getClass())) return (T) value; // no conversion needed
		
        try {
            Constructor<?> constructor;
            try {
            	constructor = type.getConstructor(Object.class);
            } catch (NoSuchMethodException e) {
                constructor = type.getConstructor(String.class);
            	value = value.toString();
            }
            return (T) constructor.newInstance(value);
        } catch (Exception e) {
            throw new CayenneRuntimeException(e);
        }
	}
}