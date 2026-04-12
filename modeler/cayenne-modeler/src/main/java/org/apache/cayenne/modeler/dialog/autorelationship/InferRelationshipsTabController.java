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
package org.apache.cayenne.modeler.dialog.autorelationship;

import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.TableSizer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

public class InferRelationshipsTabController extends CayenneController {

    public static final String GENERATE_PROPERTY = "generate";

    private static final String[] COLUMN_HEADERS = {"", "Source", "Target", "Join", "Name", "To Many"};
    private static final Class<?>[] COLUMN_CLASSES = {
            Boolean.class, String.class, String.class, String.class, String.class, String.class
    };

    protected InferRelationshipsPanel view;
    protected AbstractTableModel tableModel;

    public InferRelationshipsTabController(InferRelationshipsControllerBase parent) {
        super(parent);
        this.view = new InferRelationshipsPanel();
        initBindings();
    }

    protected InferRelationshipsControllerBase getParentController() {
        return (InferRelationshipsControllerBase) getParent();
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        view.getCheckAll().addActionListener(e -> checkAllAction());

        tableModel = new AbstractTableModel() {
            public int getRowCount() {
                List<?> entities = getParentController().getEntities();
                return entities != null ? entities.size() : 0;
            }
            public int getColumnCount() { return COLUMN_HEADERS.length; }
            public String getColumnName(int col) { return COLUMN_HEADERS[col]; }
            public Class<?> getColumnClass(int col) { return COLUMN_CLASSES[col]; }
            public boolean isCellEditable(int row, int col) { return col == 0; }

            public Object getValueAt(int row, int col) {
                InferredRelationship item = getItem(row);
                if (col == 0) return getParentController().isSelected(item);
                if (col == 1) return item.getSource().getName();
                if (col == 2) return item.getTarget().getName();
                if (col == 3) return getParentController().getJoin(item);
                if (col == 4) return item.getName();
                return getParentController().getToMany(item);
            }

            public void setValueAt(Object value, int row, int col) {
                if (col == 0) {
                    getParentController().setSelected(getItem(row), (Boolean) value);
                    entitySelectedAction();
                }
            }

            private InferredRelationship getItem(int row) {
                return (InferredRelationship) getParentController().getEntities().get(row);
            }
        };

        view.getTable().setModel(tableModel);
        TableSizer.sizeColumns(view.getTable(),
                Boolean.TRUE,
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    }

    /**
     * A callback action that updates the state of Select All checkbox.
     */
    public void entitySelectedAction() {
        int selectedCount = getParentController().getSelectedEntitiesSize();
        if (selectedCount == 0) {
            view.getCheckAll().setSelected(false);
        } else if (selectedCount == getParentController().getEntities().size()) {
            view.getCheckAll().setSelected(true);
        }
    }

    public void checkAllAction() {
        if (getParentController().updateSelection(view.getCheckAll().isSelected() ? o -> true : o -> false)) {
            tableModel.fireTableDataChanged();
        }
    }

}

