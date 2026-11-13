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
package org.apache.cayenne.projecttools;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.project.ConfigurationNameMapper;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectLoader;
import org.apache.cayenne.project.ProjectLoadingMode;
import org.apache.cayenne.project.DataMapLoader;
import org.apache.cayenne.project.DefaultConfigurationNameMapper;
import org.apache.cayenne.project.upgrade.ProjectFileUpgrader;
import org.apache.cayenne.project.xml.XMLProjectLoader;
import org.apache.cayenne.project.xml.XMLDataMapLoader;
import org.apache.cayenne.project.xml.XMLReaderProvider;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.cgen.CgenDescriptor;
import org.apache.cayenne.project.cgen.CgenPaths;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class FileProjectSaverSaveAsTest {

    @TempDir
    public File tempDir;

    @Test
    public void saveAs() throws Exception {

        FileProjectSaver saver = new FileProjectSaver();

        Injector injector = DIBootstrap.createInjector(testModule());
        injector.injectMembers(saver);

        String testConfigName = "PROJECT2";
        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(
                baseUrl + "/cayenne-" + testConfigName + ".xml");

        Resource source = new URLResource(url);
        Project project = injector.getInstance(ProjectLoader.class).load(source);

        saver.saveAs(project, new URLResource(tempDir.toURI().toURL()));

        File rootFile = new File(tempDir, "cayenne-PROJECT2.xml");
        assertTrue(rootFile.exists());
        assertTrue(rootFile.length() > 0);

        File map1File = new File(tempDir, "testProjectMap2_1.map.xml");
        assertTrue(map1File.exists());
        assertTrue(map1File.length() > 0);

        File map2File = new File(tempDir, "testProjectMap2_2.map.xml");
        assertTrue(map2File.exists());
        assertTrue(map2File.length() > 0);
    }

    @Test
    public void saveAs_RecoverFromSaveError() throws Exception {

        FileProjectSaver saver = new FileProjectSaver() {

            @Override
            void saveToTempFile(SaveUnit unit, PrintWriter printWriter) {
                throw new CayenneRuntimeException("Test Exception");
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule());
        injector.injectMembers(saver);

        String testConfigName = "PROJECT2";
        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(
                baseUrl + "/cayenne-" + testConfigName + ".xml");

        Resource source = new URLResource(url);
        Project project = injector.getInstance(ProjectLoader.class).load(source);

        assertEquals(0, tempDir.list().length);

        assertThrows(CayenneRuntimeException.class,
                () -> saver.saveAs(project, new URLResource(tempDir.toURI().toURL())));

        assertEquals(0, tempDir.list().length);
    }

    @Test
    public void saveAs_KeepsCgenOutputDir() throws Exception {
        File oldDir = new File(tempDir, "old");
        File newDir = Files.createDirectories(tempDir.toPath().resolve(Path.of("new", "project"))).toFile();
        Resource source = copyProject("PROJECT2", oldDir, "testProjectMap2_1", "testProjectMap2_2");

        FileProjectSaver saver = new FileProjectSaver();
        Injector injector = DIBootstrap.createInjector(testModule());
        injector.injectMembers(saver);

        Project project = injector.getInstance(ProjectLoader.class).load(source);
        DataMap dataMap = project.getDataMap("testProjectMap2_1");
        CgenDescriptor relative = new CgenDescriptor();
        relative.setName("Relative");
        relative.setDestDir("../java");
        dataMap.addCgenDescriptor(relative);
        CgenDescriptor absolute = new CgenDescriptor();
        absolute.setName("Absolute");
        absolute.setDestDir(new File(tempDir, "generated").getAbsolutePath());
        dataMap.addCgenDescriptor(absolute);
        CgenDescriptor notSet = new CgenDescriptor();
        notSet.setName("NotSet");
        dataMap.addCgenDescriptor(notSet);

        saver.saveAs(project, new URLResource(newDir.toURI().toURL()));

        String map = Files.readString(new File(newDir, "testProjectMap2_1.map.xml").toPath());
        assertTrue(map.contains("<destDir>../../java</destDir>"), map);
        assertTrue(map.contains("<destDir>../../generated</destDir>"), map);
        assertTrue(map.contains("<destDir>../../old</destDir>"), map);

        Path dataMapDir = Path.of(dataMap.getConfigurationSource().getURL().toURI()).getParent();
        assertEquals(newDir.toPath(), dataMapDir);
        assertEquals(tempDir.toPath().resolve("java"), dataMapDir.resolve(relative.getDestDir()).normalize());
        assertEquals(tempDir.toPath().resolve("generated"), dataMapDir.resolve(absolute.getDestDir()).normalize());
        assertEquals(oldDir.toPath(), dataMapDir.resolve(notSet.getDestDir()).normalize());
    }

    /**
     * A never saved DataMap saved to a Maven resource directory gets the Maven source directory.
     */
    @Test
    public void saveAs_NeverSavedDataMapInMavenTree() throws Exception {
        File resourceDir = Files.createDirectories(tempDir.toPath().resolve(Path.of("src", "main", "resources")))
                .toFile();

        FileProjectSaver saver = new FileProjectSaver();
        DIBootstrap.createInjector(testModule()).injectMembers(saver);

        DataMap dataMap = new DataMap("newMap");
        CgenDescriptor descriptor = new CgenDescriptor();
        dataMap.addCgenDescriptor(descriptor);
        Project project = new Project();
        project.setName("newProject");
        project.getDataMaps().add(dataMap);

        saver.saveAs(project, new URLResource(resourceDir.toURI().toURL()));

        String map = Files.readString(new File(resourceDir, "newMap.map.xml").toPath());
        assertTrue(map.contains("<destDir>../java</destDir>"), map);
        assertEquals("../java", descriptor.getDestDir());
    }

    @Test
    public void saveAs_NeverSavedDataMap() throws Exception {
        File dir = Files.createDirectories(tempDir.toPath().resolve("project")).toFile();

        FileProjectSaver saver = new FileProjectSaver();
        DIBootstrap.createInjector(testModule()).injectMembers(saver);

        DataMap dataMap = new DataMap("newMap");
        dataMap.addCgenDescriptor(new CgenDescriptor());
        Project project = new Project();
        project.setName("newProject");
        project.getDataMaps().add(dataMap);

        saver.saveAs(project, new URLResource(dir.toURI().toURL()));

        String map = Files.readString(new File(dir, "newMap.map.xml").toPath());
        assertTrue(map.contains("<destDir>.</destDir>"), map);
    }

    /**
     * Settings shared by two DataMaps are rebased once.
     */
    @Test
    public void saveAs_SharedCgenSettings() throws Exception {
        File oldDir = new File(tempDir, "old");
        File newDir = Files.createDirectories(tempDir.toPath().resolve(Path.of("new", "project"))).toFile();
        Resource source = copyProject("PROJECT2", oldDir, "testProjectMap2_1", "testProjectMap2_2");

        FileProjectSaver saver = new FileProjectSaver();
        Injector injector = DIBootstrap.createInjector(testModule());
        injector.injectMembers(saver);

        Project project = injector.getInstance(ProjectLoader.class).load(source);
        CgenDescriptor shared = new CgenDescriptor();
        shared.setDestDir("../java");
        project.getDataMap("testProjectMap2_1").addCgenDescriptor(shared);
        project.getDataMap("testProjectMap2_2").addCgenDescriptor(shared);

        saver.saveAs(project, new URLResource(newDir.toURI().toURL()));

        assertEquals("../../java", shared.getDestDir());
        for (String map : new String[]{"testProjectMap2_1.map.xml", "testProjectMap2_2.map.xml"}) {
            String xml = Files.readString(new File(newDir, map).toPath());
            assertTrue(xml.contains("<destDir>../../java</destDir>"), xml);
        }
    }

    @Test
    public void saveAs_FailedSaveKeepsCgenOutputDirs() throws Exception {
        File oldDir = new File(tempDir, "old");
        File newDir = Files.createDirectories(tempDir.toPath().resolve(Path.of("new", "project"))).toFile();
        Resource source = copyProject("PROJECT2", oldDir, "testProjectMap2_1", "testProjectMap2_2");

        FileProjectSaver failingSaver = new FileProjectSaver() {

            @Override
            void saveToTempFile(SaveUnit unit, PrintWriter printWriter) {
                throw new CayenneRuntimeException("Test Exception");
            }
        };
        Injector injector = DIBootstrap.createInjector(testModule());
        injector.injectMembers(failingSaver);

        Project project = injector.getInstance(ProjectLoader.class).load(source);
        DataMap dataMap = project.getDataMap("testProjectMap2_1");
        List<CgenDescriptor> descriptors = addCgenDescriptors(dataMap);
        List<String> destDirs = descriptors.stream().map(CgenDescriptor::getDestDir).toList();
        List<Path> outputDirs = outputDirs(dataMap);

        assertThrows(CayenneRuntimeException.class,
                () -> failingSaver.saveAs(project, new URLResource(newDir.toURI().toURL())));

        assertEquals(destDirs, descriptors.stream().map(CgenDescriptor::getDestDir).toList());
        assertEquals(outputDirs, outputDirs(dataMap));

        FileProjectSaver saver = new FileProjectSaver();
        injector.injectMembers(saver);
        saver.save(project, null);

        String map = Files.readString(new File(oldDir, "testProjectMap2_1.map.xml").toPath());
        assertTrue(map.contains("<destDir>../java</destDir>"), map);
        assertTrue(map.contains("<destDir>../generated</destDir>"), map);
        assertTrue(map.contains("<destDir>.</destDir>"), map);
    }

    /**
     * The committed DataMaps keep the rebased settings, and the others the original ones.
     */
    @Test
    public void saveAs_PartlyCommittedSaveKeepsCgenOutputDirs() throws Exception {
        File oldDir = new File(tempDir, "old");
        File newDir = Files.createDirectories(tempDir.toPath().resolve(Path.of("new", "project"))).toFile();
        Resource source = copyProject("PROJECT2", oldDir, "testProjectMap2_1", "testProjectMap2_2");

        FileProjectSaver failingSaver = new FileProjectSaver() {

            @Override
            void saveCommit(Collection<SaveUnit> units) {
                List<SaveUnit> allUnits = new ArrayList<>(units);
                super.saveCommit(allUnits.subList(0, allUnits.size() - 1));
                throw new CayenneRuntimeException("Test Exception");
            }
        };
        Injector injector = DIBootstrap.createInjector(testModule());
        injector.injectMembers(failingSaver);

        Project project = injector.getInstance(ProjectLoader.class).load(source);
        Map<DataMap, List<Path>> outputDirs = new HashMap<>();
        for (DataMap dataMap : project.getDataMaps()) {
            addCgenDescriptors(dataMap);
            outputDirs.put(dataMap, outputDirs(dataMap));
        }

        assertThrows(CayenneRuntimeException.class,
                () -> failingSaver.saveAs(project, new URLResource(newDir.toURI().toURL())));

        Set<Path> dataMapDirs = new HashSet<>();
        for (DataMap dataMap : project.getDataMaps()) {
            dataMapDirs.add(CgenPaths.dataMapDir(dataMap).orElseThrow());
            assertEquals(outputDirs.get(dataMap), outputDirs(dataMap), dataMap.getName());
        }
        assertEquals(Set.of(oldDir.toPath(), newDir.toPath()), dataMapDirs, "one DataMap is committed, one is not");
    }

    // a relative, an absolute and an unset output directory
    private List<CgenDescriptor> addCgenDescriptors(DataMap dataMap) {
        CgenDescriptor relative = new CgenDescriptor();
        relative.setDestDir("../java");
        CgenDescriptor absolute = new CgenDescriptor();
        absolute.setDestDir(new File(tempDir, "generated").getAbsolutePath());
        CgenDescriptor notSet = new CgenDescriptor();

        List<CgenDescriptor> descriptors = List.of(relative, absolute, notSet);
        descriptors.forEach(dataMap::addCgenDescriptor);
        return descriptors;
    }

    private static List<Path> outputDirs(DataMap dataMap) {
        Path dataMapDir = CgenPaths.dataMapDir(dataMap).orElseThrow();
        return dataMap.getCgenDescriptors().stream()
                .map(descriptor -> dataMapDir.resolve(Objects.requireNonNullElse(descriptor.getDestDir(), ""))
                        .normalize())
                .toList();
    }

    private static Module testModule() {
        return binder -> {
            binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
            binder.bind(ProjectFileUpgrader.class).to(ProjectFileUpgrader.class);
            binder.bind(ProjectLoader.class).to(XMLProjectLoader.class);
            binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            binder.bind(ProjectLoadingMode.class).toInstance(ProjectLoadingMode.FULL);
            binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();
        };
    }

    private Resource copyProject(String projectName, File dir, String... dataMapNames) throws Exception {
        Files.createDirectories(dir.toPath());
        String packagePath = getClass().getPackage().getName().replace('.', '/');
        List<String> files = new ArrayList<>();
        files.add("cayenne-" + projectName + ".xml");
        for (String dataMapName : dataMapNames) {
            files.add(dataMapName + ".map.xml");
        }
        for (String file : files) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(packagePath + "/" + file)) {
                Files.copy(in, new File(dir, file).toPath());
            }
        }
        return new URLResource(new File(dir, files.get(0)).toURI().toURL());
    }
}
