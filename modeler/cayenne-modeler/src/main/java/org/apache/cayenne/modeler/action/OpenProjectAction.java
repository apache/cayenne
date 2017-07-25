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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerController;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectLoader;
import org.apache.cayenne.project.upgrade.UpgradeMetaData;
import org.apache.cayenne.project.upgrade.UpgradeService;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.swing.control.FileMenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenProjectAction extends ProjectAction {

    private static Logger logObj = LoggerFactory.getLogger(OpenProjectAction.class);

    private static final Map<String, String> PROJECT_TO_MODELER_VERSION;
    static {
        // Correspondence between project version and latest Modeler version that can upgrade it.
        // Modeler v4.1 can handle versions from 3.1 and 4.0 (including intermediate versions) modeler.
        Map<String, String> map = new HashMap<>();
        map.put("1.0",      "v3.0");
        map.put("1.1",      "v3.0");
        map.put("1.2",      "v3.0");
        map.put("2.0",      "v3.0");
        map.put("3.0.0.1",  "v3.1");
        PROJECT_TO_MODELER_VERSION = Collections.unmodifiableMap(map);
    }

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
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            CayenneModelerController controller = Application.getInstance().getFrameController();
            controller.addToLastProjListAction(file);

            URL url = file.toURI().toURL();
            Resource rootSource = new URLResource(url);

            UpgradeService upgradeService = getApplication().getInjector().getInstance(UpgradeService.class);
            UpgradeMetaData metaData = upgradeService.getUpgradeType(rootSource);
            switch (metaData.getUpgradeType()) {
                case INTERMEDIATE_UPGRADE_NEEDED:
                    String modelerVersion = PROJECT_TO_MODELER_VERSION.get(metaData.getProjectVersion());
                    if(modelerVersion == null) {
                        modelerVersion = "";
                    }
                    JOptionPane.showMessageDialog(Application.getFrame(),
                                    "Open the project in the older Modeler " + modelerVersion
                                            + " to do an intermediate upgrade\nbefore you can upgrade to latest version.",
                                    "Can't Upgrade Project", JOptionPane.ERROR_MESSAGE);
                    closeProject(false);
                    return;

                case DOWNGRADE_NEEDED:
                    JOptionPane.showMessageDialog(Application.getFrame(),
                                    "Can't open project - it was created using a newer version of the Modeler",
                                    "Can't Open Project", JOptionPane.ERROR_MESSAGE);
                    closeProject(false);
                    return;

                case UPGRADE_NEEDED:
                    if (processUpgrades()) {
                        rootSource = upgradeService.upgradeProject(rootSource);
                    } else {
                        closeProject(false);
                        return;
                    }
                    break;
            }

            openProjectResourse(rootSource, controller);
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
