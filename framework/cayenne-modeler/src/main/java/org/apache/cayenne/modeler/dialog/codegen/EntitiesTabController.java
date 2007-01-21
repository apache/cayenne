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

package org.apache.cayenne.modeler.dialog.codegen;

import java.awt.Component;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.swing.TableBindingBuilder;

public class EntitiesTabController extends CayenneController {

    public static final String GENERATE_PROPERTY = "generate";

    protected EntitiesTabPanel view;
    protected ObjectBinding tableBinding;

    public EntitiesTabController(CodeGeneratorControllerBase parent) {
        super(parent);

        this.view = new EntitiesTabPanel();
        initBindings();
    }

    protected CodeGeneratorControllerBase getParentController() {
        return (CodeGeneratorControllerBase) getParent();
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {

        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getCheckAll(), "checkAllAction()");

        TableBindingBuilder tableBuilder = new TableBindingBuilder(builder);

        tableBuilder.addColumn(
                "",
                "parent.setCurrentEntity(#item), selected",
                Boolean.class,
                true,
                Boolean.TRUE);
        tableBuilder.addColumn(
                "Entity",
                "#item.name",
                String.class,
                false,
                "XXXXXXXXXXXXXX");

        tableBuilder.addColumn(
                "Comments, Warnings",
                "parent.getProblem(#item.name)",
                String.class,
                false,
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        this.tableBinding = tableBuilder.bindToTable(view.getTable(), "parent.entities");
    }

    public boolean isSelected() {
        return getParentController().isSelected();
    }

    public void setSelected(boolean selected) {
        getParentController().setSelected(selected);
        entitySelectedAction();
    }

    /**
     * A callback action that updates the state of Select All checkbox.
     */
    public void entitySelectedAction() {
        int selectedCount = getParentController().getSelectedEntitiesSize();

        if (selectedCount == 0) {
            view.getCheckAll().setSelected(false);
        }
        else if (selectedCount == getParentController().getEntities().size()) {
            view.getCheckAll().setSelected(true);
        }
    }

    /**
     * An action that updates entity check boxes in response to the Select All state
     * change.
     */
    public void checkAllAction() {

        Predicate predicate = view.getCheckAll().isSelected() ? PredicateUtils
                .truePredicate() : PredicateUtils.falsePredicate();

        if (getParentController().updateSelection(predicate)) {
            tableBinding.updateView();
        }
    }
}
