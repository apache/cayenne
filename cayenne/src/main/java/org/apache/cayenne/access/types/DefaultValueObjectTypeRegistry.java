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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.di.Inject;

/**
 * Default implementation of {@link ValueObjectTypeRegistry}
 * @since 4.0
 */
public class DefaultValueObjectTypeRegistry implements ValueObjectTypeRegistry {

    final Map<String, ValueObjectType> typeCache;

    public DefaultValueObjectTypeRegistry(@Inject List<ValueObjectType<?, ?>> valueObjectTypeList) {
        typeCache = new ConcurrentHashMap<>();
        buildTypeCache(valueObjectTypeList);
    }

    private void buildTypeCache(List<ValueObjectType<?, ?>> valueObjectTypeList) {
        for(ValueObjectType<?, ?> valueObjectType : valueObjectTypeList) {
            typeCache.put(valueObjectType.getValueType().getName(), valueObjectType);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> ValueObjectType<T, ?> getValueType(Class<? extends T> valueClass) {
        return typeCache.get(valueClass.getName());
    }
}
