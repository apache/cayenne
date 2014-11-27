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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;

public class DbImportConfigurationTest {

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