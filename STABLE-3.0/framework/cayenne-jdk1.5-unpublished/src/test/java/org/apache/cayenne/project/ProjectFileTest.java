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

import java.io.PrintWriter;

import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class ProjectFileTest extends CayenneCase {
    protected ProjectFile pf;

    /**
    * @see junit.framework.TestCase#setUp()
    */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pf = new TestProjectFile("name.ext");
    }

    public void testSynchronizeLocation() throws Exception {
        assertEquals("name.ext", pf.location);
        pf.synchronizeLocation();
        assertEquals(TestProjectFile.OBJ_NAME, pf.location);
    }
    
    public void testRenamed() throws Exception {
    	assertTrue(pf.isRenamed());
    	pf.synchronizeLocation();
    	assertFalse(pf.isRenamed());
    }

    public void testLocation() throws Exception {
        assertEquals(TestProjectFile.OBJ_NAME, pf.getLocation());
    }

    public void testOldLocation() throws Exception {
        assertEquals("name.ext", pf.getOldLocation());
    }

    // inner class to allow testing of the abstract ProjectFile
    class TestProjectFile extends ProjectFile {
        public static final String OBJ_NAME = "obj";

        /**
         * Constructor for TestProjectFile.
         * @param name
         * @param extension
         */
        public TestProjectFile(String location) {
            super(null, location);
        }

        /**
         * @see org.apache.cayenne.project.ProjectFile#getObject()
         */
        @Override
        public Object getObject() {
            return null;
        }

        /**
         * @see org.apache.cayenne.project.ProjectFile#getObjectName()
         */
        @Override
        public String getObjectName() {
            return OBJ_NAME;
        }

        /**
         * @see org.apache.cayenne.project.ProjectFile#saveToFile(File)
         */
        @Override
        public void save(PrintWriter out) throws Exception {}

        /**
         * @see org.apache.cayenne.project.ProjectFile#createFileWrapper(Object)
         */
        public ProjectFile createProjectFile(Project project, Object obj) {
            return null;
        }

        /**
         * @see org.apache.cayenne.project.ProjectFile#isObjectSupported(Object)
         */
        @Override
        public boolean canHandle(Object obj) {
            return false;
        }
    }
}
