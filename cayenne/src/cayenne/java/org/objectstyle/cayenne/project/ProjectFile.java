/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.objectstyle.cayenne.util.Util;

/**
 * ProjectFile is an adapter from an object in Cayenne project
 * to its representation in the file system.
 * 
 * @author Andrei Adamchik
 */
public abstract class ProjectFile {

    protected String location;
    protected File tempFile;
    protected Project projectObj;

    public ProjectFile() {}

    /**
     * Constructor for ProjectFile.
     */
    public ProjectFile(Project project, String location) {
        this.location = location;
        this.projectObj = project;
    }

    /**
     * Builds a filename from the object name and "file suffix".
     */
    public String getLocation() {
        String oName = getObjectName();
        if (oName == null) {
            throw new NullPointerException("Null name.");
        }

        return oName + getLocationSuffix();
    }

    /**
    * Returns saved location of a file.
    */
    public String getOldLocation() {
        return location;
    }

    /**
     * Returns suffix to append to object name when 
     * creating a file name. Default implementation 
     * returns empty string.
     */
    public String getLocationSuffix() {
        return "";
    }

    /**
     * Returns a project object associated with this file.
     */
    public abstract Object getObject();

    /**
     * Returns a name of associated object, that is also 
     * used as a file name.
     */
    public abstract String getObjectName();

    /**
     * Saves an underlying object to the file. 
     * The procedure is dependent on the type of
     * object and is implemented by concrete subclasses.
     */
    public abstract void save(PrintWriter out) throws Exception;

    /**
     * Returns true if this file wrapper can handle a
     * specified object.
     */
    public abstract boolean canHandle(Object obj);
    
   /**
     * Returns true if this file wrapper can handle an
     * internally stored object.
     */
    public boolean canHandleObject() {
    	return canHandle(getObject());
    }

    /**
     * Replaces internally stored filename with the current object name.
     */
    public void synchronizeLocation() {
        location = getLocation();
    }

    /**
     * This method is called by project to let file know that
     * it will be saved. Default implementation is a noop.
     */
    public void willSave() {}

    /**
     * Saves ProjectFile's underlying object to a temporary 
     * file, returning this file to the caller. If any problems are 
     * encountered during saving, an Exception is thrown.
     */
    public void saveTemp() throws Exception {
        // cleanup any previous temp files
        if (tempFile != null && tempFile.isFile()) {
            tempFile.delete();
            tempFile = null;
        }

        // check write permissions for the target final file...
        File finalFile = resolveFile();
        checkWritePermissions(finalFile);

        // ...but save to temp file first
        tempFile = tempFileForFile(finalFile);
        FileWriter fw = new FileWriter(tempFile);

        try {
            PrintWriter pw = new PrintWriter(fw);
            try {
                save(pw);
            } finally {
                pw.close();
            }
        } finally {
            fw.close();
        }
    }

    /**
     * Returns a file which is a canonical representation of the 
     * file to store a wrapped object. If an object was renamed, 
     * the <b>new</b> name is returned.
     */
    public File resolveFile() {
        return getProject().resolveFile(getLocation());
    }

    /**
     * Returns a file which is a canonical representation of the 
     * file to store a wrapped object. If an object was renamed, 
     * the <b>old</b> name is returned. Returns null if this file 
     * has never been saved before. 
     */
    public File resolveOldFile() {
    	String oldLocation = getOldLocation();
        return (oldLocation != null) ? getProject().resolveFile(oldLocation) : null;
    }

    /**
     * Finishes saving the underlying object.
     */
    public File saveCommit() throws ProjectException {
        File finalFile = resolveFile();
        
        if (tempFile != null) {
            if (finalFile.exists()) {
                if (!finalFile.delete()) {
                    throw new ProjectException(
                        "Unable to remove old master file : " + finalFile);
                }
            }

            if (!tempFile.renameTo(finalFile)) {
                throw new ProjectException(
                    "Unable to move " + tempFile + " to " + finalFile);
            }

            tempFile = null;
        }
        
        return finalFile;
    }

    /**
     * Cleans up after unsuccessful or canceled save attempt.
     */
    public void saveUndo() {
        if (tempFile != null && tempFile.isFile()) {
            tempFile.delete();
            tempFile = null;
        }
    }

    /**
      * Returns the project.
      * @return Project
      */
    public Project getProject() {
        return projectObj;
    }

    public boolean isRenamed() {
        return !Util.nullSafeEquals(location, getLocation());
    }

    /** 
     * Creates a temporary file for the master file.
     */
    protected File tempFileForFile(File f) throws IOException {
        File parent = f.getParentFile();
        String name = f.getName();

        if (name == null || name.length() < 3) {
            name = "cayenne-project";
        }
 
        if(!parent.exists()) {
        	if(!parent.mkdirs()) {
        		throw new IOException("Error creating directory tree: " + parent);
        	}
        }
         
        return File.createTempFile(name, null, parent);
    }

    protected void checkWritePermissions(File file) throws IOException {
        if (file.isDirectory()) {
            throw new IOException("Target file is a directory: " + file);
        }

        if (file.exists() && !file.canWrite()) {
            throw new IOException("Can't write to file: " + file);
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("ProjectFile [").append(getClass().getName()).append("]: name = ");
        if (getObject() != null) {
            buf.append("*null*");
        } else {
            buf.append(getObjectName());
        }

        return buf.toString();
    }
}
