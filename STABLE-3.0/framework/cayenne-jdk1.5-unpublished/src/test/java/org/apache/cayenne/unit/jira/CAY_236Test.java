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

package org.apache.cayenne.unit.jira;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.project.DataMapFile;
import org.apache.cayenne.project.ProjectFile;
import org.apache.cayenne.unit.BasicCase;

/**
 */
public class CAY_236Test extends BasicCase {

    private static final String TABLE1_BY = "\u0442\u0430\u0431\u043bi\u0446\u04301";

    public void testLoadUTF8() throws Exception {
        MapLoader loader = new MapLoader();

        // the idea here is to avoid passing the reader to the loader, and make sure the
        // loader does the right thing (i.e. loads UTF-8 encoded file) itself.
        DataMap map = loader.loadDataMap("i18n/by/DataMap.map.xml");
        assertNotNull(map.getDbEntity(TABLE1_BY));
    }

    public void testStoreUTF8() throws Exception {
        MapLoader loader = new MapLoader();

        // the idea here is to avoid passing the reader to the loader, and make sure the
        // loader does the right thing (i.e. loads UTF-8 encoded file) itself.
        DataMap map = loader.loadDataMap("i18n/by/DataMap.map.xml");

        File mapFile = new File(getTestDir(), "CAY_236Map.map.xml");
        TestProjectFile file = new TestProjectFile(map, "DataMap", mapFile);
        file.saveTemp();

        String contents = fileContents(mapFile, "UTF-8");
        assertTrue(contents.contains(TABLE1_BY));
    }

    String fileContents(File f, String encoding) throws IOException {
        FileInputStream fin = new FileInputStream(f);
        InputStreamReader fr = new InputStreamReader(fin, "UTF-8");

        BufferedReader in = new BufferedReader(fr);
        StringBuffer buf = new StringBuffer();

        try {
            String line;
            while ((line = in.readLine()) != null) {
                buf.append(line).append('\n');
            }
        }
        finally {
            try {
                in.close();
            }
            catch (IOException ioex) {

            }
        }

        return buf.toString();
    }

    class TestProjectFile extends ProjectFile {

        DataMap map;
        String location;
        File file;

        TestProjectFile(DataMap map, String location, File file) {
            this.map = map;
            this.location = location;
            this.file = file;
        }

        @Override
        public String getLocationSuffix() {
            return DataMapFile.LOCATION_SUFFIX;
        }

        @Override
        public boolean canHandle(Object obj) {
            return false;
        }

        @Override
        public Object getObject() {
            return map;
        }

        @Override
        public String getObjectName() {
            return location;
        }

        @Override
        public void save(PrintWriter out) throws Exception {
            map.encodeAsXML(out);
        }

        @Override
        protected File tempFileForFile(File f) throws IOException {
            return file;
        }

        @Override
        public File resolveFile() {
            return file;
        }
    }

}
