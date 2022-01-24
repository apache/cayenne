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

package org.apache.cayenne.modeler.editor.cgen;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;

/**
 * @since 4.1
 */
public class GeneratorTabPanel extends JPanel {

    private static final String LAYOUT_COLUMN_SPECS = "p, 2dlu, p, 2dlu, p, 2dlu, p";
    private final JRadioButton standardModeRButton;
    private final JRadioButton clientModeRButton;
    private final JRadioButton customTemplateModeRButton;
    private final CardLayout modeLayout;
    private final JPanel modesPanel;


    public GeneratorTabPanel( StandardModePanel standardModePanel, StandardModePanel clientModePanel, CustomModePanel customModePanel) {

        this.standardModeRButton = new JRadioButton("Standard Persistent Objects", true);
        this.clientModeRButton = new JRadioButton("Client Persistent Objects");
        this.customTemplateModeRButton = new JRadioButton("Custom template mode");
        this.modeLayout = new CardLayout();
        this.modesPanel = new JPanel(modeLayout);

        initiateRadioButtons();
        buildView(standardModePanel,clientModePanel,customModePanel);
        this.setPreferredSize(new Dimension(550, 480));
    }

    private void buildView(StandardModePanel standardModePanel, StandardModePanel clientModePanel, CustomModePanel customModePanel) {
        FormLayout layout = new FormLayout(LAYOUT_COLUMN_SPECS, "p");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.append(standardModeRButton);
        builder.append(clientModeRButton);
        builder.append(customTemplateModeRButton);

        modesPanel.add(standardModePanel, GenerationModes.STANDARD_MODE.getMode());
        modesPanel.add(clientModePanel, GenerationModes.CLIENT_MODE.getMode());
        modesPanel.add(customModePanel, GenerationModes.CUSTOM_MODE.getMode());

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(builder.getPanel(), BorderLayout.NORTH);
        panel.add(modesPanel, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    private void initiateRadioButtons() {
        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(standardModeRButton);
        radioButtonGroup.add(clientModeRButton);
        radioButtonGroup.add(customTemplateModeRButton);

        standardModeRButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                modeLayout.show(modesPanel, GenerationModes.STANDARD_MODE.getMode());
            }
        });

        clientModeRButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                modeLayout.show(modesPanel, GenerationModes.CLIENT_MODE.getMode());
            }
        });

        customTemplateModeRButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                modeLayout.show(modesPanel, GenerationModes.CUSTOM_MODE.getMode());
            }
        });
    }

    public JRadioButton getStandardModeRButton() {
        return standardModeRButton;
    }

    public JRadioButton getClientModeRButton() {
        return clientModeRButton;
    }

    public JRadioButton getCustomTemplateModeRButton() {
        return customTemplateModeRButton;
    }
}
