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

import javax.swing.JPanel;

import org.scopemvc.core.Control;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SComboBox;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.SwingView;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 */
public class CacheSyncConfigDialog extends SPanel {
    public static final String EMPTY_CARD_KEY = "Empty";

    protected JPanel configPanel;

    public CacheSyncConfigDialog() {
        initView();
    }

    protected void initView() {
        setDisplayMode(SwingView.MODAL_DIALOG);
        this.setLayout(new BorderLayout());
        this.setTitle("Configure Remote Cache Synchronization");

        SComboBox type = new SComboBox();
        type.setSelector(CacheSyncTypesModel.NOTIFICATION_TYPES_SELECTOR);
        type.setSelectionSelector(CacheSyncTypesModel.FACTORY_LABEL_SELECTOR);

        SButton saveButton = new SButton(CacheSyncConfigController.SAVE_CONFIG_CONTROL);
        SButton cancelButton =
            new SButton(CacheSyncConfigController.CANCEL_CONFIG_CONTROL);

        // buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // type form
        FormLayout layout = new FormLayout("right:150, 3dlu, left:200", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.append("Notification Transport Type:", type);

        // config panel
        configPanel = new JPanel(new CardLayout());
        addCard(new JPanel(), EMPTY_CARD_KEY);

        this.add(builder.getPanel(), BorderLayout.NORTH);
        this.add(configPanel, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);

        showCard(EMPTY_CARD_KEY);
    }

    public Control getCloseControl() {
        return new Control(CacheSyncConfigController.CANCEL_CONFIG_CONTROL);
    }

    public void addCard(Component card, String key) {
        configPanel.add(card, key);
    }

    public void showCard(String key) {
        ((CardLayout) configPanel.getLayout()).show(configPanel, key);
    }
}
