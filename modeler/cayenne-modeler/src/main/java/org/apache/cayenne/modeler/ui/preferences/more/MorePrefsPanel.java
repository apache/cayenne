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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.PreferencesRepository;
import org.apache.cayenne.modeler.toolkit.AppPanel;
import org.apache.cayenne.modeler.ui.action.ProjectAction;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;

public class MorePrefsPanel extends AppPanel {

    private final JButton copyAllButton;
    private final JButton resetToDefaultsButton;
    private final JCheckBox importLegacyPreferencesCheckBox;

    public MorePrefsPanel(Application app) {
        super(app);

        this.copyAllButton = new JButton("Copy All to Clipboard");
        this.resetToDefaultsButton = new JButton("Reset to Defaults");
        this.importLegacyPreferencesCheckBox = new JCheckBox("Import older Modeler preferences if available", true);
        this.importLegacyPreferencesCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        initLayout();
        initBindings();
    }

    private void initLayout() {
        FormLayout layout = new FormLayout(
                "12dlu, default:grow",
                "p, 6dlu, p, 12dlu, p, 6dlu, p, 3dlu, p, fill:default:grow");

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("Export Preferences", cc.xyw(1, 1, 2));
        builder.add(copyAllButton, cc.xy(2, 3, CellConstraints.LEFT, CellConstraints.DEFAULT));

        builder.addSeparator("Reset Preferences", cc.xyw(1, 5, 2));
        builder.add(resetToDefaultsButton, cc.xy(2, 7, CellConstraints.LEFT, CellConstraints.DEFAULT));
        builder.add(importLegacyPreferencesCheckBox, cc.xy(2, 9, CellConstraints.LEFT, CellConstraints.DEFAULT));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initBindings() {
        copyAllButton.addActionListener(e -> copyAllClicked());
        resetToDefaultsButton.addActionListener(e -> resetToDefaultsClicked(importLegacyPreferencesCheckBox.isSelected()));
    }

    private void copyAllClicked() {
        PreferencesRepository repository = app().getPreferencesRepository();
        String json = repository.exportAsJson();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
    }

    private void resetToDefaultsClicked(boolean importLegacy) {
        int answer = JOptionPane.showConfirmDialog(
                this,
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
        ProjectAction projectAction = app().getActionManager().getAction(ProjectAction.class);
        if (!projectAction.closeProject(true)) {
            return;
        }

        // Tear down the visible UI: the preferences dialog and the main frame.
        // The frame holds the docked log console, so disposing it cleans up
        // both. Subsequent startup() will rebuild everything.
        Window prefsDialog = SwingUtilities.getWindowAncestor(this);
        if (prefsDialog != null) {
            prefsDialog.dispose();
        }
        app().getFrame().dispose();

        app().getPreferencesRepository().resetToDefaults(importLegacy);

        // Defer the rebuild to a later EDT tick so the in-flight action handler
        // (and any pending dispose events) drain first.
        SwingUtilities.invokeLater(() -> app().startup(null));
    }
}
