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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.swing.table.TableSizer;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableSelectorController extends ChildController<ProjectController> {

    private static final String[] COLUMN_HEADERS = {"", "Table", "Problems"};
    private static final Class<?>[] COLUMN_CLASSES = {Boolean.class, String.class, String.class};

    protected TableSelectorView view;
    protected AbstractTableModel tableModel;

    protected List<DbEntity> tables;
    protected int permanentlyExcludedCount;
    protected Map<String, DbEntity> excludedTables;
    protected List<DbEntity> selectableTablesList;
    protected Map<String, String> validationMessages;

    public TableSelectorController(ProjectController parent) {
        super(parent);
        this.view = new TableSelectorView();
        this.excludedTables = new HashMap<>();
        this.selectableTablesList = new ArrayList<>();
        this.validationMessages = new HashMap<>();
        initController();
    }

    public Component getView() {
        return view;
    }

    public Collection<DbEntity> getExcludedTables() {
        return excludedTables.values();
    }

    public List<DbEntity> getTables() {
        return tables;
    }

    public boolean isIncluded(DbEntity entity) {
        return !excludedTables.containsKey(entity.getName());
    }

    public void setIncluded(DbEntity entity, boolean b) {
        if (b) {
            excludedTables.remove(entity.getName());
        } else {
            excludedTables.put(entity.getName(), entity);
        }
        tableSelectedAction();
    }

    public String getProblem(DbEntity entity) {
        return validationMessages.get(entity.getName());
    }

    public void tableSelectedAction() {
        int unselectedCount = excludedTables.size() - permanentlyExcludedCount;
        if (unselectedCount == selectableTablesList.size()) {
            view.getCheckAll().setSelected(false);
        } else if (unselectedCount == 0) {
            view.getCheckAll().setSelected(true);
        }
    }

    protected void initController() {
        view.getCheckAll().addActionListener(e -> checkAllAction());

        tableModel = new AbstractTableModel() {
            public int getRowCount() { return tables != null ? tables.size() : 0; }
            public int getColumnCount() { return COLUMN_HEADERS.length; }
            public String getColumnName(int col) { return COLUMN_HEADERS[col]; }
            public Class<?> getColumnClass(int col) { return COLUMN_CLASSES[col]; }
            public boolean isCellEditable(int row, int col) { return col == 0; }

            public Object getValueAt(int row, int col) {
                DbEntity entity = tables.get(row);
                if (col == 0) return isIncluded(entity);
                if (col == 1) return entity.getName();
                return getProblem(entity);
            }

            public void setValueAt(Object value, int row, int col) {
                if (col == 0) setIncluded(tables.get(row), (Boolean) value);
            }
        };

        view.getTables().setModel(tableModel);
        TableSizer.sizeColumns(view.getTables(), Boolean.TRUE, "XXXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    }

    public void updateTables(Collection<DataMap> dataMaps) {
        this.tables = new ArrayList<>();
        for (DataMap dataMap : dataMaps) {
            this.tables.addAll(dataMap.getDbEntities());
        }

        excludedTables.clear();
        validationMessages.clear();

        Project project = getApplication().getProject();
        ProjectValidator projectValidator = getApplication().getProjectValidator();
        ValidationResult validationResult = projectValidator.validate(project.getRootNode());

        if (validationResult.getFailures().size() > 0) {
            for (ValidationFailure nextProblem : validationResult.getFailures()) {
                DbEntity failedEntity = null;

                if (nextProblem.getSource() instanceof DbAttribute) {
                    failedEntity = ((DbAttribute) nextProblem.getSource()).getEntity();
                } else if (nextProblem.getSource() instanceof DbRelationship) {
                    failedEntity = ((DbRelationship) nextProblem.getSource()).getSourceEntity();
                } else if (nextProblem.getSource() instanceof DbEntity) {
                    failedEntity = (DbEntity) nextProblem.getSource();
                }

                if (failedEntity == null) {
                    continue;
                }

                excludedTables.put(failedEntity.getName(), failedEntity);
                validationMessages.put(failedEntity.getName(), nextProblem.getDescription());
            }
        }

        permanentlyExcludedCount = excludedTables.size();
        selectableTablesList.clear();
        for (DbEntity table : tables) {
            if (!excludedTables.containsKey(table.getName())) {
                selectableTablesList.add(table);
            }
        }

        tableModel.fireTableDataChanged();
        tableSelectedAction();
    }

    public void checkAllAction() {
        boolean isCheckAllSelected = view.getCheckAll().isSelected();

        if (isCheckAllSelected) {
            selectableTablesList.clear();
            selectableTablesList.addAll(tables);
            excludedTables.clear();
        } else {
            excludedTables.clear();
            for (DbEntity table : tables) {
                excludedTables.put(table.getName(), table);
            }
            selectableTablesList.clear();
        }

        tableModel.fireTableDataChanged();
    }

}
