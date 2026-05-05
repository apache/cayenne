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

package org.apache.cayenne.modeler.ui.project.editor.datamap.defaults;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.toolkit.ProjectDialog;
import org.apache.cayenne.modeler.project.ProjectSession;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * Shared base for "apply default to DbEntities" dialogs (catalog, schema, package,
 * superclass). Provides the standard radio-button layout (Update All / Don't override
 * existing) and modal lifecycle. Subclasses supply the labels, title, and the per-row
 * update action.
 */
public abstract class DefaultsUpdateDialog extends ProjectDialog {

    protected final DataMap dataMap;

    private final JRadioButton updateAll;
    private final JRadioButton updateEmpty;
    private final JButton updateButton;
    private final JButton cancelButton;

    protected DefaultsUpdateDialog(ProjectSession session, Window owner, DataMap dataMap,
                                   String title, String allControl, String uninitializedControl) {
        super(session, owner, title, ModalityType.APPLICATION_MODAL);
        this.dataMap = dataMap;

        this.updateAll = new JRadioButton(allControl, true);
        this.updateEmpty = new JRadioButton(uninitializedControl);
        this.updateButton = new JButton("Update");
        this.cancelButton = new JButton("Cancel");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(updateAll);
        buttonGroup.add(updateEmpty);

        initLayout();
        initBindings();
    }

    /**
     * @return true if "update all" radio is selected, false if "don't override existing".
     */
    protected boolean isAllEntities() {
        return updateAll.isSelected();
    }

    /**
     * Performs the actual update. Subclasses iterate over data and fire model events.
     * Should NOT close the dialog — that's handled in the surrounding flow.
     */
    protected abstract void applyUpdate();

    private void initLayout() {
        getRootPane().setDefaultButton(updateButton);

        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout("left:max(180dlu;pref)", "p, 3dlu, p, 3dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.add(updateAll, cc.xy(1, 1));
        builder.add(updateEmpty, cc.xy(1, 3));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(updateButton);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void initBindings() {
        cancelButton.addActionListener(e -> dispose());
        updateButton.addActionListener(e -> {
            applyUpdate();
            dispose();
        });
    }
}
