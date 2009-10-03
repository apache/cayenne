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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.conf.ConfigStatus;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.project.validator.Validator;

/**
 * Describes a model of Cayenne project. Project is a set of files in the filesystem
 * describing storing Cayenne DataMaps, DataNodes and other information.
 * <p>
 * Project has a project directory, which is a canonical directory. All project files are
 * relative to the project directory.
 * </p>
 * 
 */
public abstract class Project {

    public static final String CURRENT_PROJECT_VERSION = "3.0.0.1";
    static final int UPGRADE_STATUS_OLD = -1;
    static final int UPGRADE_STATUS_CURRENT = 0;
    static final int UPGRADE_STATUS_NEW = 1;

    protected File projectDir;
    protected List<ProjectFile> files = new ArrayList<ProjectFile>();
    protected int upgradeStatus;
    protected List<String> upgradeMessages;
    protected boolean modified;

    /**
     * Factory method to create the right project type given project file.
     */
    public static Project createProject(File projectFile) {
        String fileName = projectFile.getName();

        if (fileName.endsWith(Configuration.DEFAULT_DOMAIN_FILE)) {
            return new ApplicationProject(projectFile);
        }
        else if (fileName.endsWith(DataMapFile.LOCATION_SUFFIX)) {
            return new DataMapProject(projectFile);
        }
        else {
            throw new ProjectException("Unsupported project file: " + projectFile);
        }
    }

    /**
     * @since 1.2
     */
    protected Project() {

    }

    /**
     * Constructor for Project. <code>projectFile</code> must denote a file (existent or
     * non-existent) in an existing directory. If projectFile has no parent directory,
     * current directory is assumed.
     */
    public Project(File projectFile) {
        initialize(projectFile);
        postInitialize(projectFile);
    }

    /**
     * @since 1.2
     */
    protected void initialize(File projectFile) {
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
            }
            catch (IOException e) {
                throw new ProjectException("Error creating project.", e);
            }
        }
    }

    /**
     * Finished project initialization. Called from constructor. Default implementation
     * builds a file list and checks for upgrades.
     */
    protected void postInitialize(File projectFile) {
        // take a snapshot of files used by the project
        files = Collections.synchronizedList(buildFileList());
        upgradeMessages = Collections.synchronizedList(new ArrayList<String>());
        checkForUpgrades();
    }

    /**
     * Returns true if project location is not defined. For instance, when project was
     * created in memory and is not tied to a file yet.
     */
    public boolean isLocationUndefined() {
        return getMainFile() == null;
    }

    /**
     * Returns project upgrade status. "0" means project version matches the framework
     * version, "-1" means project is older than the framework, "+1" means the framework
     * is older than the project.
     * 
     * @since 2.0
     */
    public int getUpgradeStatus() {
        return upgradeStatus;
    }

    /**
     * Returns a list of upgrade messages.
     */
    public List<String> getUpgradeMessages() {
        return upgradeMessages;
    }

    /**
     * Returns true is project has renamed files. This is useful when converting from
     * older versions of the modeler projects.
     */
    public boolean hasRenamedFiles() {
        if (files == null) {
            return false;
        }

        synchronized (files) {
            for (ProjectFile file : files) {
                if (file.isRenamed()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Creates a list of project files.
     */
    public List<ProjectFile> buildFileList() {
        List<ProjectFile> projectFiles = new ArrayList<ProjectFile>();

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
     * Looks up and returns a file wrapper for a project object. Returns null if no file
     * exists.
     */
    public ProjectFile findFile(Object obj) {
        if (obj == null) {
            return null;
        }

        // to avoid full scan, a map may be a better
        // choice of collection here,
        // though normally projects have very few files...
        synchronized (files) {
            for (ProjectFile file : files) {
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
        }
        catch (IOException e) {
            // error converting path
            return null;
        }
    }

    /**
     * Returns a "symbolic" name of a file. Returns null if file is invalid. Symbolic name
     * is a string path of a file relative to the project directory. It is built in a
     * platform independent fashion.
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

        }
        catch (IOException e) {
            // error converting path
            return null;
        }
    }

    /**
     * Returns project directory. This is a directory where project file is located.
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
     * Determines whether the project needs to be upgraded. Populates internal list of
     * upgrade messages with discovered information.
     */
    public abstract void checkForUpgrades();

    /**
     * Returns an Iterator over project tree of objects.
     */
    public Iterator treeNodes() {
        return FlatProjectView.getInstance().flattenProjectTree(this).iterator();
    }

    /**
     * @since 1.1
     */
    public abstract void upgrade() throws ProjectException;

    /**
     * Saves project. All currently existing files are updated, without checking for
     * modifications. New files are created as needed, unused files are deleted.
     */
    public void save() throws ProjectException {

        // sanity check
        if (isLocationUndefined()) {
            throw new ProjectException("Project location is undefined.");
        }

        // 1. Traverse project tree to find file wrappers that require update.
        List<ProjectFile> filesToSave = new ArrayList<ProjectFile>();
        List<Object> wrappedObjects = new ArrayList<Object>();
        prepareSave(filesToSave, wrappedObjects);

        // 2. Try saving individual file wrappers
        processSave(filesToSave);

        // 3. Commit changes
        List<File> savedFiles = new ArrayList<File>();
        for (ProjectFile file : filesToSave) {
            savedFiles.add(file.saveCommit());
        }

        // 4. Take care of deleted
        processDelete(wrappedObjects, savedFiles);

        // 5. Refresh file list
        List<ProjectFile> freshList = buildFileList();
        for (ProjectFile file : freshList) {
            file.synchronizeLocation();
        }

        files = freshList;

        synchronized (upgradeMessages) {
            upgradeMessages.clear();
        }

        // update state
        setModified(false);
    }

    protected void prepareSave(List<ProjectFile> filesToSave, List<Object> wrappedObjects)
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
            }
            else if (existingFile.canHandleObject()) {
                wrappedObjects.add(existingFile.getObject());
                filesToSave.add(existingFile);
            }
        }

    }

    /**
     * Saves a list of modified files to temporary files.
     */
    protected void processSave(List<ProjectFile> modifiedFiles) throws ProjectException {
        // notify that files will be saved
        for (ProjectFile file : modifiedFiles) {
            file.willSave();
        }

        try {
            for (ProjectFile file : modifiedFiles) {
                file.saveTemp();
            }
        }
        catch (Exception ex) {
            for (ProjectFile file : modifiedFiles) {
                file.saveUndo();
            }

            throw new ProjectException("Project save failed and was cancelled.", ex);
        }
    }

    protected void processDelete(List<Object> existingObjects, List<File> savedFiles) {

        // check for deleted
        synchronized (files) {
            for (ProjectFile f : files) {
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
                }
                else if (f.getObject() == null) {
                    delete = true;
                }
                else if (!existingObjects.contains(f.getObject())) {
                    delete = true;
                }
                else if (!f.canHandleObject()) {
                    // this happens too - node can start using JNDI for instance
                    delete = true;
                }

                if (delete) {
                    deleteFile(file);
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
