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

package org.apache.cayenne.modeler.action;

import java.awt.Frame;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.OverwriteDialog;
import org.apache.cayenne.modeler.util.FileFilters;
import org.apache.cayenne.project.ApplicationProject;
import org.apache.cayenne.project.DataMapProject;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * File chooser panel used to select a directory to store project files.
 * 
 */
class ProjectOpener extends JFileChooser {

    private static Log logObj = LogFactory.getLog(ProjectOpener.class);

    /**
     * Selects a directory to store the project.
     */
    File newProjectDir(Frame f, Project p) {
        if (p instanceof ApplicationProject) {
            // configure for application project
            return newProjectDir(f, Configuration.DEFAULT_DOMAIN_FILE, FileFilters
                    .getApplicationFilter());
        }
        else if (p instanceof DataMapProject) {
            // configure for DataMap project
            ProjectFile projFileWrapper = p.projectFileForObject(p);
            return newProjectDir(f, projFileWrapper.getLocation(), FileFilters
                    .getDataMapFilter());
        }
        else {
            String message = (p == null)
                    ? "Null project."
                    : "Unrecognized project class: " + p.getClass().getName();
            throw new CayenneRuntimeException(message);
        }
    }

    File newProjectDir(Frame f, String location, FileFilter filter) {
        // configure dialog
        setDialogTitle("Select Project Directory");
        setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        setCurrentDirectory(getDefaultStartDir());

        // preselect current directory
        if (getCurrentDirectory() != null) {
            setSelectedFile(getCurrentDirectory());
        }

        // configure filters
        resetChoosableFileFilters();
        // allow users to see directories with cayenne.xml files
        addChoosableFileFilter(filter);

        File selectedDir;

        while (true) {
            int status = showDialog(f, "Select");
            selectedDir = getSelectedFile();
            if (status != JFileChooser.APPROVE_OPTION || selectedDir == null) {
                logObj.info("Save canceled.");
                return null;
            }

            // normalize selection
            logObj.info("Selected: " + selectedDir);
            if (!selectedDir.isDirectory()) {
                selectedDir = getSelectedFile().getParentFile();
            }

            // check for overwrite
            File projectFile = new File(selectedDir, location);
            if (projectFile.exists()) {
                OverwriteDialog dialog = new OverwriteDialog(projectFile, f);
                dialog.show();

                if (dialog.shouldOverwrite()) {
                    break;
                }
                else if (dialog.shouldSelectAnother()) {
                    continue;
                }
                else {
                    // canceled
                    return null;
                }
            }
            else {
                break;
            }
        }

        return selectedDir;
    }

    /**
     * Runs a dialog to open Cayenne project.
     */
    File openProjectFile(Frame f) {

        // configure dialog
        setDialogTitle("Select Project File");
        setFileSelectionMode(JFileChooser.FILES_ONLY);
        setCurrentDirectory(getDefaultStartDir());

        // configure filters
        resetChoosableFileFilters();
        addChoosableFileFilter(FileFilters.getApplicationFilter());
        addChoosableFileFilter(FileFilters.getDataMapFilter());

        // default to App projects
        setFileFilter(FileFilters.getApplicationFilter());

        int status = showOpenDialog(f);
        if (status != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        return getSelectedFile();
    }

    /**
     * Returns directory where file search should start. This is either coming from saved
     * preferences, or a current directory is used.
     */
    File getDefaultStartDir() {
        // find start directory in preferences
        File existingDir = Application
                .getInstance()
                .getFrameController()
                .getLastDirectory()
                .getExistingDirectory(false);

        if (existingDir == null) {
            // go to current dir...
            existingDir = new File(System.getProperty("user.dir"));
        }

        return existingDir;
    }
}
