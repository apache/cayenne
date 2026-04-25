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

package org.apache.cayenne.modeler.ui;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.action.*;
import org.apache.cayenne.modeler.event.model.RecentFileListListener;
import org.apache.cayenne.modeler.pref.LastProjectsPreferences;
import org.apache.cayenne.modeler.ui.action.*;
import org.apache.cayenne.modeler.ui.logconsole.LogConsoleController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ModelerMenuBar extends JMenuBar {

    private final LogConsoleController logConsoleController;
    private final JCheckBoxMenuItem logMenu;
    private final List<RecentFileListListener> recentFileListeners;

    ModelerMenuBar(ActionManager actionManager, LogConsoleController logConsoleController) {
        this.logConsoleController = logConsoleController;
        this.recentFileListeners = new ArrayList<>();

        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu projectMenu = new JMenu("Project");
        JMenu toolMenu = new JMenu("Tools");
        JMenu helpMenu = new JMenu("Help");

        fileMenu.setMnemonic(KeyEvent.VK_F);
        editMenu.setMnemonic(KeyEvent.VK_E);
        projectMenu.setMnemonic(KeyEvent.VK_P);
        toolMenu.setMnemonic(KeyEvent.VK_T);
        helpMenu.setMnemonic(KeyEvent.VK_H);

        fileMenu.add(actionManager.getAction(NewProjectAction.class).buildMenu());
        fileMenu.add(actionManager.getAction(OpenProjectAction.class).buildMenu());
        fileMenu.add(actionManager.getAction(ProjectAction.class).buildMenu());
        fileMenu.add(actionManager.getAction(ImportDataMapAction.class).buildMenu());
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getAction(SaveAction.class).buildMenu());
        fileMenu.add(actionManager.getAction(SaveAsAction.class).buildMenu());
        fileMenu.add(actionManager.getAction(RevertAction.class).buildMenu());
        fileMenu.addSeparator();

        editMenu.add(actionManager.getAction(UndoAction.class).buildMenu());
        editMenu.add(actionManager.getAction(RedoAction.class).buildMenu());
        editMenu.add(actionManager.getAction(CutAction.class).buildMenu());
        editMenu.add(actionManager.getAction(CopyAction.class).buildMenu());
        editMenu.add(actionManager.getAction(PasteAction.class).buildMenu());

        RecentFileMenu recentFileMenu = new RecentFileMenu("Recent Projects");
        recentFileListeners.add(recentFileMenu);
        fileMenu.add(recentFileMenu);

        fileMenu.addSeparator();
        fileMenu.add(actionManager.getAction(ExitAction.class).buildMenu());

        projectMenu.add(actionManager.getAction(ValidateAction.class).buildMenu());
        projectMenu.add(actionManager.getAction(ShowValidationConfigAction.class).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(actionManager.getAction(CreateNodeAction.class).buildMenu());
        projectMenu.add(actionManager.getAction(CreateDataMapAction.class).buildMenu());

        projectMenu.add(actionManager.getAction(CreateObjEntityAction.class).buildMenu());
        projectMenu.add(actionManager.getAction(CreateEmbeddableAction.class).buildMenu());
        projectMenu.add(actionManager.getAction(CreateDbEntityAction.class).buildMenu());

        projectMenu.add(actionManager.getAction(CreateProcedureAction.class).buildMenu());
        projectMenu.add(actionManager.getAction(CreateQueryAction.class).buildMenu());

        projectMenu.addSeparator();
        projectMenu.add(actionManager.getAction(ObjEntitySyncAction.class).buildMenu());
        projectMenu.add(actionManager.getAction(DbEntitySyncAction.class).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(actionManager.getAction(RemoveAction.class).buildMenu());

        toolMenu.add(actionManager.getAction(InferRelationshipsAction.class).buildMenu());
        toolMenu.add(actionManager.getAction(ImportEOModelAction.class).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(actionManager.getAction(GenerateCodeAction.class).buildMenu());
        toolMenu.add(actionManager.getAction(GenerateDBAction.class).buildMenu());
        toolMenu.add(actionManager.getAction(MigrateAction.class).buildMenu());

        // Menu for opening Log console
        toolMenu.addSeparator();
        logMenu = actionManager.getAction(ShowLogConsoleAction.class).buildCheckBoxMenu();

        if (!logConsoleController.getConsoleProperty(LogConsoleController.DOCKED_PROPERTY)
                && logConsoleController.getConsoleProperty(LogConsoleController.SHOW_CONSOLE_PROPERTY)) {
            logConsoleController.setConsoleProperty(LogConsoleController.SHOW_CONSOLE_PROPERTY, false);
        }

        updateLogConsoleMenu();
        toolMenu.add(logMenu);

        toolMenu.addSeparator();
        toolMenu.add(actionManager.getAction(ConfigurePreferencesAction.class).buildMenu());

        helpMenu.add(actionManager.getAction(AboutAction.class).buildMenu());
        helpMenu.add(actionManager.getAction(DocumentationAction.class).buildMenu());

        add(fileMenu);
        add(editMenu);
        add(projectMenu);
        add(toolMenu);
        add(helpMenu);
    }

    /**
     * Selects/deselects menu item, depending on status of log console
     */
    void updateLogConsoleMenu() {
        logMenu.setSelected(logConsoleController.getConsoleProperty(LogConsoleController.SHOW_CONSOLE_PROPERTY));
    }

    void addRecentFileListener(RecentFileListListener listener) {
        recentFileListeners.add(listener);
    }

    void fireRecentFileListChanged() {
        for (RecentFileListListener listener : recentFileListeners) {
            listener.recentFileListChanged();
        }
    }

    static class RecentFileMenu extends JMenu implements RecentFileListListener {

        public RecentFileMenu(String s) {
            super(s);
        }

        /**
         * Rebuilds internal menu items list with the files stored in CayenneModeler preferences.
         */
        public void rebuildFromPreferences() {

            List<File> files = LastProjectsPreferences.getFiles();

            // read menus
            Component[] comps = getMenuComponents();
            int curSize = comps.length;
            int prefSize = files.size();

            OpenProjectAction action = Application.getInstance().getActionManager().getAction(OpenProjectAction.class);

            for (int i = 0; i < prefSize; i++) {
                String name = files.get(i).getAbsolutePath();
                if (i < curSize) {
                    ((JMenuItem) comps[i]).setText(name);
                } else {

                    JMenuItem item = new JMenuItem(name) {
                        @Override
                        protected void configurePropertiesFromAction(Action a) {
                            // exclude most generic action keys that are not applicable here
                            setIcon((Icon) a.getValue(Action.SMALL_ICON));
                            setEnabled(a.isEnabled());
                        }
                    };

                    item.setAction(action);
                    add(item);
                }
            }

            // remove any hanging items
            for (int i = curSize - 1; i >= prefSize; i--) {
                remove(i);
            }
        }

        @Override
        public void recentFileListChanged() {
            rebuildFromPreferences();
            setEnabled(getMenuComponentCount() > 0);
        }
    }
}
