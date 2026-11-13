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
import org.apache.cayenne.projecttools.ProjectSaver;
import org.apache.cayenne.projecttools.ProjectToolsModule;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.tools.ToolsInjectorBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.helpers.NOPLogger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CgenRoundTripTest {

    @TempDir
    Path tempDir;

    @Test
    public void roundTrip() throws Exception {
        assertEquals("""
                <?xml version="1.0" encoding="utf-8"?>
                <dataMap xmlns="http://cayenne.apache.org/schema/15/dataMap"
                \t xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                \t xsi:schemaLocation="http://cayenne.apache.org/schema/15/dataMap \
                https://cayenne.apache.org/schema/15/dataMap.xsd">
                \t<objEntity name="Artist" className="org.example.Artist"/>
                \t<cgen>
                \t\t<name>Default</name>
                \t\t<destDir>../java</destDir>
                \t\t<mode>entity</mode>
                \t\t<template>templates/v4_1/subclass.vm</template>
                \t\t<superTemplate>templates/v4_1/superclass.vm</superTemplate>
                \t\t<embeddableTemplate>templates/v4_1/embeddable-subclass.vm</embeddableTemplate>
                \t\t<embeddableSuperTemplate>custom embeddable superclass template</embeddableSuperTemplate>
                \t\t<dataMapTemplate>templates/v4_1/datamap-subclass.vm</dataMapTemplate>
                \t\t<dataMapSuperTemplate>templates/v4_1/datamap-superclass.vm</dataMapSuperTemplate>
                \t\t<outputPattern>*.java</outputPattern>
                \t\t<makePairs>true</makePairs>
                \t\t<usePkgPath>true</usePkgPath>
                \t\t<overwrite>false</overwrite>
                \t\t<createPropertyNames>false</createPropertyNames>
                \t\t<createPKProperties>true</createPKProperties>
                \t\t<externalToolConfig>tools.properties</externalToolConfig>
                \t</cgen>
                </dataMap>
                """, loadAndSave("cgenRoundTrip.map.xml"));
    }

    /**
     * The excluded names are saved sorted, without the names missing from the DataMap.
     */
    @Test
    public void roundTripAllSettings() throws Exception {
        assertEquals("""
                <?xml version="1.0" encoding="utf-8"?>
                <dataMap xmlns="http://cayenne.apache.org/schema/15/dataMap"
                \t xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                \t xsi:schemaLocation="http://cayenne.apache.org/schema/15/dataMap \
                https://cayenne.apache.org/schema/15/dataMap.xsd">
                \t<embeddable className="org.example.Address"/>
                \t<embeddable className="org.example.Phone"/>
                \t<objEntity name="Artist" className="org.example.Artist"/>
                \t<objEntity name="Gallery" className="org.example.Gallery"/>
                \t<objEntity name="Painting" className="org.example.Painting"/>
                \t<cgen>
                \t\t<name>Custom</name>
                \t\t<excludeEntities>Artist,Painting</excludeEntities>
                \t\t<excludeEmbeddables>org.example.Phone</excludeEmbeddables>
                \t\t<destDir>../java</destDir>
                \t\t<mode>all</mode>
                \t\t<template>subclass template</template>
                \t\t<superTemplate>superclass template</superTemplate>
                \t\t<embeddableTemplate>embeddable subclass template</embeddableTemplate>
                \t\t<embeddableSuperTemplate>embeddable superclass template</embeddableSuperTemplate>
                \t\t<dataMapTemplate>datamap subclass template</dataMapTemplate>
                \t\t<dataMapSuperTemplate>datamap superclass template</dataMapSuperTemplate>
                \t\t<outputPattern>*.groovy</outputPattern>
                \t\t<makePairs>false</makePairs>
                \t\t<usePkgPath>false</usePkgPath>
                \t\t<overwrite>true</overwrite>
                \t\t<createPropertyNames>true</createPropertyNames>
                \t\t<superPkg>org.example.auto</superPkg>
                \t\t<createPKProperties>false</createPKProperties>
                \t\t<externalToolConfig>tools.properties</externalToolConfig>
                \t</cgen>
                \t<cgen>
                \t\t<name>Default</name>
                \t\t<destDir>.</destDir>
                \t\t<mode>entity</mode>
                \t\t<template>templates/v4_1/subclass.vm</template>
                \t\t<superTemplate>templates/v4_1/superclass.vm</superTemplate>
                \t\t<embeddableTemplate>templates/v4_1/embeddable-subclass.vm</embeddableTemplate>
                \t\t<embeddableSuperTemplate>templates/v4_1/embeddable-superclass.vm</embeddableSuperTemplate>
                \t\t<dataMapTemplate>templates/v4_1/datamap-subclass.vm</dataMapTemplate>
                \t\t<dataMapSuperTemplate>templates/v4_1/datamap-superclass.vm</dataMapSuperTemplate>
                \t\t<outputPattern>*.java</outputPattern>
                \t\t<makePairs>true</makePairs>
                \t\t<usePkgPath>true</usePkgPath>
                \t\t<overwrite>false</overwrite>
                \t\t<createPropertyNames>false</createPropertyNames>
                \t\t<createPKProperties>true</createPKProperties>
                \t</cgen>
                </dataMap>
                """, loadAndSave("cgenRoundTripAll.map.xml"));
    }

    @Test
    public void singleClassTemplates() throws Exception {
        Path mapFile = copy("cgenRoundTrip.map.xml");
        Injector injector = injector();
        DataMap dataMap = load(injector, mapFile);

        CgenConfiguration configuration = defaultConfiguration(dataMap);
        configuration.setMakePairs(false);
        configuration.setTemplate(TemplateType.ENTITY_SINGLE_CLASS.defaultTemplate());
        configuration.setEmbeddableTemplate(TemplateType.EMBEDDABLE_SINGLE_CLASS.defaultTemplate());
        configuration.setDataMapTemplate(TemplateType.DATAMAP_SINGLE_CLASS.defaultTemplate());
        save(injector, dataMap);

        String saved = read(mapFile);
        assertTrue(saved.contains("\t\t<template>templates/v4_1/singleclass.vm</template>\n"), saved);
        assertTrue(saved.contains(
                "\t\t<embeddableTemplate>templates/v4_1/embeddable-singleclass.vm</embeddableTemplate>\n"), saved);
        assertTrue(saved.contains(
                "\t\t<dataMapTemplate>templates/v4_1/datamap-singleclass.vm</dataMapTemplate>\n"), saved);

        Injector reloadInjector = injector();
        DataMap reloaded = load(reloadInjector, mapFile);
        assertSingleClassTemplates(defaultConfiguration(reloaded));

        save(reloadInjector, reloaded);
        assertEquals(saved, read(mapFile));

        Injector secondReloadInjector = injector();
        assertSingleClassTemplates(defaultConfiguration(load(secondReloadInjector, mapFile)));
    }

    private static void assertSingleClassTemplates(CgenConfiguration configuration) {
        assertFalse(configuration.isMakePairs());
        assertEquals(TemplateType.ENTITY_SINGLE_CLASS.defaultTemplate(), configuration.getTemplate());
        assertEquals(TemplateType.EMBEDDABLE_SINGLE_CLASS.defaultTemplate(), configuration.getEmbeddableTemplate());
        assertEquals(TemplateType.DATAMAP_SINGLE_CLASS.defaultTemplate(), configuration.getDataMapTemplate());
    }

    private String loadAndSave(String resource) throws Exception {
        Path mapFile = copy(resource);
        Injector injector = injector();
        save(injector, load(injector, mapFile));
        return read(mapFile);
    }

    private Path copy(String resource) throws Exception {
        Path mapFile = tempDir.resolve(resource);
        try (InputStream in = getClass().getResourceAsStream("/" + resource)) {
            assertNotNull(in, "test DataMap is missing from the classpath");
            Files.copy(in, mapFile);
        }
        return mapFile;
    }

    private static Injector injector() {
        return new ToolsInjectorBuilder()
                .addModule(new ToolsModule(NOPLogger.NOP_LOGGER))
                .addModule(new ProjectToolsModule())
                .create();
    }

    private static DataMap load(Injector injector, Path mapFile) throws Exception {
        return injector.getInstance(DataMapLoader.class).load(new URLResource(mapFile.toUri().toURL()));
    }

    private static void save(Injector injector, DataMap dataMap) {
        injector.getInstance(ProjectSaver.class).save(dataMap, null);
    }

    private static CgenConfiguration defaultConfiguration(DataMap dataMap) {
        CgenConfiguration configuration = new CgenConfigList(dataMap).getByName("Default");
        assertNotNull(configuration);
        return configuration;
    }

    private static String read(Path mapFile) throws Exception {
        return Files.readString(mapFile, StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
    }
}
