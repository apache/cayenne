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

package org.apache.cayenne.modeler.ui.preferences.classpath;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.toolkit.table.CMTable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;


public class ClasspathPreferencesView extends JPanel {

    private final ClasspathTableModel tableModel;

    public ClasspathPreferencesView(ClasspathPreferencesController controller) {

        this.tableModel = new ClasspathTableModel(controller);

        JButton addJarButton = new JButton("Add Jar");
        JButton addDirButton = new JButton("Add Class Folder");
        JButton addMvnButton = new JButton("Get From Maven Central");
        JButton deleteEntryButton = new JButton("Delete");

        JTable table = new CMTable();
        table.setRowMargin(3);
        table.setRowHeight(25);
        table.setTableHeader(null);
        table.setModel(tableModel);

        addJarButton.addActionListener(e -> controller.addJarClicked());
        addDirButton.addActionListener(e -> controller.addClassDirectoryClicked());
        addMvnButton.addActionListener(e -> controller.addMvnDependencyClicked());
        deleteEntryButton.addActionListener(e -> removeEntryClicked(controller, table, tableModel));

        DefaultFormBuilder sidebar = new DefaultFormBuilder(
                new FormLayout("fill:min(150dlu;pref)", ""));
        sidebar.append(addJarButton);
        sidebar.append(addDirButton);
        sidebar.append(addMvnButton);
        sidebar.append(deleteEntryButton);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel content = new JPanel(new BorderLayout());
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(sidebar.getPanel(), BorderLayout.EAST);

        CellConstraints cc = new CellConstraints();
        PanelBuilder outer = new PanelBuilder(new FormLayout(
                "fill:default:grow",
                "p, 3dlu, fill:default:grow"));
        outer.setDefaultDialogBorder();
        outer.addSeparator("Extra Classpath", cc.xy(1, 1));
        outer.add(content, cc.xy(1, 3));

        setLayout(new BorderLayout());
        add(outer.getPanel(), BorderLayout.CENTER);
    }

    public void entryAdded() {
        int len = tableModel.getRowCount();

        tableModel.fireTableRowsInserted(len, len);
    }

    private void removeEntryClicked(ClasspathPreferencesController controller, JTable table, AbstractTableModel tableModel) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        controller.entryRemoved(selectedRow);
        tableModel.fireTableRowsDeleted(selectedRow, selectedRow);
    }

    private static class ClasspathTableModel extends AbstractTableModel {

        private final ClasspathPreferencesController controller;

        ClasspathTableModel(ClasspathPreferencesController controller) {
            this.controller = controller;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public int getRowCount() {
            return controller.getEntries().size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return controller.getEntries().get(rowIndex);
        }

        @Override
        public String getColumnName(int column) {
            return "Custom ClassPath";
        }
    }
}
