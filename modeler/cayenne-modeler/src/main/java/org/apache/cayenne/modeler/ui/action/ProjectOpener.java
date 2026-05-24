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
import org.apache.cayenne.modeler.pref.adapters.RecentProjectsPrefs;
import org.apache.cayenne.modeler.ui.project.overwrite.OverwriteDialog;
import org.apache.cayenne.modeler.toolkit.filechooser.FileChooserFactory;
import org.apache.cayenne.modeler.toolkit.filechooser.FileFilters;
import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;


/**
 * Helper that shows file-chooser dialogs for opening and saving Cayenne projects.
 */
class ProjectOpener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectOpener.class);

    /**
     * Selects a directory to store the project.
     */
    public File newProjectDir(Application application, Project p) {
        if (p == null) {
            throw new CayenneRuntimeException("Null project.");
        }

        StringBuilder nameProject = new StringBuilder("cayenne");
        if (((DataChannelDescriptor) p.getRootNode()).getName() != null) {
            nameProject.append("-").append(((DataChannelDescriptor) p.getRootNode()).getName());
        }
        nameProject.append(".xml");

        FileChooserFactory factory = application.getFileChooserFactory();
        File startDir = getDefaultStartDir(application);

        while (true) {
            File selectedDir = factory.saveDir(application.getFrame(), "Select Project Directory", startDir);
            if (selectedDir == null) {
                LOGGER.info("Save canceled.");
                return null;
            }

            LOGGER.info("Selected: {}", selectedDir);
            File projectFile = new File(selectedDir, nameProject.toString());
            if (projectFile.exists()) {
                OverwriteDialog dialog = new OverwriteDialog(projectFile, application.getFrame());
                dialog.show();
                if (dialog.shouldOverwrite()) {
                    return selectedDir;
                } else if (!dialog.shouldSelectAnother()) {
                    return null;
                }
                // else loop again
            } else {
                return selectedDir;
            }
        }
    }

    /**
     * Runs a dialog to open a Cayenne project.
     */
    public File openProjectFile(Application application) {
        return application.getFileChooserFactory().openFile(
                application.getFrame(),
                "Select Project File",
                getDefaultStartDir(application),
                FileFilters.getApplicationFilter());
    }

    private File getDefaultStartDir(Application application) {
        List<File> recent = new RecentProjectsPrefs(application.getPrefsLocator().appNode(RecentProjectsPrefs.NODE)).getFiles();
        if (!recent.isEmpty()) {
            File parent = recent.get(0).getParentFile();
            if (parent != null && parent.isDirectory()) {
                return parent;
            }
        }
        return new File(System.getProperty("user.dir"));
    }
}
