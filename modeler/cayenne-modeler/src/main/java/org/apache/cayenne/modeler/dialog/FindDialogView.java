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
package org.apache.cayenne.modeler.dialog;

import org.apache.cayenne.modeler.action.FindAction;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.swing.ImageRendererColumn;
import org.apache.cayenne.swing.components.TopBorder;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.util.List;


/**
 * Swing component displaying results produced by search feature.
 */
public class FindDialogView extends JDialog {

    private JButton okButton;
    private JTable table;
    private int index;

    public JTable getTable() {
        return table;
    }

    public FindDialogView(List<FindAction.SearchResultEntry> searchResults) {
        super((Frame) null, "Search results", true);

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableModel.setDataVector(convertToDataVector(searchResults), new Object[] {""});

        table = new JTable(tableModel);
        table.getColumnModel().getColumn(0).setCellRenderer(new ImageRendererColumn());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        InputMap im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        InputMap imParent = im.getParent();
        imParent.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        im.setParent(imParent);
        im.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        table.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, im);

        JPanel okPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        okButton = new JButton("OK");
        getRootPane().setDefaultButton(okButton);
        okPanel.setBorder(TopBorder.create());
        okPanel.add(okButton);

        JComponent contentPane = (JComponent) getContentPane();
        contentPane.setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentPane.add(scrollPane);
        contentPane.add(okPanel, BorderLayout.SOUTH);
        contentPane.setPreferredSize(new Dimension(400, 325));
    }

    private JLabel[][] convertToDataVector(List<FindAction.SearchResultEntry> resultEntries) {
        JLabel[][] dataVector = new JLabel[resultEntries.size()][1];
        for (FindAction.SearchResultEntry entry : resultEntries) {
            JLabel labelIcon = new JLabel();
            labelIcon.setIcon(CellRenderers.iconForObject(entry.getObject()));
            labelIcon.setText(entry.getName());
            dataVector[index++] = new JLabel[]{labelIcon};
        }

        return dataVector;
    }

    public JButton getOkButton() {
        return okButton;
    }
}
