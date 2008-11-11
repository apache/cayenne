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
import java.awt.Container;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

/**
 */
public class PreferenceDialogView extends JDialog {

    protected JSplitPane split;
    protected JList list;
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
        this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.list = new JList();
        this.detailLayout = new CardLayout();
        this.detailPanel = new JPanel(detailLayout);
        this.saveButton = new JButton("Save");
        this.cancelButton = new JButton("Cancel");

        // assemble

        Container leftContainer = new JPanel(new BorderLayout());
        leftContainer.add(new JScrollPane(list));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(saveButton);

        Container rightContainer = new JPanel(new BorderLayout());
        rightContainer.add(detailPanel, BorderLayout.CENTER);
        rightContainer.add(buttons, BorderLayout.SOUTH);

        split.setLeftComponent(leftContainer);
        split.setRightComponent(rightContainer);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(split, BorderLayout.CENTER);
        setTitle("Edit Preferences");
    }

    public JList getList() {
        return list;
    }

    public JSplitPane getSplit() {
        return split;
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
