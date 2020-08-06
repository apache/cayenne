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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;

/**
 * @since 4.0
 */
public class ValueObjectTypeFactory implements ExtendedTypeFactory {

    ValueObjectTypeRegistry valueObjectTypeRegistry;

    private ExtendedTypeMap map;

    public ValueObjectTypeFactory(ExtendedTypeMap map, ValueObjectTypeRegistry valueObjectTypeRegistry) {
        this.map = map;
        this.valueObjectTypeRegistry = valueObjectTypeRegistry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ExtendedType<? extends ValueObjectType> getType(Class<?> objectClass) {

        ValueObjectType<?, ?> valueObjectType = valueObjectTypeRegistry.getValueType(objectClass);
        if(valueObjectType == null) {
            return null;
        }
        ExtendedType<?> decorator = map.getExplictlyRegisteredType(valueObjectType.getTargetType().getCanonicalName());

        return new ExtendedTypeConverter(decorator, valueObjectType);
    }

    static class ExtendedTypeConverter<T, E> implements ExtendedType<T> {

        ExtendedType<E> extendedType;
        ValueObjectType<T, E> valueObjectType;

        ExtendedTypeConverter(ExtendedType<E> extendedType, ValueObjectType<T, E> valueObjectType) {
            this.extendedType = Objects.requireNonNull(extendedType);
            this.valueObjectType = Objects.requireNonNull(valueObjectType);
        }

        @Override
        public String getClassName() {
            return valueObjectType.getValueType().getName();
        }

        protected T toJavaObject(E materializedValue) {
            if(materializedValue == null) {
                return null;
            }
            return valueObjectType.toJavaObject(materializedValue);
        }

        @Override
        public T materializeObject(CallableStatement rs, int index, int type) throws Exception {
            return toJavaObject(extendedType.materializeObject(rs, index, type));
        }

        @Override
        public T materializeObject(ResultSet rs, int index, int type) throws Exception {
            return toJavaObject(extendedType.materializeObject(rs, index, type));
        }

        @Override
        public void setJdbcObject(PreparedStatement statement, T value, int pos, int type, int precision) throws Exception {
            E dbValue = value == null ? null : valueObjectType.fromJavaObject(value);
            extendedType.setJdbcObject(statement, dbValue, pos, type, precision);
        }

        @Override
        public String toString(T value) {
            if (value == null) {
                return "NULL";
            }

            return valueObjectType.toCacheKey(value);
        }
    }

}
