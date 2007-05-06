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

package org.apache.cayenne.modeler.dialog.db;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DerivedDbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.validator.ValidationDisplayHandler;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.project.validator.ValidationInfo;
import org.apache.cayenne.project.validator.Validator;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.swing.TableBindingBuilder;

/**
 * @author Andrus Adamchik
 */
public class TableSelectorController extends CayenneController {

    protected TableSelectorView view;
    protected ObjectBinding tableBinding;

    protected DbEntity table;
    protected List tables;
    protected int permanentlyExcludedCount;
    protected Map excludedTables;
    protected List selectableTablesList;
    protected Map validationMessages;

    public TableSelectorController(ProjectController parent) {
        super(parent);
        this.view = new TableSelectorView();
        this.excludedTables = new HashMap();
        this.selectableTablesList = new ArrayList();
        this.validationMessages = new HashMap();
        initController();
    }

    // ----- properties -----

    public Component getView() {
        return view;
    }

    /**
     * Called by table binding script to set current table.
     */
    public void setTable(DbEntity table) {
        this.table = table;
    }

    /**
     * Returns DbEntities that are excluded from DB generation.
     */
    public Collection getExcludedTables() {
        return excludedTables.values();
    }

    public List getTables() {
        return tables;
    }

    public boolean isIncluded() {
        if (table == null) {
            return false;
        }

        return !excludedTables.containsKey(table.getName());
    }

    public void setIncluded(boolean b) {
        if (table == null) {
            return;
        }

        if (b) {
            excludedTables.remove(table.getName());
        }
        else {
            excludedTables.put(table.getName(), table);
        }
        
        tableSelectedAction();
    }
    
    /**
     * A callback action that updates the state of Select All checkbox.
     */
    public void tableSelectedAction() {
        int unselectedCount = excludedTables.size() - permanentlyExcludedCount;

        if (unselectedCount == selectableTablesList.size()) {
            view.getCheckAll().setSelected(false);
        }
        else if (unselectedCount == 0) {
            view.getCheckAll().setSelected(true);
        }
    }

    public Object getProblem() {
        return (table != null) ? validationMessages.get(table.getName()) : null;
    }

    // ------ other stuff ------

    protected void initController() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getCheckAll(), "checkAllAction()");

        TableBindingBuilder tableBuilder = new TableBindingBuilder(builder);

        tableBuilder.addColumn(
                "",
                "setTable(#item), included",
                Boolean.class,
                true,
                Boolean.TRUE);
        tableBuilder.addColumn(
                "Table",
                "#item.name",
                String.class,
                false,
                "XXXXXXXXXXXXXXXX");
        tableBuilder.addColumn(
                "Problems",
                "setTable(#item), problem",
                String.class,
                false,
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        this.tableBinding = tableBuilder.bindToTable(view.getTables(), "tables");
    }

    /**
     * Performs validation of DbEntities in the current DataMap. Returns a collection of
     * ValidationInfo objects describing the problems.
     */
    public void updateTables(DataMap dataMap) {
        this.tables = new ArrayList(dataMap.getDbEntities());

        excludedTables.clear();
        validationMessages.clear();

        // if there were errors, filter out those related to
        // non-derived DbEntities...

        // TODO: this is inefficient.. we need targeted validation
        // instead of doing it on the whole project

        Validator validator = ((ProjectController) getParent())
                .getProject()
                .getValidator();
        int validationCode = validator.validate();
        if (validationCode >= ValidationDisplayHandler.WARNING) {

            Iterator it = validator.validationResults().iterator();
            while (it.hasNext()) {
                ValidationInfo nextProblem = (ValidationInfo) it.next();
                Entity failedEntity = null;

                if (nextProblem.getValidatedObject() instanceof DbAttribute) {
                    DbAttribute failedAttribute = (DbAttribute) nextProblem
                            .getValidatedObject();
                    failedEntity = failedAttribute.getEntity();
                }
                else if (nextProblem.getValidatedObject() instanceof DbRelationship) {
                    DbRelationship failedRelationship = (DbRelationship) nextProblem
                            .getValidatedObject();
                    failedEntity = failedRelationship.getSourceEntity();
                }
                else if (nextProblem.getValidatedObject() instanceof DbEntity) {
                    failedEntity = (Entity) nextProblem.getValidatedObject();
                }

                if (failedEntity == null) {
                    continue;
                }

                excludedTables.put(failedEntity.getName(), failedEntity);
                validationMessages.put(failedEntity.getName(), nextProblem.getMessage());
            }
        }

        // now do a pass through the tables and exclude derived
        Iterator tablesIt = tables.iterator();
        while (tablesIt.hasNext()) {
            DbEntity table = (DbEntity) tablesIt.next();
            if (table instanceof DerivedDbEntity) {
                excludedTables.put(table.getName(), table);
                validationMessages.put(table.getName(), "derived entity");
            }
        }

        // Find selectable tables
        permanentlyExcludedCount = excludedTables.size();
        selectableTablesList.clear();
        tablesIt = tables.iterator();
        while (tablesIt.hasNext()) {
            DbEntity table = (DbEntity) tablesIt.next();
            
            if (false == excludedTables.containsKey(table.getName())) {
                selectableTablesList.add(table);
            }
        }

        tableBinding.updateView();
        tableSelectedAction();
    }

    public void checkAllAction() {

        boolean isCheckAllSelected = view.getCheckAll().isSelected();
        
        // now do a pass through the selectable tables and reset selected status
        Iterator tablesIt = selectableTablesList.iterator();
        while (tablesIt.hasNext()) {
            table = (DbEntity) tablesIt.next();
            setIncluded(isCheckAllSelected);
        }
        
        tableBinding.updateView();
    }
}
