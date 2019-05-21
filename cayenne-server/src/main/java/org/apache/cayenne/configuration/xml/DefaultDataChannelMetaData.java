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

package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.configuration.ConfigurationNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 *     Default implementation of {@link DataChannelMetaData} that stores data in Map.
 * </p>
 * <p>
 *     This implementation is thread safe.
 * </p>
 *
 * @see NoopDataChannelMetaData
 * @since 4.1
 */
public class DefaultDataChannelMetaData implements DataChannelMetaData {

    private Map<ConfigurationNode, Map<Class<?>, Object>> map;

    public DefaultDataChannelMetaData() {
        map = new ConcurrentHashMap<>();
    }

    /**
     * value.getClass() will be used under the hood to associate data with the key object.
     *
     * @param key object for which we want to store data
     * @param value data to store
     */
    @Override
    public void add(ConfigurationNode key, Object value) {
        if(key == null || value == null) {
            return;
        }

        Map<Class<?>, Object> data = map.get(key);
        if(data == null) {
            data = new ConcurrentHashMap<>();
            Map<Class<?>, Object> old = map.put(key, data);
            // extra check in case if someone was fast enough
            if(old != null) {
                data.putAll(old);
            }
        }
        data.put(value.getClass(), value);
    }

    /**
     * If either key or value is {@code null} then {@code null} will be returned.
     *
     * @param key object for wich we want meta data
     * @param type meta data type class
     * @param <T> data type
     * @return value or {@code null}
     */
    @Override
    public <T> T get(ConfigurationNode key, Class<T> type) {
        if(key == null || type == null) {
            return null;
        }

        Map<Class<?>, Object> data = map.get(key);
        if(data == null) {
            return null;
        }

        return type.cast(data.get(type));
    }

    /**
     *
     * @param key object for wich we want meta data
     * @param type meta data type class
     * @param <T> data type
     * @return removed value or {@code null}
     */
    @Override
    public <T> T remove(ConfigurationNode key, Class<T> type) {
        if(key == null || type == null) {
            return null;
        }

        Map<Class<?>, Object> data = map.get(key);
        if(data == null) {
            return null;
        }

        return type.cast(data.remove(type));
    }
}
