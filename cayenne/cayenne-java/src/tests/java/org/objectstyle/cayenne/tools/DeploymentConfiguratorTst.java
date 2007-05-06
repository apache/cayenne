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

import java.io.File;
import java.net.URL;

import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.project.DataNodeConfigInfo;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.ZipUtil;

/**
 * @author Andrei Adamchik
 */
public class DeploymentConfiguratorTst extends CayenneTestCase {

    private static final File baseDir = new File(CayenneTestResources
            .getResources()
            .getTestDir(), "cdeploy");
    private static File src = new File(baseDir, "cdeploy-test.jar");
    private static File altFile = new File(baseDir, "alt-cayenne.xml");
    private static File altNodeFile = new File(baseDir, "alt-node1.xml");

    protected DeploymentConfigurator task;
    protected Project project;
    protected File dest;

    static {
        extractFiles();
    }

    private static void extractFiles() {
        baseDir.mkdirs();

        ResourceLocator locator = new ResourceLocator();
        locator.setSkipAbsolutePath(true);
        locator.setSkipClasspath(false);
        locator.setSkipCurrentDirectory(true);
        locator.setSkipHomeDirectory(true);

        URL url1 = locator.findResource("cdeploy/cdeploy-test.jar");
        Util.copy(url1, src);

        URL url2 = locator.findResource("cdeploy/alt-cayenne.xml");
        Util.copy(url2, altFile);

        URL url3 = locator.findResource("cdeploy/alt-node1.xml");
        Util.copy(url3, altNodeFile);
    }

    public void setUp() throws Exception {
        // create test dir
        File testDir = null;
        for (int i = 1; i < 50; i++) {
            File tmp = new File(baseDir, "test" + i);
            if (!tmp.exists()) {
                tmp.mkdirs();
                testDir = tmp;
                break;
            }
        }

        project = new Project();
        project.setBaseDir(testDir);

        dest = new File(project.getBaseDir(), "test-out.jar");

        task = new DeploymentConfigurator();
        task.setProject(project);
        task.setTaskName("Test");
        task.setLocation(Location.UNKNOWN_LOCATION);
        task.setSrc(src);
        task.setDest(dest);

        // assert setup success
        assertTrue(testDir.isDirectory());
        assertFalse(dest.exists());
        assertSame(dest, task.getInfo().getDestJar());
        assertSame(src, task.getInfo().getSourceJar());
    }

    public void testPassThrough() throws Exception {
        // run task
        task.execute();

        // check results
        assertTrue(dest.isFile());
    }

    public void testAltFile() throws Exception {
        task.setAltProjectFile(altFile);

        // run task
        task.execute();

        // check results
        assertTrue(dest.isFile());

        ZipUtil.unzip(dest, project.getBaseDir());
        File newRoot = new File(project.getBaseDir(), Configuration.DEFAULT_DOMAIN_FILE);
        assertTrue(newRoot.isFile());
        assertEquals(altFile.length(), newRoot.length());
    }

    public void testAltNode() throws Exception {
        DataNodeConfigInfo node = new DataNodeConfigInfo();
        node.setName("node1");
        node.setAdapter("non-existent-adapter");

        task.addNode(node);

        // run task
        task.execute();

        // check results
        assertTrue(dest.isFile());

        ZipUtil.unzip(dest, project.getBaseDir());
        File newRoot = new File(project.getBaseDir(), Configuration.DEFAULT_DOMAIN_FILE);

        String fileContents = Util.stringFromFile(newRoot);
        assertTrue(fileContents.indexOf(node.getName()) >= 0);
        assertTrue(fileContents.indexOf(node.getAdapter()) >= 0);
    }

    public void testSameJar() throws Exception {
        Util.copy(src, dest);
        long size = dest.length();
        task.setSrc(dest);
        task.setAltProjectFile(altFile);

        // run task
        task.execute();

        // check results
        assertFalse(size == dest.length());
    }
}
