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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class stores set of CgenConfigurations using for classes generation
 *
 * @since 4.2
 */
public class CgenConfigList {

    public static final String DEFAULT_CONFIG_NAME = "Default";
    private final List<CgenConfiguration> configurations;

    public CgenConfigList() {
        this.configurations = new ArrayList<>();
    }

    public CgenConfiguration getByName(String name) {
        return configurations.stream().filter(config -> config.getName().equals(name)).findFirst().orElse(null);
    }

    public void removeByName(String name) {
        configurations.removeIf(configuration -> configuration.getName().equals(name));
    }

    public void add(CgenConfiguration configuration) {
        configurations.add(configuration);
    }

    public List<String> getNames() {
        return configurations.stream()
                .map(CgenConfiguration::getName)
                .collect(Collectors.toList());
    }

    public List<CgenConfiguration> getAll() {
        return configurations;
    }

    public boolean isExist(String name) {
        return configurations.stream().anyMatch(c -> name.equals(c.getName()));
    }
}
