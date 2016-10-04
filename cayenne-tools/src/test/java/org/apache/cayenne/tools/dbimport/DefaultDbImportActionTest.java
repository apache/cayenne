/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.tools.dbimport;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.CayenneDbSyncModule;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.merge.AddColumnToDb;
import org.apache.cayenne.dbsync.merge.AddRelationshipToDb;
import org.apache.cayenne.dbsync.merge.CreateTableToDb;
import org.apache.cayenne.dbsync.merge.CreateTableToModel;
import org.apache.cayenne.dbsync.merge.DefaultModelMergeDelegate;
import org.apache.cayenne.dbsync.merge.MergerToken;
import org.apache.cayenne.dbsync.merge.builders.DataMapBuilder;
import org.apache.cayenne.dbsync.merge.factory.DefaultMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.db.DbLoader;
import org.apache.cayenne.dbsync.reverse.db.DbLoaderConfiguration;
import org.apache.cayenne.dbsync.reverse.db.DbLoaderDelegate;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.project.FileProjectSaver;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

import javax.sql.DataSource;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.dbAttr;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.dbEntity;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.objAttr;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.objEntity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        DbLoader dbLoader = new DbLoader(mockConnection, mockAdapter, mockDelegate, mockNameGenerator) {
            @Override
            public void load(DataMap dataMap, DbLoaderConfiguration config) throws SQLException {
                new DataMapBuilder(dataMap).withDbEntities(2).build();
            }
        };

        DbImportConfiguration config = mock(DbImportConfiguration.class);
        when(config.createLoader(any(DbAdapter.class), any(Connection.class), any(DbLoaderDelegate.class)))
                .thenReturn(dbLoader);

        final DataMap dataMap = new DataMap();
        when(config.createDataMap()).thenReturn(dataMap);
        when(config.createMergeDelegate()).thenReturn(new DefaultModelMergeDelegate());
        when(config.getDbLoaderConfig()).thenReturn(new DbLoaderConfiguration());

        final boolean[] haveWeTriedToSave = {false};
        DefaultDbImportAction action = buildDbImportAction(new FileProjectSaver() {
            @Override
            public void save(Project project) {
                haveWeTriedToSave[0] = true;

                // Validation phase
                assertSame(dataMap, project.getRootNode());
            }
        }, null);

        action.execute(config);

        assertTrue("We should try to save.", haveWeTriedToSave[0]);
    }

    @Test
    public void testImportWithFieldChanged() throws Exception {
        DbLoader dbLoader = new DbLoader(mockConnection, mockAdapter, mockDelegate, mockNameGenerator) {
            @Override
            public void load(DataMap dataMap, DbLoaderConfiguration config) throws SQLException {
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
            }
        };

        DbImportConfiguration params = mock(DbImportConfiguration.class);
        when(params.createLoader(any(DbAdapter.class), any(Connection.class), any(DbLoaderDelegate.class)))
                .thenReturn(dbLoader);

        when(params.createDataMap()).thenReturn(new DataMap("testImport"));
        when(params.getDataMapFile()).thenReturn(FILE_STUB);
        when(params.createMergeDelegate()).thenReturn(new DefaultModelMergeDelegate());
        when(params.getDbLoaderConfig()).thenReturn(new DbLoaderConfiguration());
        when(params.getNameGenerator()).thenReturn(new DefaultObjectNameGenerator());
        when(params.getMeaningfulPKFilter()).thenReturn(NamePatternMatcher.EXCLUDE_ALL);

        final boolean[] haveWeTriedToSave = {false};
        DefaultDbImportAction action = buildDbImportAction(new FileProjectSaver() {
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
        }, new MapLoader() {

            @Override
            public synchronized DataMap loadDataMap(InputSource src) throws CayenneRuntimeException {
                return new DataMapBuilder().with(
                        dbEntity("ARTGROUP").attributes(
                                dbAttr("GROUP_ID").typeInt().primaryKey(),
                                dbAttr("NAME").typeVarchar(100).mandatory(),
                                dbAttr("PARENT_GROUP_ID").typeInt()
                        )).with(
                        objEntity("org.apache.cayenne.testdo.testmap", "ArtGroup", "ARTGROUP").attributes(
                                objAttr("name").type(String.class).dbPath("NAME")
                        )).build();
            }
        });

        action.execute(params);

        assertTrue("We should try to save.", haveWeTriedToSave[0]);
    }

    @Test
    public void testImportWithoutChanges() throws Exception {
        DbLoader dbLoader = new DbLoader(mockConnection, mockAdapter, mockDelegate, mockNameGenerator) {
            @Override
            public void load(DataMap dataMap, DbLoaderConfiguration config) throws SQLException {
                new DataMapBuilder(dataMap).with(
                        dbEntity("ARTGROUP").attributes(
                                dbAttr("NAME").typeVarchar(100).mandatory()
                        ));
            }
        };

        DbImportConfiguration params = mock(DbImportConfiguration.class);
        when(params.createLoader(any(DbAdapter.class), any(Connection.class), any(DbLoaderDelegate.class)))
                .thenReturn(dbLoader);

        when(params.createDataMap()).thenReturn(new DataMap("testImport"));
        when(params.getDataMapFile()).thenReturn(FILE_STUB);
        when(params.createMergeDelegate()).thenReturn(new DefaultModelMergeDelegate());
        when(params.getDbLoaderConfig()).thenReturn(new DbLoaderConfiguration());

        Log log = mock(Log.class);
        when(log.isDebugEnabled()).thenReturn(false);
        when(log.isInfoEnabled()).thenReturn(false);

        FileProjectSaver projectSaver = mock(FileProjectSaver.class);
        doNothing().when(projectSaver).save(any(Project.class));

        MapLoader mapLoader = mock(MapLoader.class);
        stub(mapLoader.loadDataMap(any(InputSource.class))).toReturn(new DataMapBuilder().with(
                dbEntity("ARTGROUP").attributes(
                        dbAttr("NAME").typeVarchar(100).mandatory()
                )).build());

        DefaultDbImportAction action = buildDbImportAction(log, projectSaver, mapLoader);

        action.execute(params);

        // no changes - we still
        verify(projectSaver, never()).save(any(Project.class));
        verify(mapLoader, times(1)).loadDataMap(any(InputSource.class));
    }

    @Test
    public void testImportWithDbError() throws Exception {
        DbLoader dbLoader = mock(DbLoader.class);
        doThrow(new SQLException()).when(dbLoader).load(any(DataMap.class), any(DbLoaderConfiguration.class));

        DbImportConfiguration params = mock(DbImportConfiguration.class);
        when(params.createLoader(any(DbAdapter.class), any(Connection.class), any(DbLoaderDelegate.class)))
                .thenReturn(dbLoader);

        FileProjectSaver projectSaver = mock(FileProjectSaver.class);
        doNothing().when(projectSaver).save(any(Project.class));

        MapLoader mapLoader = mock(MapLoader.class);
        when(mapLoader.loadDataMap(any(InputSource.class))).thenReturn(null);

        DefaultDbImportAction action = buildDbImportAction(projectSaver, mapLoader);

        try {
            action.execute(params);
            fail();
        } catch (SQLException e) {
            // expected
        }

        verify(projectSaver, never()).save(any(Project.class));
        verify(mapLoader, never()).loadDataMap(any(InputSource.class));
    }

    private DefaultDbImportAction buildDbImportAction(FileProjectSaver projectSaver, MapLoader mapLoader) throws Exception {
        Log log = mock(Log.class);
        when(log.isDebugEnabled()).thenReturn(true);
        when(log.isInfoEnabled()).thenReturn(true);

        return buildDbImportAction(log, projectSaver, mapLoader);
    }

    private DefaultDbImportAction buildDbImportAction(Log log, FileProjectSaver projectSaver, MapLoader mapLoader) throws Exception {
        DbAdapter dbAdapter = mock(DbAdapter.class);

        DbAdapterFactory adapterFactory = mock(DbAdapterFactory.class);
        when(adapterFactory.createAdapter(any(DataNodeDescriptor.class), any(DataSource.class))).thenReturn(dbAdapter);

        DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);
        DataSource mock = mock(DataSource.class);
        when(dataSourceFactory.getDataSource(any(DataNodeDescriptor.class))).thenReturn(mock);

        MergerTokenFactoryProvider mergerTokenFactoryProvider = mock(MergerTokenFactoryProvider.class);
        when(mergerTokenFactoryProvider.get(any(DbAdapter.class))).thenReturn(new DefaultMergerTokenFactory());

        return new DefaultDbImportAction(log, projectSaver, dataSourceFactory, adapterFactory, mapLoader, mergerTokenFactoryProvider);
    }

    @Test
    public void testSaveLoaded() throws Exception {
        Log log = mock(Log.class);
        Injector i = DIBootstrap.createInjector(new CayenneDbSyncModule(), new ToolsModule(log), new DbImportModule());

        DefaultDbImportAction action = (DefaultDbImportAction) i.getInstance(DbImportAction.class);

        String packagePath = getClass().getPackage().getName().replace('.', '/');
        URL packageUrl = getClass().getClassLoader().getResource(packagePath);
        assertNotNull(packageUrl);
        URL outUrl = new URL(packageUrl, "dbimport/testSaveLoaded1.map.xml");

        File out = new File(outUrl.toURI());
        out.delete();
        assertFalse(out.isFile());

        DataMap map = new DataMap("testSaveLoaded1");
        map.setConfigurationSource(new URLResource(outUrl));

        action.saveLoaded(map);

        assertTrue(out.isFile());

        String contents = Util.stringFromFile(out);
        assertTrue("Has no project version saved", contents.contains("project-version=\""));
    }

    @Test
    public void testMergeTokensSorting() {
        LinkedList<MergerToken> tokens = new LinkedList<MergerToken>();
        tokens.add(new AddColumnToDb(null, null));
        tokens.add(new AddRelationshipToDb(null, null));
        tokens.add(new CreateTableToDb(null));
        tokens.add(new CreateTableToModel(null));

        assertEquals(asList("AddColumnToDb", "CreateTableToDb", "CreateTableToModel", "AddRelationshipToDb"),
                toClasses(DefaultDbImportAction.sort(tokens)));
    }

    private List<String> toClasses(List<MergerToken> sort) {
        LinkedList<String> res = new LinkedList<String>();
        for (MergerToken mergerToken : sort) {
            res.add(mergerToken.getClass().getSimpleName());
        }
        return res;
    }
}
