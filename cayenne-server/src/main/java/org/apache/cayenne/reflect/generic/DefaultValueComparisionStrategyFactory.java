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

package org.apache.cayenne.reflect.generic;

import java.io.Serializable;
import java.util.Objects;

import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.ObjAttribute;

/**
 * @since 4.2
 */
public class DefaultValueComparisionStrategyFactory implements ValueComparisionStrategyFactory {

    private static final ValueComparisionStrategy<Object> DEFAULT_STRATEGY = new DefaultValueComparisionStrategy();

    private final ValueObjectTypeRegistry valueObjectTypeRegistry;

    public DefaultValueComparisionStrategyFactory(@Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        this.valueObjectTypeRegistry = valueObjectTypeRegistry;
    }

    @Override
    public ValueComparisionStrategy<Object> getStrategy(ObjAttribute attribute) {
        ValueObjectType<?, ?> valueObjectType = valueObjectTypeRegistry.getValueType(attribute.getJavaClass());
        if(valueObjectType == null) {
            return DEFAULT_STRATEGY;
        } else {
            return new ValueObjectTypeComparisionStrategy(valueObjectType);
        }
    }

    // Using classes instead of lambdas to allow serialization

    @SuppressWarnings({"rawtypes"})
    static class ValueObjectTypeComparisionStrategy implements ValueComparisionStrategy<Object>, Serializable {
        private final ValueObjectType valueObjectType;

        public ValueObjectTypeComparisionStrategy(ValueObjectType<?, ?> valueObjectType) {
            this.valueObjectType = valueObjectType;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object value1, Object value2) {
            return valueObjectType.equals(value1, value2);
        }
    }

    static class DefaultValueComparisionStrategy implements ValueComparisionStrategy<Object>, Serializable {
        @Override
        public boolean equals(Object a, Object b) {
            return Objects.equals(a, b);
        }
    }
}
