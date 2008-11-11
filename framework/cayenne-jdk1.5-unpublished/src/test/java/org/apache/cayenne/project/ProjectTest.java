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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.project.validator.Validator;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class ProjectTest extends CayenneCase {

    protected Project p;
    protected File f;

    /**
      * @see junit.framework.TestCase#setUp()
      */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        f = new File("xyz");
        p = new TstProject(f);
    }

    public void testModified() throws Exception {
    	assertFalse(p.isModified());
    	p.setModified(true);
    	assertTrue(p.isModified());    	
    }
    
    public void testValidator() throws Exception {
        Validator v1 = p.getValidator();
        assertSame(p, v1.getProject());

        Validator v2 = p.getValidator();
        assertSame(p, v2.getProject());

        assertTrue(v1 != v2);
    }

    public void testProcessSave() throws Exception {
        List list = new ArrayList();
        SaveEmulator file = new SaveEmulator(false);
        list.add(file);
        list.add(file);

        p.processSave(list);
        assertEquals(2, file.saveTempCount);
        assertEquals(0, file.commitCount);
        assertEquals(0, file.undoCount);
    }

    public void testProcessSaveFail() throws Exception {
        List list = new ArrayList();
        SaveEmulator file = new SaveEmulator(true);
        list.add(file);

        try {
            p.processSave(list);
            fail("Save must have failed.");
        } catch (ProjectException ex) {
            // exception expected
            assertEquals(1, file.saveTempCount);
            assertEquals(0, file.commitCount);
            assertEquals(1, file.undoCount);
        }
    }


    class SaveEmulator extends ProjectFile {
        protected int commitCount;
        protected int undoCount;
        protected int deleteCount;
        protected int saveTempCount;
        protected boolean shouldFail;

        public SaveEmulator(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        /**
         * @see org.apache.cayenne.project.ProjectFile#canHandle(Object)
         */
        @Override
        public boolean canHandle(Object obj) {
            return false;
        }

        /**
         * @see org.apache.cayenne.project.ProjectFile#createProjectFile(Object)
         */
        public ProjectFile createProjectFile(Project project, Object obj) {
            return null;
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
            return null;
        }

        /**
         * @see org.apache.cayenne.project.ProjectFile#saveToFile(File)
         */
        @Override
        public void save(PrintWriter out) throws Exception {
        }

        /**
         * @see org.apache.cayenne.project.ProjectFile#saveCommit()
         */
        @Override
        public File saveCommit() {
            commitCount++;
            return new File("abc");
        }

        /**
         * @see org.apache.cayenne.project.ProjectFile#saveDelete()
         */
        public boolean saveDelete() {
            deleteCount++;
            return !shouldFail;
        }

        /**
         * @see org.apache.cayenne.project.ProjectFile#saveTemp()
         */
        @Override
        public void saveTemp() throws Exception {
            saveTempCount++;
            
            if(shouldFail) {
            	throw new Exception("You forced me to fail...");
            }
        }

        /**
         * @see org.apache.cayenne.project.ProjectFile#saveUndo()
         */
        @Override
        public void saveUndo() {
            undoCount++;
        }

        /**
         * @see org.apache.cayenne.project.ProjectFile#getFileName()
         */
        @Override
        public String getLocation() {
            return null;
        }


        /**
         * @see org.apache.cayenne.project.ProjectFile#getOldFileName()
         */
        @Override
        public String getOldLocation() {
            return null;
        }


        /**
         * @see org.apache.cayenne.project.ProjectFile#resolveFile()
         */
        @Override
        public File resolveFile() {
            return new File("abc");
        }


        /**
         * @see org.apache.cayenne.project.ProjectFile#resolveOldFile()
         */
        @Override
        public File resolveOldFile() {
            return new File("xyz");
        }


    }
}
