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

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.adapters.RecentProjectsPrefs;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.toolkit.filechooser.FileFilters;
import org.apache.cayenne.modeler.ui.MainFrame;
import org.apache.cayenne.modeler.ui.errors.ErrorDialog;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.upgrade.UpgradeMetaData;
import org.apache.cayenne.project.upgrade.UpgradeService;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.Map;

public class OpenProjectAction extends AppAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenProjectAction.class);

    private static final Map<String, String> PROJECT_TO_MODELER_VERSION;

    static {
        // Correspondence between project version and latest Modeler version that can upgrade it.
        // Modeler v4.1 can handle versions from 3.1 and 4.0 (including intermediate versions) modeler.
        PROJECT_TO_MODELER_VERSION = Map.of("1.0", "v3.0", "1.1", "v3.0", "1.2", "v3.0", "2.0", "v3.0", "3.0.0.1", "v3.1");
    }

    public OpenProjectAction(Application application) {
        super(application, "Open Project");
        resetClipboard();
    }

    @Override
    public String getIconName() {
        return "icon-open.png";
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    @Override
    public void performAction(ActionEvent e) {

        // Save and close (if needed) currently open project.
        if (getProjectSession() != null && !CloseProjectAction.checkSaveOnClose(this, app)) {
            return;
        }

        File f = null;
        if (e.getSource() instanceof JMenuItem) {
            // Recent-files sub-menu items carry an absolute path as their text;
            // the top-level "Open Project" menu item carries the action label,
            // so only treat the text as a path when it's absolute.
            String text = ((JMenuItem) e.getSource()).getText();
            if (text != null) {
                File candidate = new File(text);
                if (candidate.isAbsolute()) {
                    f = candidate;
                }
            }
        } else if (e.getSource() instanceof File) {
            f = (File) e.getSource();
        }

        if (f == null) {
            try {
                f = openProjectFile();
            } catch (Exception ex) {
                LOGGER.warn("Error loading project file.", ex);
            }
        }

        if (f != null) {
            // by now if the project is unsaved, this has been a user choice...
            if (getProjectSession() != null && !CloseProjectAction.closeProject(app, false)) {
                return;
            }

            openProject(f, null);
        }

        app.getUndoManager().discardAllEdits();
    }

    /**
     * Opens the specified project file. File must already exist. When {@code mcpHandshakeNonce} is non-null, the
     * MCP-driven launch contract is honored: on successful open, a handshake entry is written to Preferences so the
     * MCP server's wait loop can confirm the project loaded. Pass {@code null} for normal user-driven opens.
     */
    public void openProject(File file, String mcpHandshakeNonce) {
        try {
            if (!file.exists()) {
                JOptionPane.showMessageDialog(
                        app.getFrame(),
                        "Can't open project - file \"" + file.getPath() + "\" does not exist",
                        "Can't Open Project",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            MainFrame controller = app.getFrame();
            controller.addToLastProjListAction(file);

            URL url = file.toURI().toURL();
            Resource rootSource = new URLResource(url);

            UpgradeService upgradeService = app.getUpgradeService();
            UpgradeMetaData metaData = upgradeService.getUpgradeType(rootSource);
            switch (metaData.getUpgradeType()) {
                case INTERMEDIATE_UPGRADE_NEEDED:
                    String modelerVersion = PROJECT_TO_MODELER_VERSION.get(metaData.getProjectVersion());
                    if (modelerVersion == null) {
                        modelerVersion = "";
                    }
                    JOptionPane.showMessageDialog(app.getFrame(),
                            "Open the project in the older Modeler " + modelerVersion
                                    + " to do an intermediate upgrade\nbefore you can upgrade to latest version.",
                            "Can't Upgrade Project", JOptionPane.ERROR_MESSAGE);
                    CloseProjectAction.closeProject(app, false);
                    return;

                case DOWNGRADE_NEEDED:
                    JOptionPane.showMessageDialog(app.getFrame(),
                            "Can't open project - it was created using a newer version of the Modeler",
                            "Can't Open Project", JOptionPane.ERROR_MESSAGE);
                    CloseProjectAction.closeProject(app, false);
                    return;

                case UPGRADE_NEEDED:
                    if (processUpgrades()) {
                        rootSource = upgradeService.upgradeProject(rootSource);
                    } else {
                        CloseProjectAction.closeProject(app, false);
                        return;
                    }
                    break;
            }

            openProjectResource(rootSource, controller, mcpHandshakeNonce);


        } catch (Exception ex) {
            LOGGER.warn("Error loading project file.", ex);
            new ErrorDialog(app, "Error loading project", ex).open();
        }
    }

    private Project openProjectResource(Resource resource, MainFrame controller, String mcpHandshakeNonce) {
        Project project = app.getProjectLoader().loadProject(resource);
        controller.onProjectOpened(project, mcpHandshakeNonce);
        return project;
    }

    private boolean processUpgrades() {
        // need an upgrade
        int returnCode = JOptionPane.showConfirmDialog(
                app.getFrame(),
                "Project needs an upgrade to a newer version. Upgrade?",
                "Upgrade Needed",
                JOptionPane.YES_NO_OPTION);
        return returnCode != JOptionPane.NO_OPTION;
    }

    private void resetClipboard() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[0];
            }

            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return false;
            }

            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                throw new UnsupportedFlavorException(flavor);
            }
        }, null);
    }

    private File openProjectFile() {
        File startDir = new RecentProjectsPrefs(app.getPrefsLocator().appNode(RecentProjectsPrefs.NODE)).getStartDir();
        return app
                .getFileChooser(app.getFrame(), "Select Project File")
                .openFile(startDir, FileFilters.getApplicationFilter());
    }
}
