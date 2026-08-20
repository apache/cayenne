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
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.table.CMTable;
import org.apache.cayenne.modeler.toolkit.table.TableSizer;
import org.apache.cayenne.modeler.toolkit.AppPanel;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Table-of-tables panel for picking which DbEntities to include in a DB generation run.
 * Includes per-row check boxes, a "check all" toggle, and a "Problems" column populated
 * from project validation failures.
 */
public class TableSelectorPanel extends AppPanel {

    private static final String[] COLUMN_HEADERS = {"", "Table", "Problems"};
    private static final Class<?>[] COLUMN_CLASSES = {Boolean.class, String.class, String.class};

    private final JTable table;
    private final JCheckBox checkAll;
    private final JLabel checkAllLabel;
    private final TableSelectorTableModel tableModel;

    private List<DbEntity> tables;
    private int permanentlyExcludedCount;
    private final Map<String, DbEntity> excludedTables = new HashMap<>();
    private final List<DbEntity> selectableTablesList = new ArrayList<>();
    private final Map<String, String> validationMessages = new HashMap<>();

    public TableSelectorPanel(Application app) {
        super(app);

        this.checkAll = new JCheckBox();
        this.checkAllLabel = new JLabel("Check All Tables");
        this.tableModel = new TableSelectorTableModel();
        this.table = new CMTable();
        this.table.setRowHeight(25);
        this.table.setRowMargin(3);
        this.table.setModel(tableModel);
        TableSizer.sizeColumns(table, Boolean.TRUE, "XXXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        initLayout();
        initBindings();
    }

    public Collection<DbEntity> getExcludedTables() {
        return excludedTables.values();
    }

    public List<DbEntity> getTables() {
        return tables;
    }

    public void updateTables(Collection<DataMap> dataMaps) {
        this.tables = new ArrayList<>();
        for (DataMap dataMap : dataMaps) {
            tables.addAll(dataMap.getDbEntities());
        }

        excludedTables.clear();
        validationMessages.clear();

        Project project = app.getFrame().getProjectSession().project();
        ProjectValidator projectValidator = app.getProjectValidator();
        ValidationResult validationResult = projectValidator.validate(project.getRootNode());

        for (ValidationFailure problem : validationResult.getFailures()) {
            DbEntity failedEntity = null;

            if (problem.getSource() instanceof DbAttribute) {
                failedEntity = ((DbAttribute) problem.getSource()).getEntity();
            } else if (problem.getSource() instanceof DbRelationship) {
                failedEntity = ((DbRelationship) problem.getSource()).getSourceEntity();
            } else if (problem.getSource() instanceof DbEntity) {
                failedEntity = (DbEntity) problem.getSource();
            }

            if (failedEntity == null) {
                continue;
            }

            excludedTables.put(failedEntity.getName(), failedEntity);
            validationMessages.put(failedEntity.getName(), problem.getDescription());
        }

        permanentlyExcludedCount = excludedTables.size();
        selectableTablesList.clear();
        for (DbEntity t : tables) {
            if (!excludedTables.containsKey(t.getName())) {
                selectableTablesList.add(t);
            }
        }

        tableModel.fireTableDataChanged();
        tableSelectedAction();
    }

    private boolean isIncluded(DbEntity entity) {
        return !excludedTables.containsKey(entity.getName());
    }

    private void setIncluded(DbEntity entity, boolean b) {
        if (b) {
            excludedTables.remove(entity.getName());
        } else {
            excludedTables.put(entity.getName(), entity);
        }
        tableSelectedAction();
    }

    private String getProblem(DbEntity entity) {
        return validationMessages.get(entity.getName());
    }

    private void tableSelectedAction() {
        int unselectedCount = excludedTables.size() - permanentlyExcludedCount;
        if (unselectedCount == selectableTablesList.size()) {
            checkAll.setSelected(false);
        } else if (unselectedCount == 0) {
            checkAll.setSelected(true);
        }
    }

    private void checkAllClicked(boolean isSelected) {
        if (isSelected) {
            selectableTablesList.clear();
            selectableTablesList.addAll(tables);
            excludedTables.clear();
        } else {
            excludedTables.clear();
            for (DbEntity t : tables) {
                excludedTables.put(t.getName(), t);
            }
            selectableTablesList.clear();
        }

        tableModel.fireTableDataChanged();
    }

    private void initLayout() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        topPanel.add(checkAll);
        topPanel.add(checkAllLabel);

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(50dlu;pref):grow",
                "p, $rgap, fill:40dlu:grow"));
        builder.setDefaultDialogBorder();
        builder.addSeparator("Select Tables", cc.xy(1, 1));
        builder.add(new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), cc.xy(1, 3));

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initBindings() {
        checkAll.addItemListener(e ->
                checkAllLabel.setText(checkAll.isSelected() ? "Uncheck All Tables" : "Check All Tables"));
        checkAll.addActionListener(e -> checkAllClicked(checkAll.isSelected()));
    }

    private class TableSelectorTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return tables != null ? tables.size() : 0;
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
            DbEntity entity = tables.get(row);
            if (col == 0) return isIncluded(entity);
            if (col == 1) return entity.getName();
            return getProblem(entity);
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                setIncluded(tables.get(row), (Boolean) value);
            }
        }
    }
}
