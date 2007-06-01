/*
 * ==================================================================== The ObjectStyle
 * Group Software License, version 1.1 ObjectStyle Group - http://objectstyle.org/
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors of the
 * software. All rights reserved. Redistribution and use in source and binary forms, with
 * or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must
 * reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. The end-user documentation included with the redistribution, if any, must include
 * the following acknowlegement: "This product includes software developed by independent
 * contributors and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and wherever
 * such third-party acknowlegements normally appear. 4. The names "ObjectStyle Group" and
 * "Cayenne" must not be used to endorse or promote products derived from this software
 * without prior written permission. For written permission, email "andrus at objectstyle
 * dot org". 5. Products derived from this software may not be called "ObjectStyle" or
 * "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their names without prior
 * written permission. THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE OBJECTSTYLE
 * GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ==================================================================== This software
 * consists of voluntary contributions made by many individuals and hosted on ObjectStyle
 * Group web site. For more information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.oro.text.perl.Perl5Util;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.util.Util;

public class CayenneGeneratorTst extends CayenneTestCase {

    private static final Perl5Util regexUtil = new Perl5Util();
    private static final Project project = new Project();
    private static final File baseDir = CayenneTestResources.getResources().getTestDir();
    private static final File map = new File(baseDir, "antmap.xml");
    private static final File template = new File(baseDir, "velotemplate.vm");

    static {
        extractFiles();
        project.setBaseDir(baseDir);
    }

    protected CayenneGenerator task;

    private static void extractFiles() {
        ResourceLocator locator = new ResourceLocator();
        locator.setSkipAbsolutePath(true);
        locator.setSkipClasspath(false);
        locator.setSkipCurrentDirectory(true);
        locator.setSkipHomeDirectory(true);

        // Configuration superclass statically defines what
        // ClassLoader to use for resources. This
        // allows applications to control where resources
        // are loaded from.
        locator.setClassLoader(Configuration.getResourceLoader());

        URL url1 = locator.findResource("test-resources/testmap.map.xml");
        Util.copy(url1, map);
        URL url2 = locator.findResource("test-resources/testtemplate.vm");
        Util.copy(url2, template);
    }

    public void setUp() throws java.lang.Exception {
        task = new CayenneGenerator();
        task.setProject(project);
        task.setTaskName("Test");
        task.setLocation(Location.UNKNOWN_LOCATION);
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
        task.setTemplate(template);

        // run task
        task.execute();

        // check results
        File a = new File(mapDir, convertPath("org/objectstyle/art/Artist.java"));
        assertTrue(a.isFile());
        assertContents(a, "Artist", "org.objectstyle.art", "CayenneDataObject");

        File _a = new File(mapDir, convertPath("org/objectstyle/art/_Artist.java"));
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
        File a = new File(mapDir, convertPath("org/objectstyle/art/Artist.java"));
        assertTrue(a.isFile());
        assertContents(a, "Artist", "org.objectstyle.art", "CayenneDataObject");

        File _a = new File(mapDir, convertPath("org/objectstyle/art/_Artist.java"));
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
        assertContents(a, "Artist", "org.objectstyle.art", "CayenneDataObject");

        File _a = new File(mapDir, convertPath("_Artist.java"));
        assertFalse(_a.exists());

        File pkga = new File(mapDir, convertPath("org/objectstyle/art/Artist.java"));
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
        File a = new File(mapDir, convertPath("org/objectstyle/art/Artist.java"));
        assertTrue(a.isFile());
        assertContents(a, "Artist", "org.objectstyle.art", "_Artist");

        File _a = new File(mapDir, convertPath("org/objectstyle/art/_Artist.java"));
        assertTrue(_a.exists());
        assertContents(_a, "_Artist", "org.objectstyle.art", "CayenneDataObject");
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
        assertContents(a, "Artist", "org.objectstyle.art", "_Artist");

        File _a = new File(mapDir, convertPath("_Artist.java"));
        assertTrue(_a.exists());
        assertContents(_a, "_Artist", "org.objectstyle.art", "CayenneDataObject");

        File pkga = new File(mapDir, convertPath("org/objectstyle/art/Artist.java"));
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
        task.setSuperpkg("org.objectstyle.superart");

        // run task
        task.execute();

        // check results
        File a = new File(mapDir, convertPath("org/objectstyle/art/Artist.java"));
        assertTrue(a.isFile());
        assertContents(a, "Artist", "org.objectstyle.art", "_Artist");

        File _a = new File(mapDir, convertPath("org/objectstyle/superart/_Artist.java"));
        assertTrue(_a.exists());
        assertContents(_a, "_Artist", "org.objectstyle.superart", "CayenneDataObject");
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