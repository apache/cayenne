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

import java.lang.reflect.Method;

/**
 * A property accessor that uses set/get methods following JavaBean naming
 * conventions.
 * 
 * @since 1.2
 */
public class BeanAccessor implements Accessor {

	private static final long serialVersionUID = 606253801447018099L;

	protected String propertyName;
	protected Method readMethod;
	protected Method writeMethod;
	protected Object nullValue;

	public BeanAccessor(Class<?> objectClass, String propertyName, Class<?> propertyType) {
		this( objectClass, propertyName, propertyType, defaultBooleanGetterName( propertyName ), defaultGetterName( propertyName ), defaultSetterName( propertyName ) );
	}

	protected BeanAccessor(Class<?> objectClass, String propertyName, Class<?> propertyType, String booleanGetterName, String getterName, String setterName ) {
		if (objectClass == null) {
			throw new IllegalArgumentException("Null objectClass");
		}

		checkPropertyName(propertyName);
		
		if (booleanGetterName == null) {
			throw new IllegalArgumentException("Null booleanGetterName");
		}
		
		if (getterName ==  null) {
			throw new IllegalArgumentException("Null getterName");
		}
		
		if (setterName == null) {
			throw new IllegalArgumentException("Null setterName");
		}

		this.propertyName = propertyName;
		this.nullValue = PropertyUtils.defaultNullValueForType(propertyType);

		Method[] publicMethods = objectClass.getMethods();

		Method getter = null;
		for (Method method : publicMethods) {
			Class<?> returnType = method.getReturnType();
			// following Java Bean naming conventions, "is" methods are preferred over "get" methods
			if (method.getName().equals(booleanGetterName) && returnType.equals(Boolean.TYPE) && method.getParameterTypes().length == 0) {
				getter = method;
				break;
			}
			// Find the method with the most specific return type.
			// This is the same behavior as Class.getMethod(String, Class...) except that
			// Class.getMethod prefers synthetic methods generated for interfaces
			// over methods with more specific return types in a super class.
			if (method.getName().equals(getterName) && method.getParameterTypes().length == 0) {
				if (returnType.isPrimitive()) {
					getter = returnType.equals(Void.TYPE) ? null : method;
					if (returnType.equals(Boolean.TYPE)) {
						// keep looking for the "is" method
						continue;
					} else {
						// nothing more specific than a primitive, so stop here
						break;
					}
				}
				if (getter == null || getter.getReturnType().isAssignableFrom(returnType)) {
					getter = method;
				}
			}
		}

		if (getter == null) {
			throw new IllegalArgumentException("Property '" + propertyName + "' is not readable");
		}

		this.readMethod = getter;

		// TODO: compare 'propertyType' arg with readMethod.getReturnType()

		for (Method method : publicMethods) {
			if (!method.getName().equals(setterName) || !method.getReturnType().equals(Void.TYPE)) {
				continue;
			}
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes.length != 1) {
				continue;
			}
			if (getter.getReturnType().isAssignableFrom(parameterTypes[0])) {
				this.writeMethod = method;
				break;
			}
		}
	}

	public String getName() {
		return propertyName;
	}

	/**
	 * @since 3.0
	 */
	public Object getValue(Object object) throws PropertyException {

		try {
			return readMethod.invoke(object, (Object[]) null);
		} catch (Throwable th) {
			throw new PropertyException("Error reading property: " + propertyName, this, object, th);
		}
	}

	/**
	 * @since 3.0
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setValue(Object object, Object newValue) throws PropertyException {

		if (writeMethod == null) {
			throw new PropertyException("Property '" + propertyName + "' is not writable", this, object);
		}

		Class type = writeMethod.getParameterTypes()[0];
		Converter<?> converter = ConverterFactory.factory.getConverter(type);
		try {
			newValue = (converter != null) ? converter.convert(newValue, type) : newValue;
	
			// this will take care of primitives.
			if (newValue == null) {
				newValue = this.nullValue;
			}

			writeMethod.invoke(object, newValue);
		} catch (Throwable th) {
			throw new PropertyException("Error writing property: " + propertyName, this, object, th);
		}
	}
	
	private static String defaultSetterName( String propertyName ) {
		checkPropertyName(propertyName);
		final String capitalized = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
		return "set" + capitalized;
	}

	private static String defaultGetterName( String propertyName ) {
		checkPropertyName(propertyName);
		final String capitalized = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
		return "get" + capitalized;
	}

	private static String defaultBooleanGetterName( String propertyName ) {
		checkPropertyName(propertyName);
		final String capitalized = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
		return "is" + capitalized;
	}

	private static void checkPropertyName(String propertyName) {
		if (propertyName == null) {
			throw new IllegalArgumentException("Null propertyName");
		}

		if (propertyName.length() == 0) {
			throw new IllegalArgumentException("Empty propertyName");
		}
	}
}
