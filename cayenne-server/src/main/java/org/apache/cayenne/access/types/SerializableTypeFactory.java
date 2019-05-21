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
package org.apache.cayenne.access.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.cayenne.CayenneRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExtendedTypeFactory for handling serializable objects. Returned ExtendedType
 * is simply an object serialization wrapper on top of byte[] ExtendedType.
 * 
 * @since 3.0
 */
class SerializableTypeFactory implements ExtendedTypeFactory {

	private static final Logger logger = LoggerFactory.getLogger(SerializableTypeFactory.class);

	private ExtendedTypeMap map;

	SerializableTypeFactory(ExtendedTypeMap map) {
		this.map = map;
	}

	public ExtendedType getType(Class<?> objectClass) {
		logger.warn("Haven't found suitable ExtendedType for class '" + objectClass.getCanonicalName()
				+ "'. Most likely you need to define a custom ExtendedType.");

		if (Serializable.class.isAssignableFrom(objectClass)) {
			logger.warn("SerializableType will be used for type conversion.");

			// using a binary stream delegate instead of byte[] may actually
			// speed up things in some dbs, but at least byte[] type works consistently
			// across adapters...

			// note - can't use "getRegisteredType" as it causes infinite recursion
			@SuppressWarnings("unchecked")
			ExtendedType<byte[]> bytesType = map.getExplictlyRegisteredType("byte[]");
			return new SerializableType(objectClass, bytesType);
		}

		return null;
	}

	/**
	 * A serialization wrapper on top of byte[] ExtendedType
	 */
	final class SerializableType extends ExtendedTypeDecorator<Object, byte[]> {

		private Class<?> javaClass;

		SerializableType(Class<?> javaClass, ExtendedType<byte[]> bytesType) {
			super(bytesType);
			this.javaClass = javaClass;
		}

		@Override
		public String getClassName() {
			return javaClass.getCanonicalName();
		}

		@Override
		byte[] fromJavaObject(Object object) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream() {
				// avoid unneeded array copy...
				@Override
				public synchronized byte[] toByteArray() {
					return buf;
				}
			};

			try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
				out.writeObject(object);
			} catch (Exception e) {
				throw new CayenneRuntimeException("Error serializing object", e);
			}

			return bytes.toByteArray();
		}

		@Override
		Object toJavaObject(byte[] bytes) {
			try {
				return bytes != null && bytes.length > 0
						? new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject() : null;
			} catch (Exception e) {
				throw new CayenneRuntimeException("Error deserializing object", e);
			}
		}
	}
}
