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
package org.apache.cayenne.dba;

import org.apache.cayenne.di.DIRuntimeException;

import java.util.Map;
import java.util.Objects;

/**
 * An injectable provider that returns a given service in a context of a specific {@link DbAdapter}.
 * This allows modules to create adapter-specific extensions without altering DbAdapter API.
 *
 * @since 4.0
 */
public class PerAdapterProvider<T> {

    private Map<String, T> perAdapterValues;
    private T defaultValue;

    public PerAdapterProvider(Map<String, T> perAdapterValues, T defaultValue) {
        this.perAdapterValues = Objects.requireNonNull(perAdapterValues);
        this.defaultValue = Objects.requireNonNull(defaultValue);
    }

    public T get(DbAdapter adapter) throws DIRuntimeException {
        T t = perAdapterValues.get(adapter.unwrap().getClass().getName());
        return t != null ? t : defaultValue;
    }
}
