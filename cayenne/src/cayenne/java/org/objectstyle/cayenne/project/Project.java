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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.conf.ConfigStatus;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.project.validator.Validator;

/**
 * Describes a model of Cayenne project. Project is a set of 
 * files in the filesystem describing storing Cayenne DataMaps,
 * DataNodes and other information.
 * 
 * <p>Project has a project directory, which is a canonical directory. 
 * All project files are relative to the project directory.
 * </p>
 * 
 * @author Andrei Adamchik
 */
public abstract class Project {
    private static final Logger logObj = Logger.getLogger(Project.class);

    public static final String CURRENT_PROJECT_VERSION = "1.1";

    protected File projectDir;
    protected List files = new ArrayList();
    protected List upgradeMessages;
    protected boolean modified;

    /**
     * Factory method to create the right project type given project file.
     */
    public static Project createProject(File projectFile) {
		logObj.debug("createProject: " + projectFile);
        String fileName = projectFile.getName();

        if (fileName.endsWith(Configuration.DEFAULT_DOMAIN_FILE)) {
            return new ApplicationProject(projectFile);
        } else if (fileName.endsWith(DataMapFile.LOCATION_SUFFIX)) {
            return new DataMapProject(projectFile);
        } else {
            throw new ProjectException(
                "Unsupported project file: " + projectFile);
        }
    }

    /**
     * Constructor for Project. <code>projectFile</code> must denote 
     * a file (existent or non-existent) in an existing directory. 
     * If projectFile has no parent directory, current directory is assumed.
     */
    public Project(File projectFile) {

        if (projectFile != null) {
            File parent = projectFile.getParentFile();
            if (parent == null) {
                parent = new File(System.getProperty("user.dir"));
            }

            if (!parent.isDirectory()) {
                throw new ProjectException(
                    "Project directory does not exist or is not a directory: "
                        + parent);
            }

            try {
                projectDir = parent.getCanonicalFile();
            } catch (IOException e) {
                throw new ProjectException("Error creating project.", e);
            }
        }

        postInitialize(projectFile);
    }

    /** 
     * Finished project initialization. Called
     * from constructor. Default implementation builds a file list
     * and checks for upgrades.
     */
    protected void postInitialize(File projectFile) {
		logObj.debug("postInitialize with: " + projectFile);
        // take a snapshot of files used by the project
        files = Collections.synchronizedList(buildFileList());
        upgradeMessages = Collections.synchronizedList(new ArrayList());
        checkForUpgrades();
    }

    /**
     * Returns true if project location is not defined. For instance,
     * when project was created in memory and is not tied to a file yet.
     */
    public boolean isLocationUndefined() {
        return getMainFile() == null;
    }

    /**
     * Returns true if the project needs to be upgraded.
     */
    public boolean isUpgradeNeeded() {
        return upgradeMessages.size() > 0;
    }

    /**
      * Returns a list of upgrade messages.
      */
    public List getUpgradeMessages() {
        return upgradeMessages;
    }

    /**
     * Returns true is project has renamed files.
     * This is useful when converting from older versions
     * of the modeler projects.
     */
    public boolean hasRenamedFiles() {
        if (files == null) {
            return false;
        }

        synchronized (files) {
            Iterator it = files.iterator();
            while (it.hasNext()) {
                if (((ProjectFile) it.next()).isRenamed()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Creates a list of project files.
     */
    public List buildFileList() {
        List projectFiles = new ArrayList();

        Iterator nodes = treeNodes();
        while (nodes.hasNext()) {
            ProjectPath nodePath = (ProjectPath) nodes.next();
            Object obj = nodePath.getObject();
            ProjectFile f = projectFileForObject(obj);

            if (f != null) {
                projectFiles.add(f);
            }
        }

        return projectFiles;
    }

    /**
     * Creates an instance of Validator for validating this project.
     */
    public Validator getValidator() {
        return new Validator(this);
    }

    /**
     * Looks up and returns a file wrapper for a project
     * object. Returns null if no file exists.
     */
    public ProjectFile findFile(Object obj) {
        if (obj == null) {
            return null;
        }

        // to avoid full scan, a map may be a better 
        // choice of collection here, 
        // though normally projects have very few files...
        synchronized (files) {
            Iterator it = files.iterator();
            while (it.hasNext()) {
                ProjectFile file = (ProjectFile) it.next();
                if (file.getObject() == obj) {
                    return file;
                }
            }
        }

        return null;
    }

    /**
     * Returns a canonical file built from symbolic name.
     */
    public File resolveFile(String symbolicName) {
        try {
            // substitute to Windows backslashes if needed
            if (File.separatorChar != '/') {
                symbolicName = symbolicName.replace('/', File.separatorChar);
            }
            return new File(projectDir, symbolicName).getCanonicalFile();
        } catch (IOException e) {
            // error converting path
            logObj.info("Can't convert to canonical form.", e);
            return null;
        }
    }

    /**
      * Returns a "symbolic" name of a file. Returns null if file 
      * is invalid. Symbolic name is a string path of a file relative
      * to the project directory. It is built in a platform independent
      * fashion.
      */
    public String resolveSymbolicName(File file) {
        String symbolicName = null;
        try {
            // accept absolute files only when 
            // they are in the project directory
            String otherPath = file.getCanonicalFile().getPath();
            String thisPath = projectDir.getPath();

            // invalid absolute pathname, can't continue
            if (otherPath.length() + 1 <= thisPath.length()
                || !otherPath.startsWith(thisPath)) {
                return null;
            }

            symbolicName = otherPath.substring(thisPath.length() + 1);

            // substitute Windows backslashes if needed
            if ((symbolicName != null) && (File.separatorChar != '/')) {
                symbolicName = symbolicName.replace(File.separatorChar, '/');
            }

            return symbolicName;

        } catch (IOException e) {
            // error converting path
            logObj.info("Can't convert to canonical form.", e);
            return null;
        }
    }

    /** 
     * Returns project directory. This is a directory where
     * project file is located.
     */
    public File getProjectDirectory() {
        return projectDir;
    }

    public void setProjectDirectory(File dir) {
        this.projectDir = dir;
    }

    /**
     * Returns a canonical form of a main file associated with this project.
     */
    public File getMainFile() {
        if (projectDir == null) {
            return null;
        }

        ProjectFile f = projectFileForObject(this);
        return (f != null) ? resolveFile(f.getLocation()) : null;
    }

    /**
     * @return An object describing failures in the loaded project.
     */
    public abstract ConfigStatus getLoadStatus();

    public abstract ProjectFile projectFileForObject(Object obj);

    /**
     * Returns a list of first-level children of the project.
     */
    public abstract List getChildren();

    /**
     * Determines whether the project needs to be upgraded.
     * Populates internal list of upgrade messages with discovered
     * information.
     */
    public abstract void checkForUpgrades();

    /**
     * Returns an Iterator over project tree of objects.
     */
    public Iterator treeNodes() {
        return FlatProjectView
            .getInstance()
            .flattenProjectTree(this)
            .iterator();
    }
    
    /**
     * @since 1.1
     */
    public abstract void upgrade() throws ProjectException;

    /** 
     * Saves project. All currently existing files are updated,
     * without checking for modifications. New files are created
     * as needed, unused files are deleted.
     */
    public void save() throws ProjectException {

        // sanity check
        if (isLocationUndefined()) {
            throw new ProjectException("Project location is undefined.");
        }

        // 1. Traverse project tree to find file wrappers that require update.
        List filesToSave = new ArrayList();
        List wrappedObjects = new ArrayList();
        prepareSave(filesToSave, wrappedObjects);

        // 2. Try saving individual file wrappers
        processSave(filesToSave);

        // 3. Commit changes
        List savedFiles = new ArrayList();
        Iterator saved = filesToSave.iterator();
        while (saved.hasNext()) {
            ProjectFile f = (ProjectFile) saved.next();
            savedFiles.add(f.saveCommit());
        }

        // 4. Take care of deleted
        processDelete(wrappedObjects, savedFiles);

        // 5. Refresh file list
        List freshList = buildFileList();
        Iterator it = freshList.iterator();
        while (it.hasNext()) {
            ((ProjectFile) it.next()).synchronizeLocation();
        }

        files = freshList;

        synchronized (upgradeMessages) {
            upgradeMessages.clear();
        }

        // update state 
        setModified(false);
    }

    protected void prepareSave(List filesToSave, List wrappedObjects)
        throws ProjectException {
        Iterator nodes = treeNodes();
        while (nodes.hasNext()) {
            ProjectPath nodePath = (ProjectPath) nodes.next();
            Object obj = nodePath.getObject();

            ProjectFile existingFile = findFile(obj);

            if (existingFile == null) {
                // check if project node can have a file
                ProjectFile newFile = projectFileForObject(obj);
                if (newFile != null) {
                    filesToSave.add(newFile);
                }
            } else if (existingFile.canHandleObject()) {
                wrappedObjects.add(existingFile.getObject());
                filesToSave.add(existingFile);
            }
        }

    }

    /**
     * Saves a list of modified files to temporary files.
     */
    protected void processSave(List modifiedFiles) throws ProjectException {
        // notify that files will be saved
        Iterator willSave = modifiedFiles.iterator();
        while (willSave.hasNext()) {
            ProjectFile f = (ProjectFile) willSave.next();
            f.willSave();
        }

        try {
            Iterator modified = modifiedFiles.iterator();
            while (modified.hasNext()) {
                ProjectFile f = (ProjectFile) modified.next();

                if (logObj.isDebugEnabled()) {
                    logObj.info("Saving file " + f.resolveFile());
                }

                f.saveTemp();
            }
        } catch (Exception ex) {
            logObj.info("*** Project save failed, reverting.", ex);

            // revert
            Iterator modified = modifiedFiles.iterator();
            while (modified.hasNext()) {
                ProjectFile f = (ProjectFile) modified.next();
                f.saveUndo();
            }

            throw new ProjectException(
                "Project save failed and was canceled.",
                ex);
        }
    }

    protected void processDelete(List existingObjects, List savedFiles) {

        // check for deleted
        synchronized (files) {
            Iterator oldFiles = files.iterator();
            while (oldFiles.hasNext()) {
                ProjectFile f = (ProjectFile) oldFiles.next();
                File file = f.resolveOldFile();

                // this check is needed, since a file can reuse the name
                // of a recently deleted file, and we don't want to delete 
                // new file by mistake
                if (file == null || savedFiles.contains(file)) {
                    continue;
                }

                boolean delete = false;
                if (f.isRenamed()) {
                    delete = true;
                    logObj.info("File renamed, deleting old version: " + file);
                } else if (f.getObject() == null) {
                    delete = true;
                    logObj.info("Null internal object, deleting file: " + file);
                } else if (!existingObjects.contains(f.getObject())) {
                    delete = true;
                    logObj.info(
                        "Object deleted from the project, deleting file: "
                            + file);
                } else if (!f.canHandleObject()) {
                    // this happens too - node can start using JNDI for instance
                    delete = true;
                    logObj.info(
                        "Can no longer handle the object, deleting file: "
                            + file);
                }

                if (delete) {
                    if (!deleteFile(file)) {
                        logObj.info("*** Failed to delete file, ignoring.");
                    }
                }
            }
        }
    }

    protected boolean deleteFile(File f) {
        return (f.exists()) ? f.delete() : true;
    }

    /**
     * Returns <code>true</code> if the project is modified.
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Updates "modified" state of the project.
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }
}
