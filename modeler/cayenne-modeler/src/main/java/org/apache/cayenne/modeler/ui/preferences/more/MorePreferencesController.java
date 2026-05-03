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

package org.apache.cayenne.modeler.ui.preferences.more;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.pref.PreferencesRepository;
import org.apache.cayenne.modeler.ui.action.ProjectAction;
import org.apache.cayenne.modeler.ui.preferences.PreferenceDialogController;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class MorePreferencesController extends ChildController<PreferenceDialogController> {

    private final MorePreferencesView view;

    public MorePreferencesController(PreferenceDialogController parent) {
        super(parent);
        this.view = new MorePreferencesView(this);
    }

    @Override
    public Component getView() {
        return view;
    }

    void copyAllClicked() {
        PreferencesRepository repository = getApplication().getPreferencesRepository();
        String json = repository.exportAsJson();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
    }

    void resetToDefaultsClicked(boolean importLegacy) {
        int answer = JOptionPane.showConfirmDialog(
                view,
                "Resetting preferences to defaults requires closing and restarting CayenneModeler. Continue?",
                "Reset Preferences to Defaults",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        // Close the open project first (with the standard unsaved-changes
        // prompt). closeProject is a no-op when no project is open. If the user
        // cancels the save prompt, abort the wipe entirely.
        ProjectAction projectAction = getApplication().getActionManager().getAction(ProjectAction.class);
        if (!projectAction.closeProject(true)) {
            return;
        }

        Application application = getApplication();

        // Tear down the visible UI: the preferences dialog and the main frame.
        // The frame holds the docked log console, so disposing it cleans up
        // both. Subsequent startup() will rebuild everything.
        Window prefsDialog = SwingUtilities.getWindowAncestor(view);
        if (prefsDialog != null) {
            prefsDialog.dispose();
        }
        application.getFrameController().getView().dispose();

        application.getPreferencesRepository().resetToDefaults(importLegacy);

        // Defer the rebuild to a later EDT tick so the in-flight action handler
        // (and any pending dispose events) drain first.
        SwingUtilities.invokeLater(() -> application.startup(null));
    }
}
