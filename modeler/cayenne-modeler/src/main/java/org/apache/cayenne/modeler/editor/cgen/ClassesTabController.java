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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ImageRendererColumn;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.swing.TableBindingBuilder;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.util.List;

/**
 * @since 4.1
 */
public class ClassesTabController extends CayenneController {

    private static final Icon ERROR_ICON = ModelerUtil.buildIcon("icon-error.png");

    protected ClassesTabPanel view;
    protected ObjectBinding tableBinding;

    private ValidationResult lastValidationResult;
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
        builder.bindToAction(getParentController().getView().getCheckAll(), "checkAllAction()");

        TableBindingBuilder tableBuilder = new TableBindingBuilder(builder);

        tableBuilder.addColumn(
                "",
                "parent.setCurrentClass(#item), selected",
                Boolean.class,
                true,
                Boolean.TRUE);

        tableBuilder.addColumn(
                "Class",
                "getItemName(#item)",
                JLabel.class,
                false,
                "XXXXXXXXXXXXXXXXXXXXXXXXXX");

        tableBuilder.addColumn(
                "",
                "getProblem(#item)",
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
        if (getParentController().updateSelection(getParentController().getView().getCheckAll().isSelected() ? o -> true : o -> false)) {
            tableBinding.updateView();
            getParentController().updateSelectedEntities();
            if(getParentController().getView().getCheckAll().isSelected()) {
                getParentController().enableGenerateButton(true);
            } else {
                getParentController().enableGenerateButton(false);
            }
        }
    }

    public void validate(List<Object> classes) {
        ClassGenerationValidator validator = new ClassGenerationValidator();
        this.lastValidationResult = validator.getValidationResult(classes);
    }

    /**
     * Returns the first encountered validation problem for an antity matching the name or
     * null if the entity is valid or the entity is not present.
     */
    public JLabel getProblem(Object obj) {
        String name = null;
        if (obj instanceof ObjEntity) {
            name = ((ObjEntity) obj).getName();
        } else if (obj instanceof Embeddable) {
            name = ((Embeddable) obj).getClassName();
        }

        ValidationFailure validationFailure = null;
        if (lastValidationResult != null) {
            List<ValidationFailure> failures = lastValidationResult.getFailures(name);
            if (!failures.isEmpty()) {
                validationFailure = failures.get(0);
            }
        }

        JLabel labelIcon = new JLabel();
        labelIcon.setVisible(true);
        if(validationFailure != null) {
            labelIcon.setIcon(ERROR_ICON);
            labelIcon.setToolTipText(validationFailure.getDescription());
        }
        return labelIcon;
    }

    public JLabel getItemName(Object obj) {
        String className;
        Icon icon;
        if (obj instanceof Embeddable) {
            className = ((Embeddable) obj).getClassName();
            icon = CellRenderers.iconForObject(new Embeddable());
        } else if(obj instanceof ObjEntity) {
            className = ((ObjEntity) obj).getName();
            icon = CellRenderers.iconForObject(new ObjEntity());
        } else {
            className = ((DataMap) obj).getName();
            icon = CellRenderers.iconForObject(new DataMap());
        }
        JLabel labelIcon = new JLabel();
        labelIcon.setIcon(icon);
        labelIcon.setVisible(true);
        labelIcon.setText(className);
        return labelIcon;
    }

}
