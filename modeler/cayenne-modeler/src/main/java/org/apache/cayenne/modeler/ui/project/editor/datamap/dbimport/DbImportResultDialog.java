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

package org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.AppDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DbImportResultDialog extends AppDialog {

    private static final String DIALOG_TITLE = "Db Import Result";
    private static final int TABLE_ROW_HIGH = 24;
    private static final int TABLE_ROW_MARGIN = 3;

    private final JButton okButton;
    private final JButton revertButton;
    private final ConcurrentMap<DataMap, JTable> tableForMap;
    private final JPanel tablePanel;
    private final JPanel buttonPanel;
    private final JScrollPane scrollPane;

    // TODO: globalImport is import-session state, not dialog state. It is read/written
    //  independently of the dialog (DataDomainDbImportTab, ProjectTree, DbSyncDbImportAction).
    //  Move to a session-scoped service when one exists.
    private boolean globalImport;

    public DbImportResultDialog(Application application, Window owner) {
        super(application, owner, DIALOG_TITLE, ModalityType.MODELESS);

        this.tableForMap = new ConcurrentHashMap<>();
        this.tablePanel = new JPanel();
        this.tablePanel.setLayout(new BoxLayout(this.tablePanel, BoxLayout.Y_AXIS));
        this.scrollPane = new JScrollPane(tablePanel,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        this.revertButton = new JButton("Revert");
        this.okButton = new JButton("OK");

        initLayout();

        setResizable(true);
        setPreferredSize(new Dimension(400, 400));
        pack();
    }

    private void initLayout() {
        buttonPanel.add(revertButton);
        buttonPanel.add(okButton);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        this.add(mainPanel);

        getRootPane().setDefaultButton(okButton);
    }

    public void showDialog() {
        pack();
        setLocationRelativeTo(app.getFrame().getProjectView());
        setVisible(true);
    }

    public void setGlobalImport(boolean globalImport) {
        this.globalImport = globalImport;
    }

    public boolean isGlobalImport() {
        return globalImport;
    }

    public void resetDialog() {
        for (DataMap dataMap : tableForMap.keySet()) {
            clearTable(dataMap);
        }
        tableForMap.clear();
        removeListenersFromButtons();
        tablePanel.removeAll();
    }

    public void clearTable(DataMap dataMap) {
        JTable table = tableForMap.get(dataMap);
        if (table == null) {
            return;
        }
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        int rowCount = tableModel.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            tableModel.removeRow(i);
        }
    }

    private DefaultTableModel prepareTable(DataMap dataMap) {
        if (tableForMap.containsKey(dataMap)) {
            return (DefaultTableModel) tableForMap.get(dataMap).getModel();
        }
        DefaultTableModel tokensTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JPanel tablePane = new JPanel(new BorderLayout());
        JLabel dataMapLabel = new JLabel(String.format("    %-20s", dataMap.getName()));
        dataMapLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
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
        for (ActionListener actionListener : okButton.getActionListeners()) {
            okButton.removeActionListener(actionListener);
        }
        for (ActionListener actionListener : revertButton.getActionListeners()) {
            revertButton.removeActionListener(actionListener);
        }
    }

    public void refreshElements() {
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
