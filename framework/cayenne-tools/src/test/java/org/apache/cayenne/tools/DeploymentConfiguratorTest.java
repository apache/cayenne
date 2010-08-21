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

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.project.DataNodeConfigInfo;
import org.apache.cayenne.test.file.FileUtil;
import org.apache.cayenne.util.ResourceLocator;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.ZipUtil;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;

/**
 */
public class DeploymentConfiguratorTest extends TestCase {

    private static final File baseDir = new File(FileUtil.baseTestDirectory(), "cdeploy");
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

    @Override
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
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        task = null;
        project = null;
        dest = null;
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
        assertTrue(fileContents.contains(node.getName()));
        assertTrue(fileContents.contains(node.getAdapter()));
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
