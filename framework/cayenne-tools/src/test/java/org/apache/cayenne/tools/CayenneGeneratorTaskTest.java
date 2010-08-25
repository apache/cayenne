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
import java.net.URL;

import junit.framework.TestCase;

import org.apache.cayenne.test.file.FileUtil;
import org.apache.cayenne.util.ResourceLocator;
import org.apache.cayenne.util.Util;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;

public class CayenneGeneratorTaskTest extends TestCase {

    private static final Perl5Util regexUtil = new Perl5Util();
    private static final Project project = new Project();
    private static final File baseDir = FileUtil.baseTestDirectory();
    private static final File map = new File(baseDir, "antmap.xml");
    private static final File mapEmbeddables = new File(baseDir, "antmap-embeddables.xml");
    private static final File template = new File(baseDir, "velotemplate.vm");

    static {
        extractFiles();
        project.setBaseDir(baseDir);
    }

    protected CayenneGeneratorTask task;

    private static void extractFiles() {
        ResourceLocator locator = new ResourceLocator();
        locator.setSkipAbsolutePath(true);
        locator.setSkipClasspath(false);
        locator.setSkipCurrentDirectory(true);
        locator.setSkipHomeDirectory(true);

        URL url1 = locator.findResource("testmap.map.xml");
        Util.copy(url1, map);
        URL url2 = locator.findResource("testtemplate.vm");
        Util.copy(url2, template);

        URL url3 = locator.findResource("embeddable.map.xml");
        Util.copy(url3, mapEmbeddables);
    }

    @Override
    public void setUp() {
        task = new CayenneGeneratorTask();
        task.setProject(project);
        task.setTaskName("Test");
        task.setLocation(Location.UNKNOWN_LOCATION);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        task = null;
    }

    /** Test single classes with a non-standard template. */
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
        File a = new File(mapDir, convertPath("org/apache/art/Artist.java"));
        assertTrue(a.isFile());
        assertContents(a, "Artist", "org.apache.art", "CayenneDataObject");

        File _a = new File(mapDir, convertPath("org/apache/art/_Artist.java"));
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
        File a = new File(mapDir, convertPath("org/apache/art/Artist.java"));
        assertTrue(a.isFile());
        assertContents(a, "Artist", "org.apache.art", "CayenneDataObject");

        File _a = new File(mapDir, convertPath("org/apache/art/_Artist.java"));
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
        assertContents(a, "Artist", "org.apache.art", "CayenneDataObject");

        File _a = new File(mapDir, convertPath("_Artist.java"));
        assertFalse(_a.exists());

        File pkga = new File(mapDir, convertPath("org/apache/art/Artist.java"));
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
        File a = new File(mapDir, convertPath("org/apache/art/Artist.java"));
        assertTrue(a.isFile());
        assertContents(a, "Artist", "org.apache.art", "_Artist");

        File _a = new File(mapDir, convertPath("org/apache/art/_Artist.java"));
        assertTrue(_a.exists());
        assertContents(_a, "_Artist", "org.apache.art", "CayenneDataObject");
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
        assertContents(a, "Artist", "org.apache.art", "_Artist");

        File _a = new File(mapDir, convertPath("_Artist.java"));
        assertTrue(_a.exists());
        assertContents(_a, "_Artist", "org.apache.art", "CayenneDataObject");

        File pkga = new File(mapDir, convertPath("org/apache/art/Artist.java"));
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
        task.setSuperpkg("org.apache.superart");

        // run task
        task.execute();

        // check results
        File a = new File(mapDir, convertPath("org/apache/art/Artist.java"));
        assertTrue(a.isFile());
        assertContents(a, "Artist", "org.apache.art", "_Artist");

        File _a = new File(mapDir, convertPath("org/apache/superart/_Artist.java"));
        assertTrue(_a.exists());
        assertContents(_a, "_Artist", "org.apache.superart", "CayenneDataObject");
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
            if (regexUtil.match("/^package\\s+([^\\s;]+);/", s + '\n')) {
                assertTrue(s.indexOf(packageName) > 0);
                return;
            }
        }

        fail("No package declaration found.");
    }

    private void assertClass(BufferedReader in, String className, String extendsName)
            throws Exception {

        String s = null;
        while ((s = in.readLine()) != null) {
            if (regexUtil.match("/class\\s+([^\\s]+)\\s+extends\\s+([^\\s]+)/", s + '\n')) {
                assertTrue(s.indexOf(className) > 0);
                assertTrue(s.indexOf(extendsName) > 0);
                assertTrue(s.indexOf(className) < s.indexOf(extendsName));
                return;
            }
        }

        fail("No class declaration found.");
    }
}
