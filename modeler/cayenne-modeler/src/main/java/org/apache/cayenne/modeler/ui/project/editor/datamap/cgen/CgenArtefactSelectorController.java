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

package org.apache.cayenne.modeler.ui.project.editor.datamap.cgen;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.toolkit.table.IconCellRenderer;
import org.apache.cayenne.modeler.toolkit.table.TableSizer;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Collection;
import java.util.List;

public class CgenArtefactSelectorController extends ChildController<CgenController> {

    private static final Icon ERROR_ICON = IconFactory.buildIcon("icon-error.png");

    private static final String[] COLUMN_HEADERS = {"", "  Class", ""};
    private static final Class<?>[] COLUMN_CLASSES = {Boolean.class, JLabel.class, String.class};

    protected CgenArtefactSelectorPanel view;
    protected AbstractTableModel tableModel;
    private ValidationResult lastValidationResult;
    private final CheckBoxHeader checkBoxHeader;

    public CgenArtefactSelectorController(CgenController parent) {
        super(parent);
        this.checkBoxHeader = new CheckBoxHeader();
        this.view = new CgenArtefactSelectorPanel();
    }

    public void startup() {
        initBindings();
        classSelectedAction();
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        checkBoxHeader.addActionListener(e -> checkAllAction());

        tableModel = new AbstractTableModel() {
            public int getRowCount() {
                return parent.getClasses().size();
            }

            public int getColumnCount() {
                return COLUMN_HEADERS.length;
            }

            public String getColumnName(int col) {
                return COLUMN_HEADERS[col];
            }

            public Class<?> getColumnClass(int col) {
                return COLUMN_CLASSES[col];
            }

            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }

            public Object getValueAt(int row, int col) {
                Object item = getItem(row);
                if (col == 0) return parent.isSelected(item);
                if (col == 1) return getItemName(item);
                return getProblem(item);
            }

            public void setValueAt(Object value, int row, int col) {
                if (col == 0) {
                    parent.setSelected(getItem(row), (Boolean) value);
                    classSelectedAction();
                }
            }

            private Object getItem(int row) {
                return parent.getClasses().toArray()[row];
            }
        };

        view.getTable().setModel(tableModel);

        TableColumnModel columnModel = view.getTable().getColumnModel();
        columnModel.getColumn(0).setHeaderRenderer(checkBoxHeader);
        columnModel.getColumn(1).setCellRenderer(new IconCellRenderer());
        columnModel.getColumn(2).setCellRenderer(new IconCellRenderer());

        TableSizer.sizeColumns(view.getTable(), Boolean.TRUE, "XXXXXXXXXXXXXXXXXXXXXXXXXX", "XX");
    }

    /**
     * A callback action that updates the state of Select All checkbox.
     */
    public void classSelectedAction() {
        int selectedCount = parent.getSelectedEntitiesSize()
                + parent.getSelectedEmbeddablesSize()
                + (parent.isDataMapSelected() ? 1 : 0);
        int totalClasses = parent.getClasses().size();
        checkBoxHeader.setSelected(selectedCount >= totalClasses);
        parent.updateGenerateButton();
        parent.updateSelectedEntities();
        parent.getStandardModeController().updateTemplateEditorButtons();
        view.repaint();
    }

    public void checkAllAction() {
        if (parent.updateSelection(checkBoxHeader.isSelected() ? o -> true : o -> false)) {
            tableModel.fireTableDataChanged();
            parent.updateSelectedEntities();
            parent.updateGenerateButton();
            parent.getStandardModeController().updateTemplateEditorButtons();
        }
    }

    public void validate(Collection<? extends ConfigurationNode> classes) {
        CgenValidator validator = new CgenValidator();
        this.lastValidationResult = validator.getValidationResult(classes);
    }

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
        if (validationFailure != null) {
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
            icon = IconFactory.iconForObject(new Embeddable());
        } else if (obj instanceof ObjEntity) {
            className = ((ObjEntity) obj).getName();
            icon = IconFactory.iconForObject(new ObjEntity());
        } else {
            className = ((DataMap) obj).getName();
            icon = IconFactory.iconForObject(new DataMap());
        }
        JLabel labelIcon = new JLabel();
        labelIcon.setIcon(icon);
        labelIcon.setVisible(true);
        labelIcon.setText(className);
        return labelIcon;
    }

}
