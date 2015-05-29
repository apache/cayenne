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

package org.apache.cayenne.modeler.dialog.pref;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

/**
 */
public class PreferenceDialogView extends JDialog {

    protected JTabbedPane pane;
    protected CardLayout detailLayout;
    protected Container detailPanel;
    protected JButton cancelButton;
    protected JButton saveButton;

    public PreferenceDialogView(Dialog parent) {
        super(parent);
        init();
    }

    public PreferenceDialogView(Frame parent) {
        super(parent);
        init();
    }

    private void init() {
        this.pane = new JTabbedPane(JTabbedPane.LEFT);
        this.detailLayout = new CardLayout();
        this.detailPanel = new JPanel(detailLayout);
        this.saveButton = new JButton("Save");
        this.cancelButton = new JButton("Cancel");

        
        // assemble
        pane.setBackground(Color.WHITE);
        pane.add(detailPanel);
        Container mainContainer = new JPanel(new BorderLayout());
        mainContainer.add(pane);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(saveButton);

        mainContainer.add(buttons, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainContainer, BorderLayout.CENTER);
        setTitle("Edit Preferences");
    }

    public JTabbedPane getPane() {
        return pane;
    }

    public Container getDetailPanel() {
        return detailPanel;
    }

    public CardLayout getDetailLayout() {
        return detailLayout;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getSaveButton() {
        return saveButton;
    }
}
