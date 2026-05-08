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

import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.runtime.DataSourceFactory;
import org.apache.cayenne.configuration.runtime.DbAdapterFactory;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.merge.builders.DataMapBuilder;
import org.apache.cayenne.dbsync.merge.factory.DefaultMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.db.CreateTableToDb;
import org.apache.cayenne.dbsync.merge.token.model.AddColumnToModel;
import org.apache.cayenne.dbsync.merge.token.model.AddRelationshipToModel;
import org.apache.cayenne.dbsync.merge.token.model.CreateTableToModel;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.NoStemStemmer;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoader;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoaderConfiguration;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.dbload.DefaultModelMergeDelegate;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.project.FileProjectSaver;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.dbAttr;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.dbEntity;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.objAttr;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.objEntity;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private DbAdapter mockAdapter;
    private Connection mockConnection;
    private DbLoaderDelegate mockDelegate;
    private ObjectNameGenerator mockNameGenerator;

    @Before
    public void before() {
        mockAdapter = mock(DbAdapter.class);
        mockConnection = mock(Connection.class);
        mockDelegate = mock(DbLoaderDelegate.class);
        mockNameGenerator = mock(ObjectNameGenerator.class);
    }

    @Test
    public void testNewDataMapImport() throws Exception {

        DbImportConfiguration config = mock(DbImportConfiguration.class);
        when(config.createMergeDelegate()).thenReturn(new DefaultModelMergeDelegate());
        when(config.getDbLoaderConfig()).thenReturn(new DbLoaderConfiguration());
        when(config.getTargetDataMap()).thenReturn(new File("xyz.map.xml"));
        when(config.createNameGenerator()).thenReturn(new DefaultObjectNameGenerator(NoStemStemmer.getInstance()));
        when(config.createMeaningfulPKFilter()).thenReturn(NamePatternMatcher.EXCLUDE_ALL);

        DbLoader dbLoader = new DbLoader(mockAdapter, mockConnection, config.getDbLoaderConfig(), mockDelegate, mockNameGenerator) {
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
            public void save(Project project) {
                haveWeTriedToSave[0] = true;

                // Validation phase
                assertTrue(project.getRootNode() instanceof DataMap);
            }
        }, null, dbLoader);

        action.execute(config);

        assertTrue("We should try to save.", haveWeTriedToSave[0]);
    }

    @Test
    public void testImportWithFieldChanged() throws Exception {

        DbImportConfiguration config = mock(DbImportConfiguration.class);

        when(config.getTargetDataMap()).thenReturn(FILE_STUB);
        when(config.createMergeDelegate()).thenReturn(new DefaultModelMergeDelegate());
        when(config.getDbLoaderConfig()).thenReturn(new DbLoaderConfiguration());
        when(config.createNameGenerator()).thenReturn(new DefaultObjectNameGenerator(NoStemStemmer.getInstance()));
        when(config.createMeaningfulPKFilter()).thenReturn(NamePatternMatcher.EXCLUDE_ALL);

        DbLoader dbLoader = new DbLoader(mockAdapter, mockConnection, config.getDbLoaderConfig(), mockDelegate, mockNameGenerator) {
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
                public void save(Project project) {
                    haveWeTriedToSave[0] = true;

                    // Validation phase
                    DataMap rootNode = (DataMap) project.getRootNode();
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

        assertTrue("We should try to save.", haveWeTriedToSave[0]);
    }

    @Test
    public void testImportWithoutChanges() throws Exception {
        DbImportConfiguration config = mock(DbImportConfiguration.class);
        when(config.getTargetDataMap()).thenReturn(FILE_STUB);
        when(config.createMergeDelegate()).thenReturn(new DefaultModelMergeDelegate());
        when(config.getDbLoaderConfig()).thenReturn(new DbLoaderConfiguration());

        DbLoader dbLoader = new DbLoader(mockAdapter, mockConnection, config.getDbLoaderConfig(), mockDelegate, mockNameGenerator) {
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

        FileProjectSaver projectSaver = mock(FileProjectSaver.class);
        doNothing().when(projectSaver).save(any(Project.class));

        DataMapLoader mapLoader = mock(DataMapLoader.class);
        when(mapLoader.load(any(Resource.class))).thenReturn(new DataMapBuilder().with(
                dbEntity("ARTGROUP").attributes(
                        dbAttr("NAME").typeVarchar(100).mandatory()
                )).build());

        DefaultDbImportAction action = buildDbImportAction(projectSaver, mapLoader, dbLoader);

        action.execute(config);

        // no changes - we still
        verify(projectSaver, never()).save(any(Project.class));
        verify(mapLoader, times(1)).load(any(Resource.class));
    }

    @Test
    public void testImportWithDbError() throws Exception {
        DbLoader dbLoader = mock(DbLoader.class);
        doThrow(new SQLException()).when(dbLoader).load();

        DbImportConfiguration params = mock(DbImportConfiguration.class);

        FileProjectSaver projectSaver = mock(FileProjectSaver.class);
        doNothing().when(projectSaver).save(any(Project.class));

        DataMapLoader mapLoader = mock(DataMapLoader.class);
        when(mapLoader.load(any(Resource.class))).thenReturn(null);

        DefaultDbImportAction action = buildDbImportAction(projectSaver, mapLoader, dbLoader);

        try {
            action.execute(params);
            fail();
        } catch (SQLException e) {
            // expected
        }

        verify(projectSaver, never()).save(any(Project.class));
        verify(mapLoader, never()).load(any(Resource.class));
    }

    private DefaultDbImportAction buildDbImportAction(FileProjectSaver projectSaver, DataMapLoader mapLoader, final DbLoader dbLoader)
            throws Exception {

        Logger log = mock(Logger.class);
        when(log.isDebugEnabled()).thenReturn(true);
        when(log.isInfoEnabled()).thenReturn(true);

        DbAdapter dbAdapter = mock(DbAdapter.class);

        DbAdapterFactory adapterFactory = mock(DbAdapterFactory.class);
        when(adapterFactory.createAdapter(any(), any())).thenReturn(dbAdapter);

        DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);
        DataSource mock = mock(DataSource.class);
        when(dataSourceFactory.getDataSource(any())).thenReturn(mock);
        DataChannelMetaData metaData = mock(DataChannelMetaData.class);
        MergerTokenFactoryProvider mergerTokenFactoryProvider = mock(MergerTokenFactoryProvider.class);
        when(mergerTokenFactoryProvider.get(any())).thenReturn(new DefaultMergerTokenFactory());

        DataChannelDescriptorLoader dataChannelDescriptorLoader = mock(DataChannelDescriptorLoader.class);

        return new DefaultDbImportAction(log, projectSaver, dataSourceFactory, adapterFactory, mapLoader, mergerTokenFactoryProvider, dataChannelDescriptorLoader, metaData) {

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
    public void testSaveLoadedNoProject() throws Exception {
        Logger log = mock(Logger.class);
        Injector i = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(log), new DbImportModule());
        DbImportConfiguration params = mock(DbImportConfiguration.class);
        when(params.getCayenneProject()).thenReturn(null);

        URL outUrl = new URL(getPackageURL(), "dbimport/testSaveLoaded1.map.xml");

        DefaultDbImportAction action = (DefaultDbImportAction) i.getInstance(DbImportAction.class);

        File out = new File(outUrl.toURI());
        out.delete();
        assertFalse(out.exists());

        DataMap map = new DataMap("testSaveLoaded1");
        map.setConfigurationSource(new URLResource(outUrl));

        action.saveLoaded(map, params);

        assertTrue(out.isFile());

        String contents = Util.stringFromFile(out);
        assertTrue("Has no project version saved", contents.contains("project-version=\""));
    }

    @Test
    public void testSaveLoadedWithEmptyProject() throws Exception {
        Logger log = mock(Logger.class);
        Injector i = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(log), new DbImportModule());
        DbImportConfiguration params = mock(DbImportConfiguration.class);

        URL projectURL = new URL(getPackageURL(), "dbimport/cayenne-testProject2.map.xml");
        File projectFile = new File(projectURL.toURI());
        projectFile.delete();
        assertFalse(projectFile.exists());
        when(params.getCayenneProject()).thenReturn(projectFile);

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

        String dataMapContents = Util.stringFromFile(dataMapFile);
        assertTrue("Has no project version saved", dataMapContents.contains("project-version=\""));

        String projectContents = Util.stringFromFile(projectFile);
        assertTrue("Has no project version saved", projectContents.contains("project-version=\""));
        assertTrue("Has no datamap in project", projectContents.contains("<map name=\"testSaveLoaded2\"/>"));
    }

    @Test
    public void testSaveLoadedWithNonEmptyProject() throws Exception {
        Logger log = mock(Logger.class);
        Injector i = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(log), new DbImportModule());
        DbImportConfiguration params = mock(DbImportConfiguration.class);

        URL projectURL = new URL(getPackageURL(), "dbimport/cayenne-testProject3.map.xml");
        File projectFile = new File(projectURL.toURI());
        projectFile.delete();
        assertFalse(projectFile.exists());

        Files.write(projectFile.toPath(), ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<domain xmlns=\"http://cayenne.apache.org/schema/12/domain\"\n" +
                "\t xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "\t xsi:schemaLocation=\"http://cayenne.apache.org/schema/12/domain https://cayenne.apache.org/schema/12/domain.xsd\"\n" +
                "\t project-version=\"12\">\n" +
                "</domain>").getBytes(StandardCharsets.UTF_8));
        assertTrue(projectFile.isFile());

        when(params.getCayenneProject()).thenReturn(projectFile);

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

        String dataMapContents = Util.stringFromFile(dataMapFile);
        assertTrue("Has no project version saved", dataMapContents.contains("project-version=\""));

        String projectContents = Util.stringFromFile(projectFile);
        assertTrue("Has no project version saved", projectContents.contains("project-version=\""));
        assertTrue("Has no datamap in project", projectContents.contains("<map name=\"testSaveLoaded3\"/>"));
    }

    @Test
    public void testSaveLoadedWithNonEmptyProjectAndNonEmptyDataMap() throws Exception {
        Logger log = mock(Logger.class);
        Injector i = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(log), new DbImportModule());
        DbImportConfiguration params = mock(DbImportConfiguration.class);

        URL projectURL = new URL(getPackageURL(), "dbimport/cayenne-testProject4.map.xml");
        File projectFile = new File(projectURL.toURI());
        projectFile.delete();
        assertFalse(projectFile.exists());

        Files.write(projectFile.toPath(), ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<domain xmlns=\"http://cayenne.apache.org/schema/12/domain\"\n" +
                "\t xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "\t xsi:schemaLocation=\"http://cayenne.apache.org/schema/12/domain https://cayenne.apache.org/schema/12/domain.xsd\"\n" +
                "\t project-version=\"12\">\n" +
                "\t<map name=\"testSaveLoaded4\"/>\n" +
                "</domain>").getBytes(StandardCharsets.UTF_8));
        assertTrue(projectFile.isFile());

        when(params.getCayenneProject()).thenReturn(projectFile);

        DefaultDbImportAction action = (DefaultDbImportAction) i.getInstance(DbImportAction.class);

        URL dataMapURL = new URL(getPackageURL(), "dbimport/testSaveLoaded4.map.xml");

        File dataMapFile = new File(dataMapURL.toURI());
        dataMapFile.delete();
        assertFalse(dataMapFile.exists());

        Files.write(dataMapFile.toPath(), ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<data-map xmlns=\"http://cayenne.apache.org/schema/12/modelMap\"\n" +
                "\t xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "\t xsi:schemaLocation=\"http://cayenne.apache.org/schema/12/modelMap https://cayenne.apache.org/schema/12/modelMap.xsd\"\n" +
                "\t project-version=\"12\">\n" +
                "\t<db-entity name=\"test\">\n" +
                "\t\t<db-attribute name=\"test\" type=\"INT\"/>\n" +
                "\t</db-entity>\n" +
                "</data-map>").getBytes(StandardCharsets.UTF_8));
        assertTrue(dataMapFile.isFile());

        DataMap map = new DataMap("testSaveLoaded4");
        map.setConfigurationSource(new URLResource(dataMapURL));

        action.saveLoaded(map, params);

        assertTrue(dataMapFile.isFile());
        assertTrue(projectFile.isFile());

        String dataMapContents = Util.stringFromFile(dataMapFile);
        assertTrue("Has no project version saved", dataMapContents.contains("project-version=\""));
        assertFalse(dataMapContents.contains("<db-entity"));

        String projectContents = Util.stringFromFile(projectFile);
        assertTrue("Has no project version saved", projectContents.contains("project-version=\""));
        assertEquals("Has no or too many datamaps in project", 1, Util.countMatches(projectContents, "<map name=\"testSaveLoaded4\"/>"));
    }

    @Test
    public void testMergeTokensSorting() {
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
}
