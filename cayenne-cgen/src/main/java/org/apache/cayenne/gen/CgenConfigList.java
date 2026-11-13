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


package org.apache.cayenne.gen;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.cgen.CgenDescriptor;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A live view of the class generation settings of a DataMap ({@link DataMap#getCgenDescriptors()}) as
 * CgenConfigurations. The list always wraps the same settings in the same configuration, keeping its state that is
 * not stored in the DataMap, while another list of the same DataMap has configurations of its own.
 *
 * @since 4.2
 */
public class CgenConfigList {

    public static final String DEFAULT_CONFIG_NAME = CgenDescriptor.DEFAULT_NAME;

    private final DataMap dataMap;
    private final Map<CgenDescriptor, CgenConfiguration> configurations;

    /**
     * @since 6.0
     */
    public CgenConfigList(DataMap dataMap) {
        this.dataMap = Objects.requireNonNull(dataMap);
        this.configurations = new IdentityHashMap<>();
    }

    public CgenConfiguration getByName(String name) {
        return dataMap.getCgenDescriptors().stream()
                .filter(descriptor -> Objects.equals(descriptor.getName(), name))
                .findFirst()
                .map(this::configuration)
                .orElse(null);
    }

    /**
     * Removes all the configurations with the given name.
     */
    public void removeByName(String name) {
        for (CgenDescriptor descriptor : List.copyOf(dataMap.getCgenDescriptors())) {
            if (Objects.equals(descriptor.getName(), name)) {
                dataMap.removeCgenDescriptor(descriptor);
                configurations.remove(descriptor);
            }
        }
    }

    /**
     * Adds the configuration settings to the DataMap.
     *
     * @throws IllegalArgumentException if the configuration is tied to another DataMap
     */
    public void add(CgenConfiguration configuration) {
        DataMap configurationDataMap = configuration.getDataMap();
        if (configurationDataMap != null && configurationDataMap != dataMap) {
            throw new IllegalArgumentException("Configuration '%s' of DataMap '%s' can't be added to DataMap '%s'"
                    .formatted(configuration.getName(), configurationDataMap.getName(), dataMap.getName()));
        }

        configuration.attachTo(dataMap);
        dataMap.addCgenDescriptor(configuration.getDescriptor());
        configurations.put(configuration.getDescriptor(), configuration);
    }

    public List<String> getNames() {
        return dataMap.getCgenDescriptors().stream()
                .map(CgenDescriptor::getName)
                .toList();
    }

    public List<CgenConfiguration> getAll() {
        return dataMap.getCgenDescriptors().stream()
                .map(this::configuration)
                .toList();
    }

    public boolean isExist(String name) {
        return dataMap.getCgenDescriptors().stream()
                .anyMatch(descriptor -> Objects.equals(descriptor.getName(), name));
    }

    /**
     * @since 6.0
     */
    public boolean isEmpty() {
        return dataMap.getCgenDescriptors().isEmpty();
    }

    private CgenConfiguration configuration(CgenDescriptor descriptor) {
        return configurations.computeIfAbsent(descriptor, d -> new CgenConfiguration(d, dataMap));
    }
}
