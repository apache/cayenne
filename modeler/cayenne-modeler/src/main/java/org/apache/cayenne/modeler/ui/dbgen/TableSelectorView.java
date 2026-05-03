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

package org.apache.cayenne.modeler.ui.dbgen;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.modeler.toolkit.table.CMTable;
import org.apache.cayenne.modeler.toolkit.table.TableSizer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

public class TableSelectorView extends JPanel {

    private static final String[] COLUMN_HEADERS = {"", "Table", "Problems"};
    private static final Class<?>[] COLUMN_CLASSES = {Boolean.class, String.class, String.class};

    private final JCheckBox checkAll;
    private final TableSelectorTableModel tableModel;

    public TableSelectorView(TableSelectorController controller) {

        this.checkAll = new JCheckBox();
        this.tableModel = new TableSelectorTableModel(controller);

        JLabel checkAllLabel = new JLabel("Check All Tables");

        checkAll.addItemListener(event -> {
            if (checkAll.isSelected()) {
                checkAllLabel.setText("Uncheck All Tables");
            } else {
                checkAllLabel.setText("Check All Tables");
            }
        });
        checkAll.addActionListener(e -> controller.checkAllClicked(checkAll.isSelected()));

        JTable tables = new CMTable();
        tables.setRowHeight(25);
        tables.setRowMargin(3);
        tables.setModel(tableModel);
        TableSizer.sizeColumns(tables, Boolean.TRUE, "XXXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        // assemble
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        topPanel.add(checkAll);
        topPanel.add(checkAllLabel);

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(50dlu;pref):grow",
                "p, 3dlu, fill:40dlu:grow"));
        builder.setDefaultDialogBorder();
        builder.addSeparator("Select Tables", cc.xy(1, 1));
        builder.add(new JScrollPane(
                tables,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), cc.xy(1, 3));

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public void tablesChanged() {
        tableModel.fireTableDataChanged();
    }

    public void setCheckAll(boolean selected) {
        checkAll.setSelected(selected);
    }

    private static class TableSelectorTableModel extends AbstractTableModel {

        private final TableSelectorController controller;

        TableSelectorTableModel(TableSelectorController controller) {
            this.controller = controller;
        }

        @Override
        public int getRowCount() {
            return controller.getTables() != null ? controller.getTables().size() : 0;
        }

        @Override
        public int getColumnCount() {
            return COLUMN_HEADERS.length;
        }

        @Override
        public String getColumnName(int col) {
            return COLUMN_HEADERS[col];
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return COLUMN_CLASSES[col];
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 0;
        }

        @Override
        public Object getValueAt(int row, int col) {
            DbEntity entity = controller.getTables().get(row);
            if (col == 0) return controller.isIncluded(entity);
            if (col == 1) return entity.getName();
            return controller.getProblem(entity);
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                controller.setIncluded(controller.getTables().get(row), (Boolean) value);
            }
        }
    }
}
