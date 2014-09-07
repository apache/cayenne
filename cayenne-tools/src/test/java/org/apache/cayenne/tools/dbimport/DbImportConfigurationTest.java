/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.tools.dbimport;

import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.DbLoaderDelegate;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class DbImportConfigurationTest {

    @Test
    public void testCreateLoader() throws Exception {
        DbImportConfiguration parameters = new DbImportConfiguration();

        Connection connection = mock(Connection.class);

        DbLoader loader = parameters.createLoader(mock(DbAdapter.class), connection,
                mock(DbLoaderDelegate.class));
        assertNotNull(loader);
        assertSame(connection, loader.getConnection());
        assertTrue(loader.includeTableName("dummy"));
    }

    @Test
    public void testCreateLoader_IncludeExclude() throws Exception {
        DbImportConfiguration parameters = new DbImportConfiguration();
        parameters.setIncludeTables("a,b,c*");

        DbLoader loader1 = parameters.createLoader(mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        assertFalse(loader1.includeTableName("dummy"));
        assertFalse(loader1.includeTableName("ab"));
        assertTrue(loader1.includeTableName("a"));
        assertTrue(loader1.includeTableName("b"));
        assertTrue(loader1.includeTableName("cd"));

        parameters.setExcludeTables("cd");

        DbLoader loader2 = parameters.createLoader(mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        assertFalse(loader2.includeTableName("dummy"));
        assertFalse(loader2.includeTableName("ab"));
        assertTrue(loader2.includeTableName("a"));
        assertTrue(loader2.includeTableName("b"));
        assertFalse(loader2.includeTableName("cd"));
        assertTrue(loader2.includeTableName("cx"));
    }


    @Test
    public void testCreateLoader_MeaningfulPk_Default() throws Exception {
        DbImportConfiguration parameters = new DbImportConfiguration();
        assertNull(parameters.getMeaningfulPkTables());

        DbLoader loader1 = parameters.createLoader(mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        DataMap map = new DataMap();

        DbEntity e1 = new DbEntity("e1");
        DbAttribute pk = new DbAttribute("pk", Types.INTEGER, e1);
        pk.setPrimaryKey(true);
        e1.addAttribute(pk);
        DbAttribute nonPk = new DbAttribute("nonPk", Types.INTEGER, e1);
        e1.addAttribute(nonPk);

        map.addDbEntity(e1);

        // DbLoader is so ugly and hard to test..
        Field dbEntityList = DbLoader.class.getDeclaredField("dbEntityList");
        dbEntityList.setAccessible(true);
        List<DbEntity> entities = (List<DbEntity>) dbEntityList.get(loader1);
        entities.add(e1);

        loader1.loadObjEntities(map);

        ObjEntity oe1 = map.getObjEntity("E1");
        assertEquals(1, oe1.getAttributes().size());
        assertNotNull(oe1.getAttribute("nonPk"));
    }

    @Test
    public void testCreateLoader_MeaningfulPk_Specified() throws Exception {
        DbImportConfiguration parameters = new DbImportConfiguration();
        parameters.setMeaningfulPkTables("a*");

        DbLoader loader1 = parameters.createLoader(mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        // DbLoader is so ugly and hard to test..
        Field dbEntityList = DbLoader.class.getDeclaredField("dbEntityList");
        dbEntityList.setAccessible(true);
        List<DbEntity> entities = (List<DbEntity>) dbEntityList.get(loader1);

        DataMap map = new DataMap();

        DbEntity e1 = new DbEntity("e1");
        DbAttribute pk = new DbAttribute("pk", Types.INTEGER, e1);
        pk.setPrimaryKey(true);
        e1.addAttribute(pk);
        DbAttribute nonPk = new DbAttribute("nonPk", Types.INTEGER, e1);
        e1.addAttribute(nonPk);

        map.addDbEntity(e1);
        entities.add(e1);

        DbEntity a1 = new DbEntity("a1");
        DbAttribute apk = new DbAttribute("pk", Types.INTEGER, a1);
        apk.setPrimaryKey(true);
        a1.addAttribute(apk);
        DbAttribute anonPk = new DbAttribute("nonPk", Types.INTEGER, a1);
        a1.addAttribute(anonPk);

        map.addDbEntity(a1);
        entities.add(a1);

        loader1.loadObjEntities(map);

        ObjEntity oe1 = map.getObjEntity("E1");
        assertEquals(1, oe1.getAttributes().size());
        assertNotNull(oe1.getAttribute("nonPk"));

        ObjEntity oe2 = map.getObjEntity("A1");
        assertEquals(2, oe2.getAttributes().size());
        assertNotNull(oe2.getAttribute("nonPk"));
        assertNotNull(oe2.getAttribute("pk"));
    }

    @Test
    public void testCreateLoader_UsePrimitives_False() throws Exception {
        DbImportConfiguration parameters = new DbImportConfiguration();
        parameters.setUsePrimitives(false);

        DbLoader loader1 = parameters.createLoader(mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        DataMap map = new DataMap();

        DbEntity e1 = new DbEntity("e1");
        DbAttribute nonPk = new DbAttribute("nonPk", Types.INTEGER, e1);
        e1.addAttribute(nonPk);

        map.addDbEntity(e1);

        // DbLoader is so ugly and hard to test..
        Field dbEntityList = DbLoader.class.getDeclaredField("dbEntityList");
        dbEntityList.setAccessible(true);
        List<DbEntity> entities = (List<DbEntity>) dbEntityList.get(loader1);
        entities.add(e1);

        loader1.loadObjEntities(map);

        ObjEntity oe1 = map.getObjEntity("E1");

        ObjAttribute oa1 = oe1.getAttribute("nonPk");
        assertEquals("java.lang.Integer", oa1.getType());
    }

    @Test
    public void testCreateLoader_UsePrimitives_True() throws Exception {
        DbImportConfiguration parameters = new DbImportConfiguration();
        parameters.setUsePrimitives(true);

        DbLoader loader1 = parameters.createLoader(mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        DataMap map = new DataMap();

        DbEntity e1 = new DbEntity("e1");
        DbAttribute nonPk = new DbAttribute("nonPk", Types.INTEGER, e1);
        e1.addAttribute(nonPk);

        map.addDbEntity(e1);

        // DbLoader is so ugly and hard to test..
        Field dbEntityList = DbLoader.class.getDeclaredField("dbEntityList");
        dbEntityList.setAccessible(true);
        List<DbEntity> entities = (List<DbEntity>) dbEntityList.get(loader1);
        entities.add(e1);

        loader1.loadObjEntities(map);

        ObjEntity oe1 = map.getObjEntity("E1");

        ObjAttribute oa1 = oe1.getAttribute("nonPk");
        assertEquals("int", oa1.getType());
    }


    @Test
    public void testCreateDataMap_New() throws Exception {
        URL outUrl = dataMapUrl("testCreateDataMap1.map.xml");

        File out = new File(outUrl.toURI());
        out.delete();
        assertFalse(out.isFile());

        DbImportConfiguration parameters = new DbImportConfiguration();
        parameters.setDataMapFile(out);
        DataMap dataMap = parameters.createDataMap();
        assertEquals("testCreateDataMap1", dataMap.getName());
        assertEquals(outUrl, dataMap.getConfigurationSource().getURL());
    }

    @Test
    public void testCreateDataMap_Existing() throws Exception {

        URL outUrl = dataMapUrl("testCreateDataMap2.map.xml");

        File out = new File(outUrl.toURI());
        out.delete();
        assertFalse(out.isFile());

        DataMap tempMap = new DataMap();
        tempMap.addDbEntity(new DbEntity("X"));

        PrintWriter writer = new PrintWriter(out);
        tempMap.encodeAsXML(new XMLEncoder(writer));
        writer.close();
        assertTrue(out.isFile());

        DbImportConfiguration parameters = new DbImportConfiguration();
        parameters.setDataMapFile(out);
        DataMap dataMap = parameters.createDataMap();
        assertEquals("testCreateDataMap2", dataMap.getName());
        assertEquals(outUrl, dataMap.getConfigurationSource().getURL());
    }

    private URL dataMapUrl(String name) throws MalformedURLException {
        String packagePath = getClass().getPackage().getName().replace('.', '/');
        URL packageUrl = getClass().getClassLoader().getResource(packagePath);
        assertNotNull(packageUrl);
        return new URL(packageUrl, "dbimport/" + name);
    }

}