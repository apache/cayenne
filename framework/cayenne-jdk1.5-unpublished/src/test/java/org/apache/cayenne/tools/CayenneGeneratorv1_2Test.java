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

import junit.framework.TestCase;

import org.apache.oro.text.perl.Perl5Util;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.Path;
import org.apache.cayenne.unit.CayenneResources;

public class CayenneGeneratorv1_2Test extends TestCase {

    private static final Perl5Util regexUtil = new Perl5Util();
    private static final Project project = new Project();

    protected CayenneGenerator task;

    public void setUp() throws java.lang.Exception {
        task = new CayenneGenerator();
        task.setProject(project);
        task.setTaskName("Test");
        task.setLocation(Location.UNKNOWN_LOCATION);
    }

    /** Test pairs generation with a cross-DataMap relationship (v1.1). */
    public void testCrossDataMapRelationships_v1_1() throws Exception {

        // prepare destination directory

        File destDir = new File(
                CayenneResources.getResources().getTestDir(),
                "cgen11");
        // prepare destination directory
        if (!destDir.exists()) {
            assertTrue(destDir.mkdirs());
        }

        File map = new File(destDir, "testmap-dependent.map.xml");
        CayenneResources.copyResourceToFile("testmap-dependent.map.xml", map);

        File additionalMaps[] = new File[1];
        additionalMaps[0] = new File(destDir, "testmap.map.xml");
        CayenneResources.copyResourceToFile("testmap.map.xml", additionalMaps[0]);

        FileList additionalMapsFilelist = new FileList();
        additionalMapsFilelist.setDir(additionalMaps[0].getParentFile());
        additionalMapsFilelist.setFiles(additionalMaps[0].getName());

        Path additionalMapsPath = new Path(task.getProject());
        additionalMapsPath.addFilelist(additionalMapsFilelist);

        // setup task
        task.setMap(map);
        task.setAdditionalMaps(additionalMapsPath);
        task.setVersion("1.1");
        task.setMakepairs(true);
        task.setOverwrite(false);
        task.setMode("entity");
        task.setIncludeEntities("MyArtGroup");
        task.setDestDir(destDir);
        task.setSuperpkg("org.apache.art2.auto");
        task.setUsepkgpath(true);

        // run task
        task.execute();

        // check results
        File a = new File(destDir, convertPath("org/apache/art2/MyArtGroup.java"));
        assertTrue(a.isFile());
        assertContents(a, "MyArtGroup", "org.apache.art2", "_MyArtGroup");

        File _a = new File(destDir, convertPath("org/apache/art2/auto/_MyArtGroup.java"));
        assertTrue(_a.exists());
        assertContents(_a, "_MyArtGroup", "org.apache.art2.auto", "CayenneDataObject");
        assertContents(_a, "org.apache.art.ArtGroup getToParentGroup()");
        assertContents(_a, "setToParentGroup(org.apache.art.ArtGroup toParentGroup)");
    }

    /** Test pairs generation with a cross-DataMap relationship. */
    public void testCrossDataMapRelationships() throws Exception {
        // prepare destination directory

        File destDir = new File(
                CayenneResources.getResources().getTestDir(),
                "cgen12");
        // prepare destination directory
        if (!destDir.exists()) {
            assertTrue(destDir.mkdirs());
        }

        File map = new File(destDir, "testmap-dependent.map.xml");
        CayenneResources.copyResourceToFile("testmap-dependent.map.xml", map);

        File additionalMaps[] = new File[1];
        additionalMaps[0] = new File(destDir, "testmap.map.xml");
        CayenneResources.copyResourceToFile("testmap.map.xml", additionalMaps[0]);

        FileList additionalMapsFilelist = new FileList();
        additionalMapsFilelist.setDir(additionalMaps[0].getParentFile());
        additionalMapsFilelist.setFiles(additionalMaps[0].getName());

        Path additionalMapsPath = new Path(task.getProject());
        additionalMapsPath.addFilelist(additionalMapsFilelist);

        // setup task
        task.setMap(map);
        task.setAdditionalMaps(additionalMapsPath);
        task.setVersion("1.2");
        task.setMakepairs(true);
        task.setOverwrite(false);
        task.setMode("entity");
        task.setIncludeEntities("MyArtGroup");
        task.setDestDir(destDir);
        task.setSuperpkg("org.apache.art2.auto");
        task.setUsepkgpath(true);

        // run task
        task.execute();

        // check results
        File a = new File(destDir, convertPath("org/apache/art2/MyArtGroup.java"));
        assertTrue(a.isFile());
        assertContents(a, "MyArtGroup", "org.apache.art2", "_MyArtGroup");

        File _a = new File(destDir, convertPath("org/apache/art2/auto/_MyArtGroup.java"));
        assertTrue(_a.exists());
        assertContents(_a, "_MyArtGroup", "org.apache.art2.auto", "CayenneDataObject");
        assertContents(_a, "import org.apache.art.ArtGroup;");
        assertContents(_a, " ArtGroup getToParentGroup()");
        assertContents(_a, "setToParentGroup(ArtGroup toParentGroup)");
    }

    private String convertPath(String unixPath) {
        return unixPath.replace('/', File.separatorChar);
    }

    private void assertContents(File f, String content) throws Exception {

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(
                f)));

        try {
            String s = null;
            while ((s = in.readLine()) != null) {
                if (s.indexOf(content) >= 0)
                    return;
            }

            fail("<" + content + "> not found in " + f.getAbsolutePath() + ".");
        }
        finally {
            in.close();
        }

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
                assertTrue(s.indexOf(packageName) >= 0);
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
                assertTrue(s.indexOf(className) >= 0);
                assertTrue(s.indexOf(extendsName) >= 0);
                assertTrue(s.indexOf(className) < s.indexOf(extendsName));
                return;
            }
        }

        fail("No class declaration found.");
    }
}
