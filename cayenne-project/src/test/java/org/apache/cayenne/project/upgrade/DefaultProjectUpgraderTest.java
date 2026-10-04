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

package org.apache.cayenne.project.upgrade;

import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.upgrade.UpgradeType;
import org.apache.cayenne.configuration.upgrade.ConfigurationUpgrader;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.DefaultHandlerFactory;
import org.apache.cayenne.configuration.xml.HandlerFactory;
import org.apache.cayenne.configuration.xml.NoopDataChannelMetaData;
import org.apache.cayenne.configuration.xml.XMLDataChannelDescriptorLoader;
import org.apache.cayenne.configuration.xml.XMLDataMapLoader;
import org.apache.cayenne.configuration.xml.XMLReaderProvider;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.ProjectModule;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultProjectUpgraderTest {

    @TempDir
    public File tempDir;

    private Injector injector;

    @BeforeEach
    public void createInjector() {
        Module testModule = binder -> {
            binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
            binder.bind(ConfigurationUpgrader.class).to(ConfigurationUpgrader.class);
            binder.bind(DataChannelDescriptorLoader.class).to(XMLDataChannelDescriptorLoader.class);
            binder.bind(HandlerFactory.class).to(DefaultHandlerFactory.class);
            binder.bind(DataChannelMetaData.class).to(NoopDataChannelMetaData.class);
            binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();
        };
        injector = DIBootstrap.createInjector(new ProjectModule(), testModule);
    }

    @Test
    public void checkUpgradeNeeded() throws Exception {
        ProjectUpgrader upgrader = injector.getInstance(ProjectUpgrader.class);

        PreUpgradeState state = upgrader.checkUpgradeNeeded(projectWithVersion("5"));
        assertEquals(UpgradeType.INTERMEDIATE_UPGRADE_NEEDED, state.requiredUpgrade());
        assertEquals("5", state.projectVersion());
        assertEquals("14", state.supportedVersion());
        assertEquals("9", state.intermediateUpgradeVersion());

        state = upgrader.checkUpgradeNeeded(projectWithVersion("11"));
        assertEquals(UpgradeType.UPGRADE_NEEDED, state.requiredUpgrade());
        assertEquals("11", state.projectVersion());

        assertEquals(UpgradeType.UPGRADE_NOT_NEEDED, upgrader.checkUpgradeNeeded(projectWithVersion("14")).requiredUpgrade());
        assertEquals(UpgradeType.DOWNGRADE_NEEDED, upgrader.checkUpgradeNeeded(projectWithVersion("15")).requiredUpgrade());
    }

    @Test
    public void upgrade() throws Exception {
        File projectFile = copyToTemp("v12/cayenne-project1.xml");
        File mapFile = copyToTemp("v12/map1.map.xml");
        File graphFile = copyToTemp("v12/project1.graph.xml");
        Resource resource = new URLResource(projectFile.toURI().toURL());

        ProjectUpgrader upgrader = injector.getInstance(ProjectUpgrader.class);
        assertEquals(UpgradeType.UPGRADE_NEEDED, upgrader.checkUpgradeNeeded(resource).requiredUpgrade());

        PostUpgradeState state = upgrader.upgrade(resource);

        assertEquals(2, state.messages().size(), state.messages().toString());
        assertTrue(state.messages().get(0).contains("'graph' diagram layout"), state.messages().toString());
        assertTrue(state.messages().get(1).contains("DataNode 'node1'"), state.messages().toString());

        // files are rewritten in the current version, the obsolete graph file is deleted
        String project = Files.readString(projectFile.toPath());
        assertFalse(project.contains("project-version"), project);
        assertTrue(project.contains("http://cayenne.apache.org/schema/14/project"), project);
        assertFalse(project.contains("<node"), project);
        assertFalse(project.contains("include"), project);

        String map = Files.readString(mapFile.toPath());
        assertFalse(map.contains("project-version"), map);
        assertTrue(map.contains("http://cayenne.apache.org/schema/14/dataMap"), map);

        assertFalse(graphFile.exists(), "graph file must be deleted");

        // the upgraded project loads without an upgrade
        assertEquals(UpgradeType.UPGRADE_NOT_NEEDED, upgrader.checkUpgradeNeeded(resource).requiredUpgrade());
        ConfigurationTree<DataChannelDescriptor> tree = injector.getInstance(DataChannelDescriptorLoader.class).load(resource);
        assertEquals(1, tree.getRootNode().getDataMaps().size());
        DataMap dataMap = tree.getRootNode().getDataMaps().iterator().next();
        assertEquals(2, dataMap.getDbEntities().size());
    }

    private Resource projectWithVersion(String version) throws IOException {
        File file = new File(tempDir, "cayenne-v" + version + ".xml");
        // the version is a part of the namespace starting with version 14, and an attribute before that
        String root = Integer.parseInt(version) < 14
                ? "<domain project-version=\"" + version + "\"/>"
                : "<project xmlns=\"http://cayenne.apache.org/schema/" + version + "/project\"/>";
        Files.writeString(file.toPath(), root);
        return new URLResource(file.toURI().toURL());
    }

    private File copyToTemp(String resource) throws IOException {
        File target = new File(tempDir, resource.substring(resource.lastIndexOf('/') + 1));
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            Files.copy(in, target.toPath());
        }
        return target;
    }
}
