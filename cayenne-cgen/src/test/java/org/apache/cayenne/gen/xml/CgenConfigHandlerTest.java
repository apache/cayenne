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

package org.apache.cayenne.gen.xml;

import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.gen.CgenConfigList;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.tools.ToolsInjectorBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.helpers.NOPLogger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Checks that a {@code <cgen>} block survives a load, element for element.
 */
public class CgenConfigHandlerTest {

    @TempDir
    static Path tempDir;

    private static CgenConfiguration configuration;

    @BeforeAll
    public static void loadConfiguration() throws Exception {
        Path mapFile = tempDir.resolve("cgenRoundTrip.map.xml");
        try (InputStream in = CgenConfigHandlerTest.class.getResourceAsStream("/cgenRoundTrip.map.xml")) {
            assertNotNull(in, "test DataMap is missing from the classpath");
            Files.copy(in, mapFile);
        }

        Injector injector = new ToolsInjectorBuilder()
                .addModule(new ToolsModule(NOPLogger.NOP_LOGGER))
                .create();

        DataMap dataMap = injector.getInstance(DataMapLoader.class)
                .load(new URLResource(mapFile.toUri().toURL()));

        CgenConfigList configurations = injector.getInstance(DataChannelMetaData.class)
                .get(dataMap, CgenConfigList.class);
        assertNotNull(configurations, "no cgen configuration was loaded");

        configuration = configurations.getByName("Default");
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
}
