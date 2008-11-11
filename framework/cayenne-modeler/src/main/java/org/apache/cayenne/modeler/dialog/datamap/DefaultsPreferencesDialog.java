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

package org.apache.cayenne.modeler.dialog.datamap;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;

import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.SRadioButton;
import org.scopemvc.view.swing.SwingView;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 */
public class DefaultsPreferencesDialog extends SPanel {

    public DefaultsPreferencesDialog(String allControl, String uninitializedControl) {
        initView(allControl, uninitializedControl);
    }

    protected void initView(String allControl, String uninitializedControl) {
        SRadioButton updateAll = new SRadioButton(
                allControl,
                DefaultsPreferencesModel.ALL_ENTITIES_SELECTOR);

        SRadioButton updateEmpty = new SRadioButton(
                uninitializedControl,
                DefaultsPreferencesModel.UNINITIALIZED_ENTITIES_SELECTOR);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(updateAll);
        buttonGroup.add(updateEmpty);

        SButton updateButton = new SButton(
                DefaultsPreferencesController.UPDATE_CONTROL);
        SButton cancelButton = new SButton(DefaultsPreferencesController.CANCEL_CONTROL);

        // assemble
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(updateButton);
        buttonPanel.add(cancelButton);

        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout("left:max(180dlu;pref)", "p, 3dlu, p, 3dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.add(updateAll, cc.xy(1, 1));
        builder.add(updateEmpty, cc.xy(1, 3));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setDisplayMode(SwingView.MODAL_DIALOG);
    }
}
