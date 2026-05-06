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
import org.apache.cayenne.modeler.event.model.RecentFileListListener;
import org.apache.cayenne.modeler.pref.RecentProjectsPrefs;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.ui.action.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class MainMenuBar extends JMenuBar {

    private final List<RecentFileListListener> recentFileListeners;
    private final Application app;

    MainMenuBar(Application app) {
        this.recentFileListeners = new ArrayList<>();
        this.app = app;
        initLayout();
    }

    private void initLayout() {
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu viewMenu = new JMenu("View");
        JMenu projectMenu = new JMenu("Project");
        JMenu toolMenu = new JMenu("Tools");
        JMenu helpMenu = new JMenu("Help");

        fileMenu.setMnemonic(KeyEvent.VK_F);
        editMenu.setMnemonic(KeyEvent.VK_E);
        viewMenu.setMnemonic(KeyEvent.VK_V);
        projectMenu.setMnemonic(KeyEvent.VK_P);
        toolMenu.setMnemonic(KeyEvent.VK_T);
        helpMenu.setMnemonic(KeyEvent.VK_H);

        GlobalActions actions = app.getActionManager();

        fileMenu.add(actions.getAction(NewProjectAction.class).buildMenu());
        fileMenu.add(actions.getAction(OpenProjectAction.class).buildMenu());
        fileMenu.add(actions.getAction(CloseProjectAction.class).buildMenu());
        fileMenu.add(actions.getAction(ImportDataMapAction.class).buildMenu());
        fileMenu.addSeparator();
        fileMenu.add(actions.getAction(SaveAction.class).buildMenu());
        fileMenu.add(actions.getAction(SaveAsAction.class).buildMenu());
        fileMenu.add(actions.getAction(RevertAction.class).buildMenu());
        fileMenu.addSeparator();

        editMenu.add(actions.getAction(UndoAction.class).buildMenu());
        editMenu.add(actions.getAction(RedoAction.class).buildMenu());
        editMenu.add(actions.getAction(CutAction.class).buildMenu());
        editMenu.add(actions.getAction(CopyAction.class).buildMenu());
        editMenu.add(actions.getAction(PasteAction.class).buildMenu());

        RecentFileMenu recentFileMenu = new RecentFileMenu(
                "Recent Projects",
                actions.getAction(OpenProjectAction.class));
        recentFileListeners.add(recentFileMenu);
        fileMenu.add(recentFileMenu);

        fileMenu.addSeparator();
        fileMenu.add(actions.getAction(ExitAction.class).buildMenu());

        projectMenu.add(actions.getAction(ValidateAction.class).buildMenu());
        projectMenu.add(actions.getAction(ShowValidationConfigAction.class).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(actions.getAction(CreateNodeAction.class).buildMenu());
        projectMenu.add(actions.getAction(CreateDataMapAction.class).buildMenu());

        projectMenu.add(actions.getAction(CreateObjEntityAction.class).buildMenu());
        projectMenu.add(actions.getAction(CreateEmbeddableAction.class).buildMenu());
        projectMenu.add(actions.getAction(CreateDbEntityAction.class).buildMenu());

        projectMenu.add(actions.getAction(CreateProcedureAction.class).buildMenu());
        projectMenu.add(actions.getAction(CreateQueryAction.class).buildMenu());

        projectMenu.addSeparator();
        projectMenu.add(actions.getAction(ObjEntitySyncAction.class).buildMenu());
        projectMenu.add(actions.getAction(DbEntitySyncAction.class).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(actions.getAction(RemoveAction.class).buildMenu());

        // The action's Action.SELECTED_KEY drives the checkbox state, so it stays in sync
        // no matter where the toggle originates (menu click, the close button on the
        // docked panel, etc.).
        viewMenu.add(actions.getAction(ShowLogConsoleAction.class).buildCheckBoxMenu());

        toolMenu.add(actions.getAction(InferRelationshipsAction.class).buildMenu());
        toolMenu.add(actions.getAction(ImportEOModelAction.class).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(actions.getAction(GenerateDBAction.class).buildMenu());
        toolMenu.add(actions.getAction(MigrateAction.class).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(actions.getAction(ConfigurePreferencesAction.class).buildMenu());

        helpMenu.add(actions.getAction(AboutAction.class).buildMenu());
        helpMenu.add(actions.getAction(DocumentationAction.class).buildMenu());

        add(fileMenu);
        add(editMenu);
        add(viewMenu);
        add(projectMenu);
        add(toolMenu);
        add(helpMenu);
    }

    void addRecentFileListener(RecentFileListListener listener) {
        recentFileListeners.add(listener);
    }

    void fireRecentFileListChanged() {
        for (RecentFileListListener listener : recentFileListeners) {
            listener.recentFileListChanged();
        }
    }

    class RecentFileMenu extends JMenu implements RecentFileListListener {

        private final OpenProjectAction action;

        public RecentFileMenu(String s, OpenProjectAction action) {
            super(s);
            this.action = action;
        }

        /**
         * Rebuilds internal menu items list with the files stored in CayenneModeler preferences.
         */
        public void rebuildFromPreferences() {

            List<File> files = new RecentProjectsPrefs(app.getPreferencesRepository()).getFiles();

            // read menus
            Component[] comps = getMenuComponents();
            int curSize = comps.length;
            int prefSize = files.size();

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
