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
import java.net.URL;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerController;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectLoader;
import org.apache.cayenne.project.upgrade.ProjectUpgrader;
import org.apache.cayenne.project.upgrade.UpgradeHandler;
import org.apache.cayenne.project.upgrade.UpgradeMetaData;
import org.apache.cayenne.project.upgrade.UpgradeType;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.swing.control.FileMenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenProjectAction extends ProjectAction {

    private static Logger logObj = LoggerFactory.getLogger(OpenProjectAction.class);

    private ProjectOpener fileChooser;

    public static String getActionName() {
        return "Open Project";
    }

    public OpenProjectAction(Application application) {
        super(getActionName(), application);
        this.fileChooser = new ProjectOpener();
    }

    @Override
    public String getIconName() {
        return "icon-open.png";
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    @Override
    public void performAction(ActionEvent e) {

        // Save and close (if needed) currently open project.
        if (getProjectController() != null && !checkSaveOnClose()) {
            return;
        }

        File f = null;
        if (e.getSource() instanceof FileMenuItem) {
            FileMenuItem menu = (FileMenuItem) e.getSource();
            f = menu.getFile();
        } else if (e.getSource() instanceof File) {
            f = (File) e.getSource();
        }

        if (f == null) {
            try {
                // Get the project file name (always cayenne.xml)
                f = fileChooser.openProjectFile(Application.getFrame());
            } catch (Exception ex) {
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
                JOptionPane.showMessageDialog(
                        Application.getFrame(),
                        "Can't open project - file \"" + file.getPath() + "\" does not exist",
                        "Can't Open Project",
                        JOptionPane.OK_OPTION);
                return;
            }

            CayenneModelerController controller = Application.getInstance().getFrameController();
            controller.addToLastProjListAction(file);

            URL url = file.toURI().toURL();
            Resource rootSource = new URLResource(url);

            ProjectUpgrader upgrader = getApplication().getInjector().getInstance(ProjectUpgrader.class);
            UpgradeHandler handler = upgrader.getUpgradeHandler(rootSource);
            UpgradeMetaData md = handler.getUpgradeMetaData();

            if (UpgradeType.DOWNGRADE_NEEDED == md.getUpgradeType()) {
                JOptionPane
                        .showMessageDialog(
                                Application.getFrame(),
                                "Can't open project - it was created using a newer version of the Modeler",
                                "Can't Open Project",
                                JOptionPane.OK_OPTION);
                closeProject(false);
            } else if (UpgradeType.INTERMEDIATE_UPGRADE_NEEDED == md.getUpgradeType()) {
                JOptionPane
                        .showMessageDialog(Application.getFrame(),
                        // TODO: andrus 05/02/2010 - this message shows intermediate
                                // version of the project XML, not the Modeler code
                                // version that
                                // can be used for upgrade
                                "Can't upgrade project. Open the project in the Modeler v."
                                        + md.getIntermediateUpgradeVersion()
                                        + " to do an intermediate upgrade before you can upgrade to v."
                                        + md.getSupportedVersion(),
                                "Can't Upgrade Project",
                                JOptionPane.OK_OPTION);
                closeProject(false);
            } else if (UpgradeType.UPGRADE_NEEDED == md.getUpgradeType()) {
                if (processUpgrades()) {
                    // perform upgrade
                    logObj.info("Will upgrade project " + url.getPath());
                    Resource upgraded = handler.performUpgrade();
                    if (upgraded != null) {
                        Project project = openProjectResourse(upgraded, controller);

                        getProjectController().getFileChangeTracker().pauseWatching();
                        getProjectController().getFileChangeTracker().reconfigure();

                        // need to update project file name if it has changed
                        if (!file.getAbsolutePath().equals(project.getConfigurationResource().getURL().getPath())) {
                            File projectFile = new File(project.getConfigurationResource().getURL().toURI());
                            controller.changePathInLastProjListAction(file, projectFile);
                        }
                    } else {
                        closeProject(false);
                    }
                }
            } else {
                openProjectResourse(rootSource, controller);
            }
        } catch (Exception ex) {
            logObj.warn("Error loading project file.", ex);
            ErrorDebugDialog.guiWarning(ex, "Error loading project");
        }
    }

    private Project openProjectResourse(Resource resource, CayenneModelerController controller) {
        Project project = getApplication().getInjector().getInstance(ProjectLoader.class).loadProject(resource);
        controller.projectOpenedAction(project);
        return project;
    }

    private boolean processUpgrades() {
        // need an upgrade
        int returnCode = JOptionPane.showConfirmDialog(
                Application.getFrame(),
                "Project needs an upgrade to a newer version. Upgrade?",
                "Upgrade Needed",
                JOptionPane.YES_NO_OPTION);
        return returnCode != JOptionPane.NO_OPTION;
    }
}
