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

import org.apache.cayenne.test.file.FileUtil;
import org.apache.cayenne.test.resource.ResourceUtil;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @since 4.1
 */
public class CgenWithConfigTest {

    private static final File baseDir;
    private static final File map;

    static {
        baseDir = FileUtil.baseTestDirectory();
        map = new File(baseDir, "antmap-cgen.xml");

        ResourceUtil.copyResourceToFile("cgenTest.map.xml", map);
    }

    protected CayenneGeneratorTask task;

    @Before
    public void setUp() {

        Project project = new Project();
        project.setBaseDir(baseDir);

        task = new CayenneGeneratorTask();
        task.setProject(project);
        task.setTaskName("Test");
        task.setLocation(Location.UNKNOWN_LOCATION);
    }

    @Test
    public void testCgenWithDmConfig() {
        File mapDir = new File(baseDir, "cgenConfigTest");
        assertTrue(mapDir.mkdirs());

        task.setMap(map);

        // run task
        task.execute();

        // check results
        File entity = new File(mapDir, convertPath("ObjEntity1.txt"));
        assertTrue(entity.isFile());

        File datamap = new File(mapDir, convertPath("Antmap_cgen_xml.txt"));
        assertTrue(datamap.isFile());

        File notIncludedEntity = new File(mapDir, "ObjEntity.txt");
        assertFalse(notIncludedEntity.exists());

        File notIncludeSuperDatamap = new File("_Antmap_cgen_xml.txt");
        assertFalse(notIncludeSuperDatamap.exists());
    }

    @Test
    public void testCgenWithDmAndPomConfigs() {
        File mapDir = new File(baseDir, "cgenDmPomTest");
        assertTrue(mapDir.mkdirs());

        task.setDestDir(mapDir);
        task.setMap(map);
        task.setExcludeEntities("ObjEntity1");
        task.setMode("entity");
        task.setMakepairs(false);
        task.setOutputPattern("*.txt");

        // run task
        task.execute();

        // check results
        File entity = new File(mapDir, convertPath("ObjEntity.txt"));
        assertTrue(entity.isFile());

        File embeddable = new File(mapDir, convertPath("Embeddable.txt"));
        assertTrue(embeddable.isFile());

        File datamap = new File(mapDir, convertPath("Antmap_cgen_xml.txt"));
        assertFalse(datamap.exists());

        File notIncludedEntity = new File(mapDir, "ObjEntity1.txt");
        assertFalse(notIncludedEntity.exists());

        File notIncludeSuperDatamap = new File("_Antmap_cgen_xml.txt");
        assertFalse(notIncludeSuperDatamap.exists());

        File notIncludedSuperEntity = new File(mapDir, convertPath("_ObjEntity.txt"));
        assertFalse(notIncludedSuperEntity.exists());
    }

    private String convertPath(String unixPath) {
        return unixPath.replace('/', File.separatorChar);
    }
}
