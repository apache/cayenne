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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.Entity;

/**
 * Utility methods to quickly access object properties. This class supports
 * simple and nested properties and also conversion of property values to match
 * property type. No converter customization is provided yet, so only basic
 * converters for Strings, Numbers and primitives are available.
 * 
 * @since 1.2
 */
public class PropertyUtils {

	private static final ConcurrentMap<String, Accessor> PATH_ACCESSORS = new ConcurrentHashMap<String, Accessor>();
	private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Accessor>> SEGMENT_ACCESSORS = new ConcurrentHashMap<Class<?>, ConcurrentMap<String, Accessor>>();

	/**
	 * Compiles an accessor that can be used for fast access for the nested
	 * property of the objects of a given class.
	 * 
	 * @since 4.0
	 */
	public static Accessor accessor(String nestedPropertyName) {

		if (nestedPropertyName == null) {
			throw new IllegalArgumentException("Null property name.");
		}

		if (nestedPropertyName.length() == 0) {
			throw new IllegalArgumentException("Empty property name.");
		}

		// PathAccessor is simply a chain of path segment wrappers. The actual
		// accessor is resolved (with caching) during evaluation. Otherwise we
		// won't be able to handle subclasses of declared property types...

		// TODO: perhaps Java 7 MethodHandles are the answer to truly "compiled"
		// path accessor?

		return compilePathAccessor(nestedPropertyName);
	}

	static Accessor compilePathAccessor(String path) {

		Accessor accessor = PATH_ACCESSORS.get(path);

		if (accessor == null) {

			int dot = path.indexOf(Entity.PATH_SEPARATOR);
			if (dot == 0 || dot == path.length() - 1) {
				throw new IllegalArgumentException("Invalid path: " + path);
			}

			String segment = dot < 0 ? path : path.substring(0, dot);
			Accessor remainingAccessor = dot < 0 ? null : compilePathAccessor(path.substring(dot + 1));
			Accessor newAccessor = new PathAccessor(segment, remainingAccessor);

			Accessor existingAccessor = PATH_ACCESSORS.putIfAbsent(path, newAccessor);
			accessor = existingAccessor != null ? existingAccessor : newAccessor;
		}

		return accessor;
	}

	static Accessor getOrCreateSegmentAccessor(Class<?> objectClass, String propertyName) {
		ConcurrentMap<String, Accessor> forType = SEGMENT_ACCESSORS.get(objectClass);
		if (forType == null) {

			ConcurrentMap<String, Accessor> newPropAccessors = new ConcurrentHashMap<String, Accessor>();
			ConcurrentMap<String, Accessor> existingPropAccessors = SEGMENT_ACCESSORS.putIfAbsent(objectClass,
					newPropAccessors);
			forType = existingPropAccessors != null ? existingPropAccessors : newPropAccessors;
		}

		Accessor a = forType.get(propertyName);
		if (a == null) {
			Accessor newA = createSegmentAccessor(objectClass, propertyName);
			Accessor existingA = forType.putIfAbsent(propertyName, newA);
			a = existingA != null ? existingA : newA;
		}

		return a;
	}

	static Accessor createSegmentAccessor(Class<?> objectClass, String propertyName) {

		if (Map.class.isAssignableFrom(objectClass)) {
			return new MapAccessor(propertyName);
		} else {
			return new BeanAccessor(objectClass, propertyName, null);
		}
	}

	/**
	 * Returns object property using JavaBean-compatible introspection with one
	 * addition - a property can be a dot-separated property name path.
	 */
	public static Object getProperty(Object object, String nestedPropertyName) throws CayenneRuntimeException {
		return accessor(nestedPropertyName).getValue(object);
	}

	/**
	 * Sets object property using JavaBean-compatible introspection with one
	 * addition - a property can be a dot-separated property name path. Before
	 * setting a value attempts to convert it to a type compatible with the
	 * object property. Automatic conversion is supported between strings and
	 * basic types like numbers or primitives.
	 */
	public static void setProperty(Object object, String nestedPropertyName, Object value)
			throws CayenneRuntimeException {
		accessor(nestedPropertyName).setValue(object, value);
	}

	/**
	 * "Normalizes" passed type, converting primitive types to their object
	 * counterparts.
	 */
	static Class<?> normalizeType(Class<?> type) {
		if (type.isPrimitive()) {

			String className = type.getName();
			if ("byte".equals(className)) {
				return Byte.class;
			} else if ("int".equals(className)) {
				return Integer.class;
			} else if ("long".equals(className)) {
				return Long.class;
			} else if ("short".equals(className)) {
				return Short.class;
			} else if ("char".equals(className)) {
				return Character.class;
			} else if ("double".equals(className)) {
				return Double.class;
			} else if ("float".equals(className)) {
				return Float.class;
			} else if ("boolean".equals(className)) {
				return Boolean.class;
			}
		}

		return type;
	}

	/**
	 * Returns default value that should be used for nulls. For non-primitive
	 * types, null is returned. For primitive types a default such as zero or
	 * false is returned.
	 */
	static Object defaultNullValueForType(Class<?> type) {
		if (type != null && type.isPrimitive()) {

			String className = type.getName();
			if ("byte".equals(className)) {
				return Byte.valueOf((byte) 0);
			} else if ("int".equals(className)) {
				return Integer.valueOf(0);
			} else if ("long".equals(className)) {
				return Long.valueOf(0);
			} else if ("short".equals(className)) {
				return Short.valueOf((short) 0);
			} else if ("char".equals(className)) {
				return Character.valueOf((char) 0);
			} else if ("double".equals(className)) {
				return new Double(0.0d);
			} else if ("float".equals(className)) {
				return new Float(0.0f);
			} else if ("boolean".equals(className)) {
				return Boolean.FALSE;
			}
		}

		return null;
	}

	private PropertyUtils() {
		super();
	}

	static final class PathAccessor implements Accessor {

		private static final long serialVersionUID = 2056090443413498626L;

		private String segmentName;
		private Accessor nextAccessor;

		public PathAccessor(String segmentName, Accessor nextAccessor) {
			this.segmentName = segmentName;
			this.nextAccessor = nextAccessor;
		}

		@Override
		public String getName() {
			return segmentName;
		}

		@Override
		public Object getValue(Object object) throws PropertyException {
			if (object == null) {
				return null;
			}

			Object value = getOrCreateSegmentAccessor(object.getClass(), segmentName).getValue(object);
			return nextAccessor != null ? nextAccessor.getValue(value) : value;
		}

		@Override
		public void setValue(Object object, Object newValue) throws PropertyException {
			if (object == null) {
				return;
			}

			Accessor segmentAccessor = getOrCreateSegmentAccessor(object.getClass(), segmentName);
			if (nextAccessor != null) {
				nextAccessor.setValue(segmentAccessor.getValue(object), newValue);
			} else {
				segmentAccessor.setValue(object, newValue);
			}
		}
	}
}
