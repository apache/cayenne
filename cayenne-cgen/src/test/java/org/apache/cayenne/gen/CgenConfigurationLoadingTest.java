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

import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.DataMapLoader;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.tools.ToolsInjectorBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.helpers.NOPLogger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Checks that a {@code <cgen>} block survives a load, element for element.
 */
public class CgenConfigurationLoadingTest {

    @TempDir
    static Path tempDir;

    private static CgenConfiguration configuration;

    @BeforeAll
    public static void loadConfiguration() throws Exception {
        configuration = load("cgenRoundTrip.map.xml").getByName("Default");
        assertNotNull(configuration);
    }

    @Test
    public void embeddableSuperTemplate() {
        assertEquals("custom embeddable superclass template", configuration.getEmbeddableSuperTemplate().getData());
        assertEquals(TemplateType.DATAMAP_SUBCLASS.defaultTemplate().getData(),
                configuration.getDataMapTemplate().getData());
    }

    @Test
    public void externalToolConfig() {
        assertEquals("tools.properties", configuration.getExternalToolConfig());
    }

    @Test
    public void destDirIsResolved() {
        assertEquals(tempDir, configuration.getRootPath());
        assertEquals(tempDir.resolve("../java").normalize(), configuration.requireOutputDirectory());
    }

    /**
     * Inline templates are registered with the template engine by type, so a wrong type would make one replace another.
     */
    @Test
    public void inlineTemplatesLoadWithTheTypeOfTheirTag() throws Exception {
        CgenConfigList configurations = load("cgenRoundTripAll.map.xml");
        assertEquals(List.of("Custom", "Default"), configurations.getNames());
        CgenConfiguration custom = configurations.getByName("Custom");
        assertNotNull(custom);

        assertEquals(TemplateType.ENTITY_SUBCLASS, custom.getTemplate().getType());
        assertEquals(TemplateType.ENTITY_SUPERCLASS, custom.getSuperTemplate().getType());
        assertEquals(TemplateType.EMBEDDABLE_SUBCLASS, custom.getEmbeddableTemplate().getType());
        assertEquals(TemplateType.EMBEDDABLE_SUPERCLASS, custom.getEmbeddableSuperTemplate().getType());
        assertEquals(TemplateType.DATAMAP_SUBCLASS, custom.getDataMapTemplate().getType());
        assertEquals(TemplateType.DATAMAP_SUPERCLASS, custom.getDataMapSuperTemplate().getType());
    }

    private static CgenConfigList load(String resource) throws Exception {
        Path mapFile = tempDir.resolve(resource);
        try (InputStream in = CgenConfigurationLoadingTest.class.getResourceAsStream("/" + resource)) {
            assertNotNull(in, "test DataMap is missing from the classpath");
            Files.copy(in, mapFile);
        }

        Injector injector = new ToolsInjectorBuilder()
                .addModule(new ToolsModule(NOPLogger.NOP_LOGGER))
                .create();

        DataMap dataMap = injector.getInstance(DataMapLoader.class).load(new URLResource(mapFile.toUri().toURL()));

        assertFalse(dataMap.getCgenDescriptors().isEmpty(), "no cgen configuration was loaded");
        return new CgenConfigList(dataMap);
    }
}
