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

package org.apache.cayenne.project;

import java.io.File;
import java.net.URL;

import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.util.Util;

/**
 * @deprecated since 3.0. {@link ProjectConfigurator} approach turned out to be not
 *             usable, and is in fact rarely used (if ever). It will be removed in
 *             subsequent releases.
 */
public class PartialProjectTest extends CayenneCase {

    protected File testProjectFile;
    protected PartialProject project;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create new test directory, copy cayenne.xml in there
        File baseDir = super.getTestDir();
        for (int i = 1; i < 100; i++) {
            File tmpDir = new File(baseDir, "partial-project-" + i);
            if (!tmpDir.exists()) {
                if (!tmpDir.mkdir()) {
                    throw new Exception("Can't create " + tmpDir);
                }

                testProjectFile = new File(tmpDir, Configuration.DEFAULT_DOMAIN_FILE);
                break;
            }
        }

        // copy cayenne.xml
        URL src = Thread.currentThread().getContextClassLoader().getResource(
                "lightweight-cayenne.xml");
        if (!Util.copy(src, testProjectFile)) {
            throw new Exception("Can't copy from " + src);
        }

        project = new PartialProject(testProjectFile);
    }

    public void testParentFile() throws Exception {
        assertEquals(testProjectFile.getParentFile().getCanonicalFile(), project
                .getProjectDirectory()
                .getCanonicalFile());
    }

    public void testProjectFile() throws Exception {
        ProjectFile f = project.findFile(project);
        assertNotNull(f);
        assertTrue(
                "Wrong main file type: " + f.getClass().getName(),
                f instanceof ApplicationProjectFile);
        assertNotNull("Null delegate", ((ApplicationProjectFile) f).getSaveDelegate());
    }

    public void testMainFile() throws Exception {
        assertEquals(project.findFile(project).resolveFile(), project.getMainFile());
    }

    public void testDomains() throws Exception {
        assertEquals(2, project.getChildren().size());
    }

    public void testNodes() throws Exception {
        PartialProject.DomainMetaData d2 = project.domains.get("d2");
        assertNotNull(d2);
        assertEquals(2, d2.nodes.size());
    }

    public void testSave() throws Exception {
        if (!testProjectFile.delete()) {
            throw new Exception("Can't delete project file: " + testProjectFile);
        }

        PartialProject old = project;
        old.save();

        assertTrue(testProjectFile.exists());

        // reinit shared project and run one of the other tests
        project = new PartialProject(testProjectFile);
        testNodes();
    }
}
