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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.project.ApplicationProject;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectException;
import org.apache.cayenne.swing.control.FileMenuItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 */
public class OpenProjectAction extends ProjectAction {

    private static Log logObj = LogFactory.getLog(OpenProjectAction.class);

    protected ProjectOpener fileChooser;

    public static String getActionName() {
        return "Open Project";
    }

    /**
     * Constructor for OpenProjectAction.
     */
    public OpenProjectAction(Application application) {
        super(getActionName(), application);
        this.fileChooser = new ProjectOpener();
    }

    public String getIconName() {
        return "icon-open.gif";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit
                .getDefaultToolkit()
                .getMenuShortcutKeyMask());
    }

    public void performAction(ActionEvent e) {

        // Save and close (if needed) currently open project.
        if (getProjectController() != null && !checkSaveOnClose()) {
            return;
        }
       
        File f = null;
        if (e.getSource() instanceof FileMenuItem) {
            FileMenuItem menu = (FileMenuItem) e.getSource();
            f = menu.getFile();
        }
        else if(e.getSource() instanceof File) {
            f = (File) e.getSource();
        }

        if (f == null) {
            try {
                // Get the project file name (always cayenne.xml)
                f = fileChooser.openProjectFile(Application.getFrame());
            }
            catch (Exception ex) {
                logObj.warn("Error loading project file.", ex);
            }
        }

        if (f != null) {
            // by now if the project is unsaved, this has been a user choice...
            if (getProjectController() != null && !closeProject(false)) {
                return;
            }

            openProject(f);
        }
        
        application.getUndoManager().discardAllEdits();
    }

    /** Opens specified project file. File must already exist. */
    public void openProject(File file) {

        try {
            if (!file.exists()) {
                JOptionPane.showMessageDialog(Application.getFrame(),
                        "Can't open project - file \"" + file.getPath() + "\" does not exist",
                        "Can't Open Project", JOptionPane.OK_OPTION);
                return;
            }
            
            getApplication().getFrameController().addToLastProjListAction(
                    file.getAbsolutePath());

            Configuration config = buildProjectConfiguration(file);
            Project project = new ApplicationProject(file, config);
            getProjectController().setProject(project);

            // if upgrade was canceled
            int upgradeStatus = project.getUpgradeStatus();
            if (upgradeStatus > 0) {
                JOptionPane
                        .showMessageDialog(
                                Application.getFrame(),
                                "Can't open project - it was created using a newer version of the Modeler",
                                "Can't Open Project",
                                JOptionPane.OK_OPTION);
                closeProject(false);
            }
            else if (upgradeStatus < 0) {
                if (processUpgrades(project)) {
                    getApplication().getFrameController().projectOpenedAction(project);
                }
                else {
                    closeProject(false);
                }
            }
            else {
                getApplication().getFrameController().projectOpenedAction(project);
            }
        }
        catch (Exception ex) {
            logObj.warn("Error loading project file.", ex);
            ErrorDebugDialog.guiWarning(ex, "Error loading project");
        }
    }

    protected boolean processUpgrades(Project project) throws ProjectException {
        // must really concat all messages, this is a temp hack...
        String msg = project.getUpgradeMessages().get(0);
        // need an upgrade
        int returnCode = JOptionPane.showConfirmDialog(
                Application.getFrame(),
                "Project needs an upgrade to a newer version. " + msg + ". Upgrade?",
                "Upgrade Needed",
                JOptionPane.YES_NO_OPTION);
        if (returnCode == JOptionPane.NO_OPTION) {
            return false;
        }

        // perform upgrade
        logObj.info("Will upgrade project " + project.getMainFile());
        getProjectController().getProjectWatcher().pauseWatching();
        project.upgrade();
        getProjectController().getProjectWatcher().reconfigure();
        return true;
    }
}
