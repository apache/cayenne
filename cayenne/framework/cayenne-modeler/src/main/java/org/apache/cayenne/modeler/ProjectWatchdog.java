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
package org.apache.cayenne.modeler;

import java.io.File;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.action.SaveAction;
import org.apache.cayenne.modeler.dialog.FileDeletedDialog;
import org.apache.cayenne.modeler.util.FileWatchdog;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectFile;

/**
 * ProjectWatchdog class is responsible for tracking changes in cayenne.xml and other
 * Cayenne project files
 * 
 */
public class ProjectWatchdog extends FileWatchdog {

    /**
     * A project to watch
     */
    protected ProjectController mediator;

    /**
     * Creates new watchdog for a specified project
     */
    public ProjectWatchdog(ProjectController mediator) {
        setName("cayenne-project-watchdog");
        this.mediator = mediator;
        setSingleNotification(true); // one message is more than enough
    }

    /**
     * Reloads files to watch from the project. Useful when project's structure has
     * changed
     */
    public void reconfigure() {
        pauseWatching();

        removeAllFiles();

        Project project = mediator.getProject();
        if (project != null // project opened
                && project.getProjectDirectory() != null) // not new project
        {
            String projectPath = project.getProjectDirectory().getPath() + File.separator;

            List<ProjectFile> files = project.buildFileList();
            for (ProjectFile pr : files)
                addFile(projectPath + pr.getLocation());
        }

        resumeWatching();
    }

    @Override
    protected void doOnChange(FileInfo fileInfo) {
        if (showConfirmation("One or more project files were changed by external program. "
                + "Do you want to load the changes?")) {
            /**
             * Currently we are reloading all project
             */
            if (mediator.getProject() != null) {
                ((OpenProjectAction) Application.getInstance().getAction(
                        OpenProjectAction.getActionName())).openProject(mediator
                        .getProject()
                        .getMainFile());
            }

        }
        else
            mediator.setDirty(true);
    }

    @Override
    protected void doOnRemove(FileInfo fileInfo) {
        if (mediator.getProject() != null
                /*&& fileInfo.getFile().equals(mediator.getProject().getMainFile()) */ ) {
            FileDeletedDialog dialog = new FileDeletedDialog(Application.getFrame());
            dialog.show();

            if (dialog.shouldSave()) {
                Application
                        .getInstance()
                        .getAction(SaveAction.getActionName())
                        .performAction(null);
            }
            else if (dialog.shouldClose()) {
                CayenneModelerController controller = Application
                        .getInstance()
                        .getFrameController();

                controller.projectClosedAction();
            }
            else
                mediator.setDirty(true);
        }
    }

    /**
     * Shows confirmation dialog
     */
    private boolean showConfirmation(String message) {
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                Application.getFrame(),
                message,
                "File changed",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
    }
}
