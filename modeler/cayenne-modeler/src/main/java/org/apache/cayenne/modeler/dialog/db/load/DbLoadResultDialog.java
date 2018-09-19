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

package org.apache.cayenne.modeler.dialog.db.load;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.FlowLayout;

/**
 * @since 4.1
 */
public class DbLoadResultDialog extends JDialog {

    private static final int TABLE_ROW_HIGH = 24;
    private static final int TABLE_ROW_MARGIN = 3;

    private DefaultTableModel tableModel;
    private JTable table;
    private JButton okButton;
    private String title;

    DbLoadResultDialog(String title) {
        super();
        this.title = title;
        initElements();
        buildElements();
        configureDialog();
    }

    private void configureDialog() {
        this.setResizable(false);
        this.setTitle(title);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setModal(false);
        this.setAlwaysOnTop(true);
    }

    private void initElements() {
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(TABLE_ROW_HIGH);
        table.setRowMargin(TABLE_ROW_MARGIN);
        tableModel.addColumn("");
        okButton = new JButton("OK");
        okButton.addActionListener(e -> DbLoadResultDialog.this.setVisible(false));
    }

    private void buildElements() {
        getRootPane().setDefaultButton(okButton);

        FormLayout layout = new FormLayout("fill:200dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(okButton);
        builder.append(panel);
        this.add(builder.getPanel());
    }

    public void addRowToOutput(String output) {
        tableModel.addRow(new Object[]{output});
    }

    public int getTableRowCount() {
        return tableModel.getRowCount();
    }
}
