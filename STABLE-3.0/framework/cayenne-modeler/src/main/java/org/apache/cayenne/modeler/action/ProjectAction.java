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

import java.awt.event.ActionEvent;
import java.io.File;

import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerController;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.UnsavedChangesDialog;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.ProjectConfiguration;
import org.apache.cayenne.project.ProjectPath;

/**
 */
public class ProjectAction extends CayenneAction {

    public static String getActionName() {
        return "Close Project";
    }

    public ProjectAction(Application application) {
        super(getActionName(), application);
    }

    /**
     * Constructor for ProjectAction.
     * 
     * @param name
     */
    public ProjectAction(String name, Application application) {
        super(name, application);
    }

    /**
     * Closes current project.
     */
    public void performAction(ActionEvent e) {
        closeProject(true);
    }

    /**
     * Creates a configuration suitable for use in the Modeler. Used mainly by subclasses.
     * 
     * @since 1.2
     */
    protected Configuration buildProjectConfiguration(File projectFile) {
        ProjectConfiguration config = new ProjectConfiguration(projectFile);
        config.setLoaderDelegate(new ModelerProjectLoadDelegate(config));
        config.setSaverDelegate(new ModelerProjectSaveDelegate(config));
        return config;
    }

    /** Returns true if successfully closed project, false otherwise. */
    public boolean closeProject(boolean checkUnsaved) {
        // check if there is a project...
        if (getProjectController() == null || getProjectController().getProject() == null) {
            return true;
        }

        if (checkUnsaved && !checkSaveOnClose()) {
            return false;
        }

        CayenneModelerController controller = Application
                .getInstance()
                .getFrameController();
        
        application.getUndoManager().discardAllEdits();
        
        controller.projectClosedAction();

        return true;
    }

    /**
     * Returns false if cancel closing the window, true otherwise.
     */
    public boolean checkSaveOnClose() {
        ProjectController projectController = getProjectController();
        if (projectController != null && projectController.isDirty()) {
            UnsavedChangesDialog dialog = new UnsavedChangesDialog(Application.getFrame());
            dialog.show();

            if (dialog.shouldCancel()) {
                // discard changes and DO NOT close
                Application.getInstance().setQuittingApplication(false);
                return false;
            }
            else if (dialog.shouldSave()) {
                // save changes and close
                ActionEvent e = new ActionEvent(
                        this,
                        ActionEvent.ACTION_PERFORMED,
                        "SaveAll");
                Application
                        .getInstance()
                        .getAction(SaveAction.getActionName())
                        .actionPerformed(e);
                if (projectController.isDirty()) {
                    // save was canceled... do not close
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Always returns true.
     */
    public boolean enableForPath(ProjectPath path) {
        return true;
    }
}
