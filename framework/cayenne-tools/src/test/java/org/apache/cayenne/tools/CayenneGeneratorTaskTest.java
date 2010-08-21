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

package org.apache.cayenne.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.apache.cayenne.test.file.FileUtil;
import org.apache.cayenne.test.resource.ResourceUtil;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;

public class CayenneGeneratorTaskTest extends TestCase {

    private static final File baseDir;
    private static final File map;
    private static final File mapEmbeddables;
    private static final File template;

    static {

        baseDir = FileUtil.baseTestDirectory();
        map = new File(baseDir, "antmap.xml");
        mapEmbeddables = new File(baseDir, "antmap-embeddables.xml");
        template = new File(baseDir, "velotemplate.vm");

        ResourceUtil.copyResourceToFile("testmap.map.xml", map);
        ResourceUtil.copyResourceToFile("embeddable.map.xml", mapEmbeddables);
        ResourceUtil.copyResourceToFile(
                "org/apache/cayenne/tools/velotemplate.vm",
                template);
    }

    protected CayenneGeneratorTask task;

    @Override
    public void setUp() {

        Project project = new Project();
        project.setBaseDir(baseDir);

        task = new CayenneGeneratorTask();
        task.setProject(project);
        task.setTaskName("Test");
        task.setLocation(Location.UNKNOWN_LOCATION);
    }

    /**
     * Test single classes with a non-standard template.
     */
    public void testSingleClassesCustTemplate() throws Exception {
        // prepare destination directory
        File mapDir = new File(baseDir, "single-classes-custtempl");
        assertTrue(mapDir.mkdirs());

        // setup task
        task.setDestDir(mapDir);
        task.setMap(map);
        task.setMakepairs(false);
        task.setUsepkgpath(true);
        task.setTemplate(template.getPath());

        // run task
        task.execute();

        // check results
        File a = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/testmap/Artist.java"));
        assertTrue(a.isFile());
        assertContents(
                a,
                "Artist",
                "org.apache.cayenne.testdo.testmap",
                "CayenneDataObject");

        File _a = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/testmap/_Artist.java"));
        assertFalse(_a.exists());
    }

    /** Test single classes generation including full package path. */
    public void testSingleClasses1() throws Exception {
        // prepare destination directory
        File mapDir = new File(baseDir, "single-classes-tree");
        assertTrue(mapDir.mkdirs());

        // setup task
        task.setDestDir(mapDir);
        task.setMap(map);
        task.setMakepairs(false);
        task.setUsepkgpath(true);

        // run task
        task.execute();

        // check results
        File a = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/testmap/Artist.java"));
        assertTrue(a.isFile());
        assertContents(
                a,
                "Artist",
                "org.apache.cayenne.testdo.testmap",
                "CayenneDataObject");

        File _a = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/testmap/_Artist.java"));
        assertFalse(_a.exists());
    }

    /** Test single classes generation ignoring package path. */
    public void testSingleClasses2() throws Exception {
        // prepare destination directory
        File mapDir = new File(baseDir, "single-classes-flat");
        assertTrue(mapDir.mkdirs());

        // setup task
        task.setDestDir(mapDir);
        task.setMap(map);
        task.setMakepairs(false);
        task.setUsepkgpath(false);

        // run task
        task.execute();

        // check results
        File a = new File(mapDir, convertPath("Artist.java"));
        assertTrue(a.exists());
        assertContents(
                a,
                "Artist",
                "org.apache.cayenne.testdo.testmap",
                "CayenneDataObject");

        File _a = new File(mapDir, convertPath("_Artist.java"));
        assertFalse(_a.exists());

        File pkga = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/testmap/Artist.java"));
        assertFalse(pkga.exists());
    }

    /** Test pairs generation including full package path. */
    public void testPairs1() throws Exception {
        // prepare destination directory
        File mapDir = new File(baseDir, "pairs-tree");
        assertTrue(mapDir.mkdirs());

        // setup task
        task.setDestDir(mapDir);
        task.setMap(map);
        task.setMakepairs(true);
        task.setUsepkgpath(true);

        // run task
        task.execute();

        // check results
        File a = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/testmap/Artist.java"));
        assertTrue(a.isFile());
        assertContents(a, "Artist", "org.apache.cayenne.testdo.testmap", "_Artist");

        File _a = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/testmap/_Artist.java"));
        assertTrue(_a.exists());
        assertContents(
                _a,
                "_Artist",
                "org.apache.cayenne.testdo.testmap",
                "CayenneDataObject");
    }

    /** Test pairs generation in the same directory. */
    public void testPairs2() throws Exception {
        // prepare destination directory
        File mapDir = new File(baseDir, "pairs-flat");
        assertTrue(mapDir.mkdirs());

        // setup task
        task.setDestDir(mapDir);
        task.setMap(map);
        task.setMakepairs(true);
        task.setUsepkgpath(false);

        // run task
        task.execute();

        // check results
        File a = new File(mapDir, convertPath("Artist.java"));
        assertTrue(a.isFile());
        assertContents(a, "Artist", "org.apache.cayenne.testdo.testmap", "_Artist");

        File _a = new File(mapDir, convertPath("_Artist.java"));
        assertTrue(_a.exists());
        assertContents(
                _a,
                "_Artist",
                "org.apache.cayenne.testdo.testmap",
                "CayenneDataObject");

        File pkga = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/testmap/Artist.java"));
        assertFalse(pkga.exists());
    }

    /**
     * Test pairs generation including full package path with superclass and subclass in
     * different packages.
     */
    public void testPairs3() throws Exception {
        // prepare destination directory
        File mapDir = new File(baseDir, "pairs-tree-split");
        assertTrue(mapDir.mkdirs());

        // setup task
        task.setDestDir(mapDir);
        task.setMap(map);
        task.setMakepairs(true);
        task.setUsepkgpath(true);
        task.setSuperpkg("org.apache.cayenne.testdo.testmap.superart");

        // run task
        task.execute();

        // check results
        File a = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/testmap/Artist.java"));
        assertTrue(a.isFile());
        assertContents(a, "Artist", "org.apache.cayenne.testdo.testmap", "_Artist");

        File _a = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/testmap/superart/_Artist.java"));
        assertTrue(_a.exists());
        assertContents(
                _a,
                "_Artist",
                "org.apache.cayenne.testdo.testmap.superart",
                "CayenneDataObject");
    }

    public void testPairsEmbeddable3() throws Exception {
        // prepare destination directory
        File mapDir = new File(baseDir, "pairs-embeddables3-split");
        assertTrue(mapDir.mkdirs());

        // setup task
        task.setDestDir(mapDir);
        task.setMap(mapEmbeddables);
        task.setMakepairs(true);
        task.setUsepkgpath(true);
        task.setSuperpkg("org.apache.cayenne.testdo.embeddable.auto");

        // run task
        task.execute();

        // check entity results
        File a = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/embeddable/EmbedEntity1.java"));
        assertTrue(a.isFile());
        assertContents(
                a,
                "EmbedEntity1",
                "org.apache.cayenne.testdo.embeddable",
                "_EmbedEntity1");

        File _a = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/embeddable/auto/_EmbedEntity1.java"));
        assertTrue(_a.exists());
        assertContents(
                _a,
                "_EmbedEntity1",
                "org.apache.cayenne.testdo.embeddable.auto",
                "CayenneDataObject");

        // check embeddable results
        File e = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/embeddable/Embeddable1.java"));
        assertTrue(e.isFile());
        assertContents(
                e,
                "Embeddable1",
                "org.apache.cayenne.testdo.embeddable",
                "_Embeddable1");

        File _e = new File(
                mapDir,
                convertPath("org/apache/cayenne/testdo/embeddable/auto/_Embeddable1.java"));
        assertTrue(_e.exists());
        assertContents(
                _e,
                "_Embeddable1",
                "org.apache.cayenne.testdo.embeddable.auto",
                "Object");
    }

    private String convertPath(String unixPath) {
        return unixPath.replace('/', File.separatorChar);
    }

    private void assertContents(
            File f,
            String className,
            String packageName,
            String extendsName) throws Exception {

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(
                f)));

        try {
            assertPackage(in, packageName);
            assertClass(in, className, extendsName);
        }
        finally {
            in.close();
        }

    }

    private void assertPackage(BufferedReader in, String packageName) throws Exception {

        String s = null;
        while ((s = in.readLine()) != null) {
            if (Pattern.matches("^package\\s+([^\\s;]+);", s)) {
                assertTrue(s.indexOf(packageName) > 0);
                return;
            }
        }

        fail("No package declaration found.");
    }

    private void assertClass(BufferedReader in, String className, String extendsName)
            throws Exception {

        Pattern classPattern = Pattern.compile("^public\\s+");

        String s = null;
        while ((s = in.readLine()) != null) {
            if (classPattern.matcher(s).find()) {
                assertTrue(s.indexOf(className) > 0);
                assertTrue(s.indexOf(extendsName) > 0);
                assertTrue(s.indexOf(className) < s.indexOf(extendsName));
                return;
            }
        }

        fail("No class declaration found.");
    }
}
