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

package org.apache.cayenne.modeler.dialog.datadomain;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 */
public class CacheSyncConfigView extends JDialog {
    public static final String EMPTY_CARD_KEY = "Empty";

    protected JPanel configPanel;
    protected JComboBox typeSelector;
    protected JButton saveButton;
    protected JButton cancelButton;

    public CacheSyncConfigView() {
        initView();
    }

    protected void initView() {
        this.setLayout(new BorderLayout());
        this.setTitle("Configure Remote Cache Synchronization");

        typeSelector = new JComboBox();
        typeSelector.addItem("JavaGroups Multicast (Default)");
        typeSelector.addItem("JMS Transport");
        typeSelector.addItem("Custom Transport");

        saveButton = new JButton(CacheSyncConfigController.SAVE_CONFIG_CONTROL);
        cancelButton =
            new JButton(CacheSyncConfigController.CANCEL_CONFIG_CONTROL);

        // buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // type form
        FormLayout layout = new FormLayout("right:150, 3dlu, left:200", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.append("Notification Transport Type:", typeSelector);

        // config panel
        configPanel = new JPanel(new CardLayout());
        addCard(new JPanel(), EMPTY_CARD_KEY);

        this.add(builder.getPanel(), BorderLayout.NORTH);
        this.add(configPanel, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);

        showCard(EMPTY_CARD_KEY);
    }

    public void addCard(Component card, String key) {
        configPanel.add(card, key);
    }

    public void showCard(String key) {
        ((CardLayout) configPanel.getLayout()).show(configPanel, key);
    }

    public JButton getSaveButton() {
        return this.saveButton;
    }

    public JButton getCancelButton() {
        return this.cancelButton;
    }

    public JComboBox getTypeSelector() {
        return this.typeSelector;
    }
}
