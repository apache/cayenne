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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.Project;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.upgrade.ProjectFileUpgrader;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class XMLProjectLoaderTest {

    private Injector injector;

    @TempDir
    public File tempDir;

    @BeforeEach
    public void setUp() {
        Module testModule = binder -> {
            binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
            binder.bind(ProjectFileUpgrader.class).to(ProjectFileUpgrader.class);
            binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            binder.bind(HandlerFactory.class).to(DefaultHandlerFactory.class);
            binder.bind(ProjectMetaData.class).to(NoopProjectMetaData.class);
            binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();
        };

        this.injector = DIBootstrap.createInjector(testModule);
    }

    @Test
    public void loadEmpty() {

        // create and initialize loader instance to test
        XMLProjectLoader loader = new XMLProjectLoader();
        injector.injectMembers(loader);

        String testConfigName = "testConfig1";

        URL url = getClass().getResource("cayenne-" + testConfigName + ".xml");
        Project project = loader.load(new URLResource(url));

        assertNotNull(project);
        assertEquals(testConfigName, project.getName());
    }

    @Test
    public void load_MissingConfig() throws Exception {

        // create and initialize loader instance to test
        XMLProjectLoader loader = new XMLProjectLoader();
        injector.injectMembers(loader);

        assertThrows(ConfigurationException.class, () ->
                loader.load(new URLResource(new URL("file:///no_such_resource"))));
    }

    @Test
    public void loadOldVersion() {
        XMLProjectLoader loader = new XMLProjectLoader();
        injector.injectMembers(loader);

        // version 9, upgraded in memory
        URL url = getClass().getResource("cayenne-testConfig4.xml");
        Project project = loader.load(new URLResource(url));
        assertEquals("testConfig4", project.getName());
        assertTrue(project.getDataMaps().isEmpty());
    }

    @Test
    public void loadOldVersionWithDataMap() {
        XMLProjectLoader loader = new XMLProjectLoader();
        injector.injectMembers(loader);

        // version 9 project and DataMap, both upgraded in memory
        URL url = getClass().getResource("cayenne-testConfig9.xml");
        Project project = loader.load(new URLResource(url));
        assertEquals("testConfig9", project.getName());
        assertEquals(1, project.getDataMaps().size());

        DataMap map = project.getDataMaps().iterator().next();
        assertEquals("testConfigMap9", map.getName());
        assertEquals(2, map.getDbEntity("ARTIST").getAttributes().size());
        assertEquals("org.apache.cayenne.GenericPersistentObject", map.getObjEntity("Artist").getClassName());
        assertEquals(1, map.getObjEntity("Artist").getDeclaredAttributes().size());
        assertInstanceOf(SelectQueryDescriptor.class, map.getQueryDescriptor("ArtistQuery"));
    }

    @Test
    public void loadOldVersionKeepsFiles() throws Exception {
        File projectFile = copyToTemp("cayenne-testConfig10.xml");
        File mapFile = copyToTemp("testConfigMap10.map.xml");
        File graphFile = copyToTemp("testConfig10.graph.xml");
        byte[] projectBytes = Files.readAllBytes(projectFile.toPath());
        byte[] mapBytes = Files.readAllBytes(mapFile.toPath());

        XMLProjectLoader loader = new XMLProjectLoader();
        injector.injectMembers(loader);

        // version 11 with a graph layout, dropped by the upgrade to version 12
        Project project = loader.load(new URLResource(projectFile.toURI().toURL()));
        assertEquals(1, project.getDataMaps().size());
        DataMap map = project.getDataMaps().iterator().next();
        assertNotNull(map.getDbEntity("db_entity"));
        assertNotNull(map.getObjEntity("Entity"));

        assertTrue(graphFile.exists(), "in-memory upgrade must not delete files");
        assertArrayEquals(projectBytes, Files.readAllBytes(projectFile.toPath()), "project file must not change");
        assertArrayEquals(mapBytes, Files.readAllBytes(mapFile.toPath()), "DataMap file must not change");
    }

    @Test
    public void loadDestructiveUpgrade() {
        XMLProjectLoader loader = new XMLProjectLoader();
        injector.injectMembers(loader);

        // version 12 with a DataNode that version 13 removes without a replacement
        URL url = getClass().getResource("cayenne-testConfig8.xml");
        ConfigurationException e = assertThrows(ConfigurationException.class, () -> loader.load(new URLResource(url)));
        assertTrue(e.getMessage().contains("DataNode 'node1'"), e.getMessage());
        assertTrue(e.getMessage().contains("CayenneModeler"), e.getMessage());
    }

    @Test
    public void loadNewerVersion() {
        XMLProjectLoader loader = new XMLProjectLoader();
        injector.injectMembers(loader);

        URL url = getClass().getResource("cayenne-testConfig6.xml");
        ConfigurationException e = assertThrows(ConfigurationException.class, () -> loader.load(new URLResource(url)));
        assertTrue(e.getMessage().contains("version 15 is newer"), e.getMessage());
    }

    @Test
    public void loadTooOldVersion() {
        XMLProjectLoader loader = new XMLProjectLoader();
        injector.injectMembers(loader);

        URL url = getClass().getResource("cayenne-testConfig7.xml");
        ConfigurationException e = assertThrows(ConfigurationException.class, () -> loader.load(new URLResource(url)));
        assertTrue(e.getMessage().contains("version 5 is too old"), e.getMessage());
    }

    private File copyToTemp(String resource) throws IOException {
        File target = new File(tempDir, resource);
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            Files.copy(in, target.toPath());
        }
        return target;
    }

    @Test
    public void loadInvalidNamespace() throws Exception {
        XMLProjectLoader loader = new XMLProjectLoader();
        injector.injectMembers(loader);

        URL url = getClass().getResource("cayenne-testConfig5.xml");
        assertThrows(CayenneRuntimeException.class, () -> loader.load(new URLResource(url)));
    }

    @Test
    public void loadDataMap() {

        // create and initialize loader instance to test
        XMLProjectLoader loader = new XMLProjectLoader();
        injector.injectMembers(loader);

        String testConfigName = "testConfig2";
        URL url = getClass().getResource("cayenne-" + testConfigName + ".xml");

        Project project = loader.load(new URLResource(url));

        assertNotNull(project);

        assertEquals(testConfigName, project.getName());

        Collection<DataMap> maps = project.getDataMaps();
        assertEquals(1, maps.size());
        assertEquals("testConfigMap2", maps.iterator().next().getName());
    }

    @Test
    public void loadDataEverything() {

        // create and initialize loader instance to test
        XMLProjectLoader loader = new XMLProjectLoader();
        injector.injectMembers(loader);

        String testConfigName = "testConfig3";
        URL url = getClass().getResource("cayenne-" + testConfigName + ".xml");

        Project project = loader.load(new URLResource(url));

        assertNotNull(project);
        assertEquals(testConfigName, project.getName());

        Collection<DataMap> maps = project.getDataMaps();
        assertEquals(2, maps.size());

        Iterator<DataMap> mapsIt = maps.iterator();

        DataMap map1 = mapsIt.next();
        DataMap map2 = mapsIt.next();

        assertEquals("testConfigMap3_1", map1.getName());
        assertEquals("testConfigMap3_2", map2.getName());
    }
}
