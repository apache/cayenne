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

package org.apache.cayenne.modeler.dialog.db.load;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;

/**
 * @since 4.1
 */
public class DbLoadResultDialog extends JDialog {

    private static final int TABLE_ROW_HIGH = 24;
    private static final int TABLE_ROW_MARGIN = 3;

    private JButton okButton;
    private JButton revertButton;
    private String title;

    private ConcurrentMap<DataMap, JTable> tableForMap;
    private JPanel tablePanel;
    private JPanel buttonPanel;
    private JScrollPane scrollPane;

    public DbLoadResultDialog(String title) {
        super();
        this.title = title;
        this.tableForMap = new ConcurrentHashMap<>();
        this.tablePanel = new JPanel();
        this.tablePanel.setLayout(new BoxLayout(this.tablePanel, BoxLayout.Y_AXIS));
        this.scrollPane = new JScrollPane(tablePanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        initElements();
        buildElements();
        configureDialog();
    }

    private void configureDialog() {
        this.setResizable(true);
        this.setTitle(title);
        this.setModal(false);
        this.setPreferredSize(new Dimension(400, 400));
        this.pack();
        this.setLocationRelativeTo(Application.getFrame().getView());
    }

    private void initElements() {
        revertButton = new JButton("Revert");
        okButton = new JButton("OK");
    }

    public void buildElements() {
        buttonPanel.add(revertButton);
        buttonPanel.add(okButton);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        this.add(mainPanel);

        getRootPane().setDefaultButton(okButton);
    }

    private DefaultTableModel prepareTable(DataMap dataMap) {
        if(tableForMap.containsKey(dataMap)) {
            return (DefaultTableModel)tableForMap.get(dataMap).getModel();
        }
        DefaultTableModel tokensTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JPanel tablePane = new JPanel(new BorderLayout());
        JLabel dataMapLabel = new JLabel(String.format("    %-20s", dataMap.getName()));
        dataMapLabel.setBorder(new EmptyBorder(5,0,5,0));
        tablePane.add(dataMapLabel, BorderLayout.NORTH);
        JTable tokensTable = new JTable(tokensTableModel);
        tokensTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tokensTable.setRowHeight(TABLE_ROW_HIGH);
        tokensTable.setRowMargin(TABLE_ROW_MARGIN);
        tokensTableModel.addColumn("");
        tablePane.add(tokensTable, BorderLayout.CENTER);
        tableForMap.put(dataMap, tokensTable);
        tablePanel.add(tablePane);
        return tokensTableModel;
    }

    public void removeListenersFromButtons() {
        for(ActionListener actionListener : okButton.getActionListeners()) {
            okButton.removeActionListener(actionListener);
        }
        for(ActionListener actionListener : revertButton.getActionListeners()) {
            revertButton.removeActionListener(actionListener);
        }
    }

    public void refreshElements(){
        revertButton.setVisible(true);
    }

    public synchronized void addRowToOutput(String output, DataMap dataMap) {
        prepareTable(dataMap).addRow(new Object[]{output});
    }

    public JButton getOkButton() {
        return okButton;
    }

    public JButton getRevertButton() {
        return revertButton;
    }

    public ConcurrentMap<DataMap, JTable> getTableForMap() {
        return tableForMap;
    }

    public JPanel getTablePanel() {
        return tablePanel;
    }
}
