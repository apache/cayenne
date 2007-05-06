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
package org.objectstyle.cayenne.tools;

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
import org.objectstyle.cayenne.unit.CayenneTestResources;

public class CayenneGeneratorv1_2Tst extends TestCase {

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

        File destDir = new File("testrun/cgen11");
        // prepare destination directory
        if (!destDir.exists()) {
            assertTrue(destDir.mkdirs());
        }

        File map = new File(destDir, "testmap-dependent.map.xml");
        CayenneTestResources.copyResourceToFile("testmap-dependent.map.xml", map);

        File additionalMaps[] = new File[1];
        additionalMaps[0] = new File(destDir, "testmap.map.xml");
        CayenneTestResources.copyResourceToFile("testmap.map.xml", additionalMaps[0]);

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
        task.setSuperpkg("org.objectstyle.art2.auto");
        task.setUsepkgpath(true);

        // run task
        task.execute();

        // check results
        File a = new File(destDir, convertPath("org/objectstyle/art2/MyArtGroup.java"));
        assertTrue(a.isFile());
        assertContents(a, "MyArtGroup", "org.objectstyle.art2", "_MyArtGroup");

        File _a = new File(
                destDir,
                convertPath("org/objectstyle/art2/auto/_MyArtGroup.java"));
        assertTrue(_a.exists());
        assertContents(
                _a,
                "_MyArtGroup",
                "org.objectstyle.art2.auto",
                "CayenneDataObject");
        assertContents(_a, "org.objectstyle.art.ArtGroup getToParentGroup()");
        assertContents(_a, "setToParentGroup(org.objectstyle.art.ArtGroup toParentGroup)");
    }

    /** Test pairs generation with a cross-DataMap relationship. */
    public void testCrossDataMapRelationships() throws Exception {
        // prepare destination directory

        File destDir = new File("testrun/cgen12");
        // prepare destination directory
        if (!destDir.exists()) {
            assertTrue(destDir.mkdirs());
        }

        File map = new File(destDir, "testmap-dependent.map.xml");
        CayenneTestResources.copyResourceToFile("testmap-dependent.map.xml", map);

        File additionalMaps[] = new File[1];
        additionalMaps[0] = new File(destDir, "testmap.map.xml");
        CayenneTestResources.copyResourceToFile("testmap.map.xml", additionalMaps[0]);

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
        task.setSuperpkg("org.objectstyle.art2.auto");
        task.setUsepkgpath(true);

        // run task
        task.execute();

        // check results
        File a = new File(destDir, convertPath("org/objectstyle/art2/MyArtGroup.java"));
        assertTrue(a.isFile());
        assertContents(a, "MyArtGroup", "org.objectstyle.art2", "_MyArtGroup");

        File _a = new File(
                destDir,
                convertPath("org/objectstyle/art2/auto/_MyArtGroup.java"));
        assertTrue(_a.exists());
        assertContents(
                _a,
                "_MyArtGroup",
                "org.objectstyle.art2.auto",
                "CayenneDataObject");
        assertContents(_a, "import org.objectstyle.art.ArtGroup;");
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
