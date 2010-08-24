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
package org.apache.cayenne.modeler.dialog.autorelationship;

import java.awt.Component;

import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.swing.TableBindingBuilder;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;

public class InferRelationshipsTabController extends CayenneController {

    public static final String GENERATE_PROPERTY = "generate";

    protected InferRelationshipsPanel view;
    protected ObjectBinding tableBinding;

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
                "Source",
                "#item.getSource().getName()",
                String.class,
                false,
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        tableBuilder.addColumn(
                "Target",
                "#item.getTarget().getName()",
                String.class,
                false,
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        tableBuilder.addColumn(
                "Join",
                "parent.getJoin(#item)",
                String.class,
                false,
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        tableBuilder.addColumn(
                "Name",
                "#item.getName()",
                String.class,
                false,
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        tableBuilder.addColumn(
                "To Many",
                "parent.getToMany(#item)",
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
