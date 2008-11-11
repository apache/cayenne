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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.apache.cayenne.util.Util;

/**
 * ProjectFile is an adapter from an object in Cayenne project
 * to its representation in the file system.
 * 
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
        
        // must encode as UTF-8 - a default used by all Cayenne XML files
        FileOutputStream fout = new FileOutputStream(tempFile);
        OutputStreamWriter fw = new OutputStreamWriter(fout, "UTF-8");

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

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("ProjectFile [").append(getClass().getName()).append("]: name = ");
        if (getObject() != null) {
            buf.append("*null*");
        } else {
            buf.append(getObjectName());
        }

        return buf.toString();
    }
}
