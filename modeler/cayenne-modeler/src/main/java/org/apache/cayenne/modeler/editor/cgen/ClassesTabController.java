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

package org.apache.cayenne.modeler.editor.cgen;

import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ImageRendererColumn;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.swing.TableBindingBuilder;

import javax.swing.JLabel;
import javax.swing.table.TableColumnModel;
import java.awt.Component;

/**
 * @since 4.1
 */
public class ClassesTabController extends CayenneController {

    protected ClassesTabPanel view;
    protected ObjectBinding tableBinding;

    private BindingBuilder builder;

    public ClassesTabController(CodeGeneratorController parent) {
        super(parent);

        this.view = new ClassesTabPanel();
        this.builder = new BindingBuilder(getApplication().getBindingFactory(), this);
    }

    public void startup(){
        initBindings();
        classSelectedAction();
    }

    protected CodeGeneratorController getParentController() {
        return (CodeGeneratorController) getParent();
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        builder.bindToAction(((CodeGeneratorPane)parent.getView()).getCheckAll(), "checkAllAction()");

        TableBindingBuilder tableBuilder = new TableBindingBuilder(builder);

        tableBuilder.addColumn(
                "",
                "parent.setCurrentClass(#item), selected",
                Boolean.class,
                true,
                Boolean.TRUE);
        tableBuilder.addColumn(
                "Class",
                "parent.getItemName(#item)",
                JLabel.class,
                false,
                "XXXXXXXXXXXXXXXXXXXXXXXXXX");

        tableBuilder.addColumn(
                "",
                "parent.getProblem(#item)",
                String.class,
                false,
                "XX");

        this.tableBinding = tableBuilder.bindToTable(view.getTable(), "parent.classes");
        TableColumnModel columnModel = view.getTable().getColumnModel();
        columnModel.getColumn(1).setCellRenderer(new ImageRendererColumn());
        columnModel.getColumn(2).setCellRenderer(new ImageRendererColumn());
    }

    public boolean isSelected() {
        return getParentController().isSelected();
    }

    public void setSelected(boolean selected) {
        getParentController().setSelected(selected);
        classSelectedAction();
    }

    /**
     * A callback action that updates the state of Select All checkbox.
     */
    public void classSelectedAction() {
        int selectedCount = getParentController().getSelectedEntitiesSize()
                + getParentController().getSelectedEmbeddablesSize()
                + (getParentController().isDataMapSelected() ? 1 : 0);
        int totalClasses = getParentController().getClasses().size();

        getParentController().enableGenerateButton(selectedCount != 0);
        getParentController().getView().getCheckAll().setSelected(selectedCount >= totalClasses);
        getParentController().updateSelectedEntities();
    }

    /**
     * An action that updates entity check boxes in response to the Select All state
     * change.
     */
    @SuppressWarnings("unused")
    public void checkAllAction() {
        if (getParentController().updateSelection(((CodeGeneratorPane)parent.getView()).getCheckAll().isSelected() ? o -> true : o -> false)) {
            tableBinding.updateView();
            getParentController().updateSelectedEntities();
            if(((CodeGeneratorPane)parent.getView()).getCheckAll().isSelected()) {
                getParentController().enableGenerateButton(true);
            } else {
                getParentController().enableGenerateButton(false);
            }
        }
    }
}
