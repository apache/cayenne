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
package org.apache.cayenne.dbsync.reverse.dbimport;

import org.apache.cayenne.project.ProjectLoader;
import org.apache.cayenne.project.DataMapLoader;
import org.apache.cayenne.dbsync.reverse.configuration.DbAdapterFactory;
import org.apache.cayenne.project.xml.ProjectMetaData;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.dbsync.merge.builders.DataMapBuilder;
import org.apache.cayenne.dbsync.merge.factory.DefaultMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.db.CreateTableToDb;
import org.apache.cayenne.dbsync.merge.token.model.AddColumnToModel;
import org.apache.cayenne.dbsync.merge.token.model.AddRelationshipToModel;
import org.apache.cayenne.dbsync.merge.token.model.CreateTableToModel;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoader;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoaderDelegate;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.projecttools.FileProjectSaver;
import org.apache.cayenne.project.ProjectNode;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.unit.TestAdapters;
import org.apache.cayenne.unit.jdbc.TestConnection;
import org.apache.cayenne.unit.jdbc.TestDataSource;
import org.apache.cayenne.dbsync.reverse.dbload.DefaultDbLoaderDelegate;
import org.apache.cayenne.project.xml.DefaultProjectMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.*;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultDbImportActionTest {

    public static final File FILE_STUB = new File("") {
        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean canRead() {
            return true;
        }
    };

    private DbAdapter adapter;
    private Connection connection;
    private DbLoaderDelegate loaderDelegate;
    private ObjectNameGenerator nameGenerator;

    @BeforeEach
    public void before() {
        adapter = TestAdapters.jdbc();
        connection = new TestConnection();
        loaderDelegate = new DefaultDbLoaderDelegate();
        nameGenerator = new DefaultObjectNameGenerator();
    }

    @Test
    public void newDataMapImport() throws Exception {

        DbImportConfiguration config = new TestDbImportConfiguration();
        config.setTargetDataMap(new File("xyz.map.xml"));

        DbLoader dbLoader = new DbLoader(adapter, connection, config.getDbLoaderConfig(), loaderDelegate,
                nameGenerator) {
            @Override
            public DataMap load() {
                DataMap map = new DataMap();
                new DataMapBuilder(map).withDbEntities(2).build();
                return map;
            }
        };

        final boolean[] haveWeTriedToSave = {false};
        DefaultDbImportAction action = buildDbImportAction(new FileProjectSaver(Collections.emptyList()) {
            @Override
            public void save(ProjectNode rootNode) {
                haveWeTriedToSave[0] = true;

                // Validation phase
                assertTrue(rootNode instanceof DataMap);
            }
        }, null, dbLoader);

        action.execute(config);

        assertTrue(haveWeTriedToSave[0], "We should try to save.");
    }

    @Test
    public void importWithFieldChanged() throws Exception {

        DbImportConfiguration config = new TestDbImportConfiguration();
        config.setTargetDataMap(FILE_STUB);

        DbLoader dbLoader = new DbLoader(adapter, connection, config.getDbLoaderConfig(), loaderDelegate,
                nameGenerator) {
            @Override
            public DataMap load() {
                DataMap dataMap = new DataMap();
                new DataMapBuilder(dataMap).with(
                        dbEntity("ARTGROUP").attributes(
                                dbAttr("GROUP_ID").typeInt().primaryKey(),
                                dbAttr("NAME").typeVarchar(100).mandatory(),
                                dbAttr("NAME_01").typeVarchar(100).mandatory(),
                                dbAttr("PARENT_GROUP_ID").typeInt()
                        )).with(
                        objEntity("org.apache.cayenne.testdo.testmap", "ArtGroup", "ARTGROUP").attributes(
                                objAttr("name").type(String.class).dbPath("NAME")
                        ));
                return dataMap;
            }
        };

        final boolean[] haveWeTriedToSave = {false};
        DefaultDbImportAction action = buildDbImportAction(
            new FileProjectSaver(Collections.emptyList()) {
                @Override
                public void save(ProjectNode node) {
                    haveWeTriedToSave[0] = true;

                    // Validation phase
                    DataMap rootNode = (DataMap) node;
                    assertEquals(1, rootNode.getObjEntities().size());
                    assertEquals(1, rootNode.getDbEntityMap().size());

                    DbEntity entity = rootNode.getDbEntity("ARTGROUP");
                    assertNotNull(entity);
                    assertEquals(4, entity.getAttributes().size());
                    assertNotNull(entity.getAttribute("NAME_01"));
                }
            },

            configurationResource -> new DataMapBuilder()
                    .with(
                        dbEntity("ARTGROUP").attributes(
                                dbAttr("GROUP_ID").typeInt().primaryKey(),
                                dbAttr("NAME").typeVarchar(100).mandatory(),
                                dbAttr("PARENT_GROUP_ID").typeInt()
                        ))
                    .with(
                        objEntity("org.apache.cayenne.testdo.testmap", "ArtGroup", "ARTGROUP").attributes(
                                objAttr("name").type(String.class).dbPath("NAME")
                        ))
                    .build()
                , dbLoader
        );

        action.execute(config);

        assertTrue(haveWeTriedToSave[0], "We should try to save.");
    }

    @Test
    public void importWithoutChanges() throws Exception {
        DbImportConfiguration config = new TestDbImportConfiguration();
        config.setTargetDataMap(FILE_STUB);

        DbLoader dbLoader = new DbLoader(adapter, connection, config.getDbLoaderConfig(), loaderDelegate,
                nameGenerator) {
            @Override
            public DataMap load() {
                DataMap dataMap = new DataMap();
                new DataMapBuilder(dataMap).with(
                        dbEntity("ARTGROUP").attributes(
                                dbAttr("NAME").typeVarchar(100).mandatory()
                        ));
                return dataMap;
            }
        };

        RecordingProjectSaver projectSaver = new RecordingProjectSaver();

        AtomicInteger mapLoads = new AtomicInteger();
        DataMapLoader mapLoader = resource -> {
            mapLoads.incrementAndGet();
            return new DataMapBuilder().with(
                    dbEntity("ARTGROUP").attributes(
                            dbAttr("NAME").typeVarchar(100).mandatory()
                    )).build();
        };

        DefaultDbImportAction action = buildDbImportAction(projectSaver, mapLoader, dbLoader);

        action.execute(config);

        // no changes - nothing is saved, but the existing map is still loaded to compare against
        assertEquals(0, projectSaver.saves);
        assertEquals(1, mapLoads.get());
    }

    @Test
    public void importWithDbError() throws Exception {
        DbImportConfiguration params = new TestDbImportConfiguration();

        DbLoader dbLoader = new DbLoader(adapter, connection, params.getDbLoaderConfig(), loaderDelegate,
                nameGenerator) {
            @Override
            public DataMap load() throws SQLException {
                throw new SQLException();
            }
        };

        RecordingProjectSaver projectSaver = new RecordingProjectSaver();

        AtomicInteger mapLoads = new AtomicInteger();
        DataMapLoader mapLoader = resource -> {
            mapLoads.incrementAndGet();
            return null;
        };

        DefaultDbImportAction action = buildDbImportAction(projectSaver, mapLoader, dbLoader);

        assertThrows(SQLException.class, () -> action.execute(params));

        assertEquals(0, projectSaver.saves);
        assertEquals(0, mapLoads.get());
    }

    private DefaultDbImportAction buildDbImportAction(FileProjectSaver projectSaver, DataMapLoader mapLoader, final DbLoader dbLoader)
            throws Exception {

        DbAdapterFactory adapterFactory = (adapterType, dataSource) -> adapter;
        ProjectMetaData metaData = new DefaultProjectMetaData();
        MergerTokenFactoryProvider mergerTokenFactoryProvider =
                new MergerTokenFactoryProvider(Map.of(), new DefaultMergerTokenFactory());

        // the imports here have no Cayenne project to load
        ProjectLoader projectLoader = source -> {
            throw new UnsupportedOperationException("Unexpected project load: " + source);
        };

        return new DefaultDbImportAction(NOPLogger.NOP_LOGGER, projectSaver, adapterFactory, mapLoader,
                mergerTokenFactoryProvider, projectLoader, metaData) {

            protected DbLoader createDbLoader(DbAdapter adapter,
                                               Connection connection,
                                               DbImportConfiguration config) {
                return dbLoader;
            }
        };
    }

    private URL getPackageURL() {
        String packagePath = getClass().getPackage().getName().replace('.', '/');
        URL packageUrl = getClass().getClassLoader().getResource(packagePath);
        assertNotNull(packageUrl);
        return packageUrl;
    }

    @Test
    public void saveLoadedNoProject() throws Exception {
        Logger log = NOPLogger.NOP_LOGGER;
        Injector i = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(log), new DbImportModule());
        DbImportConfiguration params = new DbImportConfiguration();

        URL outUrl = new URL(getPackageURL(), "dbimport/testSaveLoaded1.map.xml");

        DefaultDbImportAction action = (DefaultDbImportAction) i.getInstance(DbImportAction.class);

        File out = new File(outUrl.toURI());
        out.delete();
        assertFalse(out.exists());

        DataMap map = new DataMap("testSaveLoaded1");
        map.setConfigurationSource(new URLResource(outUrl));

        action.saveLoaded(map, params);

        assertTrue(out.isFile());

        String contents = stringFromFile(out);
        assertTrue(contents.contains("http://cayenne.apache.org/schema/15/"), "Has no project version saved");
    }

    @Test
    public void saveLoadedWithEmptyProject() throws Exception {
        Logger log = NOPLogger.NOP_LOGGER;
        Injector i = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(log), new DbImportModule());
        DbImportConfiguration params = new DbImportConfiguration();

        URL projectURL = new URL(getPackageURL(), "dbimport/cayenne-testProject2.map.xml");
        File projectFile = new File(projectURL.toURI());
        projectFile.delete();
        assertFalse(projectFile.exists());
        params.setCayenneProject(projectFile);

        DefaultDbImportAction action = (DefaultDbImportAction) i.getInstance(DbImportAction.class);

        URL dataMapURL = new URL(getPackageURL(), "dbimport/testSaveLoaded2.map.xml");

        File dataMapFile = new File(dataMapURL.toURI());
        dataMapFile.delete();
        assertFalse(dataMapFile.exists());

        DataMap map = new DataMap("testSaveLoaded2");
        map.setConfigurationSource(new URLResource(dataMapURL));

        action.saveLoaded(map, params);

        assertTrue(dataMapFile.isFile());
        assertTrue(projectFile.isFile());

        String dataMapContents = stringFromFile(dataMapFile);
        assertTrue(dataMapContents.contains("http://cayenne.apache.org/schema/15/"), "Has no project version saved");

        String projectContents = stringFromFile(projectFile);
        assertTrue(projectContents.contains("http://cayenne.apache.org/schema/15/"), "Has no project version saved");
        assertTrue(projectContents.contains("<map name=\"testSaveLoaded2\"/>"), "Has no datamap in project");
    }

    @Test
    public void saveLoadedWithNonEmptyProject() throws Exception {
        Logger log = NOPLogger.NOP_LOGGER;
        Injector i = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(log), new DbImportModule());
        DbImportConfiguration params = new DbImportConfiguration();

        URL projectURL = new URL(getPackageURL(), "dbimport/cayenne-testProject3.map.xml");
        File projectFile = new File(projectURL.toURI());
        projectFile.delete();
        assertFalse(projectFile.exists());

        Files.write(projectFile.toPath(), ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<project xmlns=\"http://cayenne.apache.org/schema/15/project\"\n" +
                "\t xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "\t xsi:schemaLocation=\"http://cayenne.apache.org/schema/15/project https://cayenne.apache.org/schema/15/project.xsd\">\n" +
                "</project>").getBytes(StandardCharsets.UTF_8));
        assertTrue(projectFile.isFile());

        params.setCayenneProject(projectFile);

        DefaultDbImportAction action = (DefaultDbImportAction) i.getInstance(DbImportAction.class);

        URL dataMapURL = new URL(getPackageURL(), "dbimport/testSaveLoaded3.map.xml");

        File dataMapFile = new File(dataMapURL.toURI());
        dataMapFile.delete();
        assertFalse(dataMapFile.exists());

        DataMap map = new DataMap("testSaveLoaded3");
        map.setConfigurationSource(new URLResource(dataMapURL));

        action.saveLoaded(map, params);

        assertTrue(dataMapFile.isFile());
        assertTrue(projectFile.isFile());

        String dataMapContents = stringFromFile(dataMapFile);
        assertTrue(dataMapContents.contains("http://cayenne.apache.org/schema/15/"), "Has no project version saved");

        String projectContents = stringFromFile(projectFile);
        assertTrue(projectContents.contains("http://cayenne.apache.org/schema/15/"), "Has no project version saved");
        assertTrue(projectContents.contains("<map name=\"testSaveLoaded3\"/>"), "Has no datamap in project");
    }

    @Test
    public void saveLoadedWithNonEmptyProjectAndNonEmptyDataMap() throws Exception {
        Logger log = NOPLogger.NOP_LOGGER;
        Injector i = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(log), new DbImportModule());
        DbImportConfiguration params = new DbImportConfiguration();

        URL projectURL = new URL(getPackageURL(), "dbimport/cayenne-testProject4.map.xml");
        File projectFile = new File(projectURL.toURI());
        projectFile.delete();
        assertFalse(projectFile.exists());

        Files.write(projectFile.toPath(), ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<project xmlns=\"http://cayenne.apache.org/schema/15/project\"\n" +
                "\t xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "\t xsi:schemaLocation=\"http://cayenne.apache.org/schema/15/project https://cayenne.apache.org/schema/15/project.xsd\">\n" +
                "\t<map name=\"testSaveLoaded4\"/>\n" +
                "</project>").getBytes(StandardCharsets.UTF_8));
        assertTrue(projectFile.isFile());

        params.setCayenneProject(projectFile);

        DefaultDbImportAction action = (DefaultDbImportAction) i.getInstance(DbImportAction.class);

        URL dataMapURL = new URL(getPackageURL(), "dbimport/testSaveLoaded4.map.xml");

        File dataMapFile = new File(dataMapURL.toURI());
        dataMapFile.delete();
        assertFalse(dataMapFile.exists());

        Files.write(dataMapFile.toPath(), ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<dataMap xmlns=\"http://cayenne.apache.org/schema/15/dataMap\"\n" +
                "\t xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "\t xsi:schemaLocation=\"http://cayenne.apache.org/schema/15/dataMap https://cayenne.apache.org/schema/15/dataMap.xsd\">\n" +
                "\t<dbEntity name=\"test\">\n" +
                "\t\t<dbAttribute name=\"test\" type=\"INT\"/>\n" +
                "\t</dbEntity>\n" +
                "</dataMap>").getBytes(StandardCharsets.UTF_8));
        assertTrue(dataMapFile.isFile());

        DataMap map = new DataMap("testSaveLoaded4");
        map.setConfigurationSource(new URLResource(dataMapURL));

        action.saveLoaded(map, params);

        assertTrue(dataMapFile.isFile());
        assertTrue(projectFile.isFile());

        String dataMapContents = stringFromFile(dataMapFile);
        assertTrue(dataMapContents.contains("http://cayenne.apache.org/schema/15/"), "Has no project version saved");
        assertFalse(dataMapContents.contains("<dbEntity"));

        String projectContents = stringFromFile(projectFile);
        assertTrue(projectContents.contains("http://cayenne.apache.org/schema/15/"), "Has no project version saved");
        assertEquals(1, Util.countMatches(projectContents, "<map name=\"testSaveLoaded4\"/>"), "Has no or too many datamaps in project");
    }

    @Test
    public void mergeTokensSorting() {
        LinkedList<MergerToken> tokens = new LinkedList<>();
        tokens.add(new AddColumnToModel(null, null));
        tokens.add(new AddRelationshipToModel(null, null));
        tokens.add(new CreateTableToDb(null));
        tokens.add(new CreateTableToModel(null));

        assertEquals(asList("CreateTableToDb", "CreateTableToModel", "AddColumnToModel", "AddRelationshipToModel"),
                toClasses(DefaultDbImportAction.sort(tokens)));
    }

    private List<String> toClasses(List<MergerToken> sort) {
        LinkedList<String> res = new LinkedList<>();
        for (MergerToken mergerToken : sort) {
            res.add(mergerToken.getClass().getSimpleName());
        }
        return res;
    }

    private static String stringFromFile(File file) throws IOException {
        StringBuilder buf = new StringBuilder();

        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null) {
                buf.append(line).append(System.lineSeparator());
            }
        }
        return buf.toString();
    }

    /**
     * A configuration whose DataSource hands out an in-memory connection stub.
     */
    static class TestDbImportConfiguration extends DbImportConfiguration {

        @Override
        public DataSource createDataSource() {
            return new TestDataSource();
        }
    }

    /**
     * A saver that counts the saves, instead of writing the files.
     */
    static class RecordingProjectSaver extends FileProjectSaver {

        int saves;

        RecordingProjectSaver() {
            super(Collections.emptyList());
        }

        @Override
        public void save(ProjectNode rootNode) {
            saves++;
        }
    }
}
