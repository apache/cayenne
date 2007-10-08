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
package org.objectstyle.cayenne.project;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.project.validator.Validator;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ProjectTst extends CayenneTestCase {

    protected Project p;
    protected File f;

    /**
      * @see junit.framework.TestCase#setUp()
      */
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
         * @see org.objectstyle.cayenne.project.ProjectFile#canHandle(Object)
         */
        public boolean canHandle(Object obj) {
            return false;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#createProjectFile(Object)
         */
        public ProjectFile createProjectFile(Project project, Object obj) {
            return null;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#getObject()
         */
        public Object getObject() {
            return null;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#getObjectName()
         */
        public String getObjectName() {
            return null;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#saveToFile(File)
         */
        public void save(PrintWriter out) throws Exception {
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#saveCommit()
         */
        public File saveCommit() {
            commitCount++;
            return new File("abc");
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#saveDelete()
         */
        public boolean saveDelete() {
            deleteCount++;
            return !shouldFail;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#saveTemp()
         */
        public void saveTemp() throws Exception {
            saveTempCount++;
            
            if(shouldFail) {
            	throw new Exception("You forced me to fail...");
            }
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#saveUndo()
         */
        public void saveUndo() {
            undoCount++;
        }

        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#getFileName()
         */
        public String getLocation() {
            return null;
        }


        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#getOldFileName()
         */
        public String getOldLocation() {
            return null;
        }


        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#resolveFile()
         */
        public File resolveFile() {
            return new File("abc");
        }


        /**
         * @see org.objectstyle.cayenne.project.ProjectFile#resolveOldFile()
         */
        public File resolveOldFile() {
            return new File("xyz");
        }


    }
}
