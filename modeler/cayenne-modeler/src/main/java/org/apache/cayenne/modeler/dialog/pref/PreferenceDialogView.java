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

import org.apache.cayenne.swing.components.TopBorder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

/**
 */
public class PreferenceDialogView extends JDialog {

    protected JSplitPane split;
    protected JList<Object> list;
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
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBorder(TopBorder.create());
        list = new JList<>();
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 0));
                return this;
            }
        });
        list.setFont(new JLabel().getFont().deriveFont(Font.BOLD, 12));
        detailLayout = new CardLayout();
        detailPanel = new JPanel(detailLayout);
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");

        // assemble

        Container leftContainer = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        leftContainer.add(scrollPane);
        leftContainer.setPreferredSize(new Dimension(180, 400));

        split.setLeftComponent(leftContainer);
        split.setRightComponent(detailPanel);
        split.setDividerSize(3);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(saveButton);
        buttons.setBorder(TopBorder.create());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(split, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
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
