/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.unit.jira;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.MapLoader;
import org.objectstyle.cayenne.project.DataMapFile;
import org.objectstyle.cayenne.project.ProjectFile;
import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * @author Andrei Adamchik
 */
public class CAY_236Tst extends BasicTestCase {

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
        assertTrue(contents.indexOf(TABLE1_BY) >= 0);
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

        public String getLocationSuffix() {
            return DataMapFile.LOCATION_SUFFIX;
        }

        public boolean canHandle(Object obj) {
            return false;
        }

        public Object getObject() {
            return map;
        }

        public String getObjectName() {
            return location;
        }

        public void save(PrintWriter out) throws Exception {
            map.encodeAsXML(out);
        }

        protected File tempFileForFile(File f) throws IOException {
            return file;
        }

        public File resolveFile() {
            return file;
        }
    }

}