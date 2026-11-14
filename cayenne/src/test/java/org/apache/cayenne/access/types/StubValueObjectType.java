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

/**
 * A ValueObjectType that only declares its value and target types, for the tests of type registration and lookup.
 * Conversions are not supported.
 */
class StubValueObjectType<V, T> implements ValueObjectType<V, T> {

    private final Class<V> valueType;
    private final Class<T> targetType;

    StubValueObjectType(Class<V> valueType, Class<T> targetType) {
        this.valueType = valueType;
        this.targetType = targetType;
    }

    @Override
    public Class<T> getTargetType() {
        return targetType;
    }

    @Override
    public Class<V> getValueType() {
        return valueType;
    }

    @Override
    public V toJavaObject(T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T fromJavaObject(V object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toCacheKey(V object) {
        throw new UnsupportedOperationException();
    }
}
