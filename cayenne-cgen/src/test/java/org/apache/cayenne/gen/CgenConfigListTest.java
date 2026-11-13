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
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.project.cgen.CgenDescriptor;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CgenConfigListTest {

    private DataMap dataMap;
    private CgenConfigList configurations;

    @BeforeEach
    public void createList() {
        dataMap = new DataMap("test");
        configurations = new CgenConfigList(dataMap);
    }

    @Test
    public void empty() {
        assertTrue(configurations.isEmpty());
        assertTrue(configurations.getAll().isEmpty());
        assertTrue(configurations.getNames().isEmpty());
        assertNull(configurations.getByName(CgenConfigList.DEFAULT_CONFIG_NAME));
        assertFalse(configurations.isExist(CgenConfigList.DEFAULT_CONFIG_NAME));
    }

    @Test
    public void add(@TempDir Path tmp) throws Exception {
        ObjEntity artist = new ObjEntity("Artist");
        artist.setClassName("org.example.Artist");
        dataMap.addObjEntity(artist);
        dataMap.setConfigurationSource(new URLResource(tmp.resolve("test.map.xml").toUri().toURL()));

        CgenConfiguration configuration = configuration("Custom");
        configurations.add(configuration);

        assertEquals(List.of(configuration.getDescriptor()), dataMap.getCgenDescriptors());
        assertEquals(List.of(configuration), configurations.getAll());
        assertSame(configuration, configurations.getByName("Custom"));
        assertTrue(configurations.isExist("Custom"));
        assertFalse(configurations.isEmpty());

        assertSame(dataMap, configuration.getDataMap());
        assertEquals(Set.of("Artist"), configuration.getEntities());
        assertEquals(tmp, configuration.getRootPath());
    }

    @Test
    public void addTwice() {
        CgenConfiguration configuration = configuration("Custom");
        configurations.add(configuration);
        configurations.add(configuration);

        assertEquals(List.of(configuration.getDescriptor()), dataMap.getCgenDescriptors());
        assertEquals(List.of(configuration), configurations.getAll());
    }

    @Test
    public void addConfigurationOfAnotherDataMap() {
        CgenConfiguration configuration = CgenConfiguration.createDefault(new DataMap("other"), null);

        assertThrows(IllegalArgumentException.class, () -> configurations.add(configuration));
        assertTrue(dataMap.getCgenDescriptors().isEmpty());
    }

    @Test
    public void addDefaultConfiguration() {
        dataMap.addObjEntity(new ObjEntity("Generic"));
        CgenConfiguration configuration = CgenConfiguration.createDefault(dataMap, null);
        configurations.add(configuration);

        assertSame(configuration, configurations.getByName(CgenConfigList.DEFAULT_CONFIG_NAME));
        assertTrue(configuration.getEntities().isEmpty(), "generic entities are not selected by default");
    }

    @Test
    public void settingsOfTheDataMap() {
        CgenDescriptor first = descriptor("First");
        CgenDescriptor second = descriptor("Second");
        dataMap.addCgenDescriptor(first);
        dataMap.addCgenDescriptor(second);

        assertEquals(List.of("First", "Second"), configurations.getNames());
        List<CgenConfiguration> all = configurations.getAll();
        assertEquals(2, all.size());
        assertSame(first, all.get(0).getDescriptor());
        assertSame(second, all.get(1).getDescriptor());
        assertSame(dataMap, all.get(0).getDataMap());
    }

    @Test
    public void sameConfiguration() {
        dataMap.addCgenDescriptor(descriptor("Custom"));

        CgenConfiguration configuration = configurations.getByName("Custom");
        configuration.setEncoding("UTF-16");

        assertSame(configuration, configurations.getByName("Custom"));
        assertSame(configuration, configurations.getAll().get(0));
        assertEquals("UTF-16", configurations.getByName("Custom").getEncoding());
    }

    @Test
    public void removeByName() {
        CgenConfiguration custom = configuration("Custom");
        CgenConfiguration other = configuration("Other");
        configurations.add(custom);
        configurations.add(other);

        configurations.removeByName("Custom");

        assertEquals(List.of(other.getDescriptor()), dataMap.getCgenDescriptors());
        assertEquals(List.of(other), configurations.getAll());
        assertFalse(configurations.isExist("Custom"));
    }

    @Test
    public void sameName() {
        CgenConfiguration first = configuration("Custom");
        CgenConfiguration second = configuration("Custom");
        configurations.add(first);
        configurations.add(second);

        assertEquals(List.of(first, second), configurations.getAll());
        assertSame(first, configurations.getByName("Custom"));

        configurations.removeByName("Custom");
        assertTrue(dataMap.getCgenDescriptors().isEmpty());
    }

    private static CgenConfiguration configuration(String name) {
        CgenConfiguration configuration = new CgenConfiguration();
        configuration.setName(name);
        return configuration;
    }

    private static CgenDescriptor descriptor(String name) {
        CgenDescriptor descriptor = new CgenDescriptor();
        descriptor.setName(name);
        return descriptor;
    }
}
