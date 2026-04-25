/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.overwrite.OverwriteDialog;
import org.apache.cayenne.modeler.util.FileFilters;
import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;


/**
 * File chooser panel used to select a directory to store project files.
 *
 */
class ProjectOpener extends JFileChooser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectOpener.class);

    /**
     * Selects a directory to store the project.
     */
    public File newProjectDir(Application application, Project p) {
        if (p != null) {
            StringBuilder nameProject = new StringBuilder("cayenne");
            if (((DataChannelDescriptor) p.getRootNode()).getName() != null) {
                nameProject.append("-").append(((DataChannelDescriptor) p.getRootNode()).getName());
            }
            nameProject.append(".xml");
            // configure for application project
            return newProjectDir(nameProject.toString(), FileFilters.getApplicationFilter(), application);
        } else {
            throw new CayenneRuntimeException("Null project.");
        }
    }

    private File newProjectDir(String location, FileFilter filter, Application application) {
        // configure dialog
        setDialogTitle("Select Project Directory");
        setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        setCurrentDirectory(getDefaultStartDir(application));

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
            int status = showDialog(application.getFrameController().getView(), "Select");
            selectedDir = getSelectedFile();
            if (status != JFileChooser.APPROVE_OPTION || selectedDir == null) {
                LOGGER.info("Save canceled.");
                return null;
            }

            // normalize selection
            LOGGER.info("Selected: " + selectedDir);
            if (!selectedDir.isDirectory()) {
                selectedDir = getSelectedFile().getParentFile();
            }

            // check for overwrite
            File projectFile = new File(selectedDir, location);
            if (projectFile.exists()) {
                OverwriteDialog dialog = new OverwriteDialog(projectFile, application.getFrameController().getView());
                dialog.show();

                if (dialog.shouldOverwrite()) {
                    break;
                } else if (!dialog.shouldSelectAnother()) {
                    // canceled
                    return null;
                }
            } else {
                break;
            }
        }

        return selectedDir;
    }

    /**
     * Runs a dialog to open Cayenne project.
     */
    public File openProjectFile(Application application) {

        // configure dialog
        setDialogTitle("Select Project File");
        setFileSelectionMode(JFileChooser.FILES_ONLY);
        setCurrentDirectory(getDefaultStartDir(application));

        // configure filters
        resetChoosableFileFilters();
        addChoosableFileFilter(FileFilters.getApplicationFilter());

        // default to App projects
        setFileFilter(FileFilters.getApplicationFilter());

        int status = showOpenDialog(application.getFrameController().getView());
        if (status != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        return getSelectedFile();
    }

    private File getDefaultStartDir(Application application) {
        File existingDir = application
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
