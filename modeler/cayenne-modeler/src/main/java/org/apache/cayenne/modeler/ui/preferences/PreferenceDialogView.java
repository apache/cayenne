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

package org.apache.cayenne.modeler.ui.preferences;

import org.apache.cayenne.modeler.toolkit.border.TopBorder;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;


public class PreferenceDialogView extends JDialog {

    private static final int LIST_CELL_LEFT_PAD = 8;
    private static final int LIST_CELL_RIGHT_PAD = 12;
    private static final int LIST_MIN_WIDTH = 120;
    private static final int LIST_HEIGHT = 400;

    private final JList<String> list;
    private final JPanel leftContainer;
    private final CardLayout detailLayout;
    private final Container detailPanel;

    public PreferenceDialogView(PreferenceDialogController controller, Dialog parent) {
        super(parent);
        this.list = new JList<>();
        this.detailLayout = new CardLayout();
        this.detailPanel = new JPanel(detailLayout);
        this.leftContainer = new JPanel(new BorderLayout());
        init(controller);
    }

    public PreferenceDialogView(PreferenceDialogController controller, Frame parent) {
        super(parent);
        this.list = new JList<>();
        this.detailLayout = new CardLayout();
        this.detailPanel = new JPanel(detailLayout);
        this.leftContainer = new JPanel(new BorderLayout());
        init(controller);
    }

    private void init(PreferenceDialogController controller) {
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(5, LIST_CELL_LEFT_PAD, 5, LIST_CELL_RIGHT_PAD));
                return this;
            }
        });
        list.setFont(new JLabel().getFont().deriveFont(Font.BOLD, 12));

        list.addListSelectionListener(e -> {
            String selection = list.getSelectedValue();
            if (selection != null) {
                controller.cardSelected(selection);
            }
        });

        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        cancelButton.addActionListener(e -> controller.cancelClicked());
        saveButton.addActionListener(e -> controller.saveClicked());

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        leftContainer.add(scrollPane);
        leftContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(TopBorder.create());
        center.add(leftContainer, BorderLayout.WEST);
        center.add(detailPanel, BorderLayout.CENTER);

        getRootPane().setDefaultButton(saveButton);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(saveButton);
        buttons.setBorder(TopBorder.create());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        setTitle("Edit Preferences");
    }

    public void setMenuItems(String[] items) {
        list.setListData(items);
        sizeListToLabels();
    }

    public void addCard(String name, Component card) {
        detailPanel.add(card, name);
    }

    public void showCard(String name) {
        detailLayout.show(detailPanel, name);
        list.setSelectedValue(name, true);
    }

    private void sizeListToLabels() {
        FontMetrics fm = list.getFontMetrics(list.getFont());
        int maxText = 0;
        for (int i = 0; i < list.getModel().getSize(); i++) {
            maxText = Math.max(maxText, fm.stringWidth(list.getModel().getElementAt(i)));
        }
        int width = Math.max(LIST_MIN_WIDTH, maxText + LIST_CELL_LEFT_PAD + LIST_CELL_RIGHT_PAD);
        leftContainer.setPreferredSize(new Dimension(width, LIST_HEIGHT));
        leftContainer.setMinimumSize(new Dimension(width, 0));
    }
}
