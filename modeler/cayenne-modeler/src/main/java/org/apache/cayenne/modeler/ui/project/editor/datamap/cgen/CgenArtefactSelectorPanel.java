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
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.toolkit.table.IconCellRenderer;
import org.apache.cayenne.modeler.toolkit.table.TableSizer;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collection;
import java.util.List;

/**
 * Per-class artefact selector for the cgen editor — left-side table where the user toggles
 * which classes participate in code generation, with validation icons in the rightmost column.
 */
public class CgenArtefactSelectorPanel extends JPanel {

    private static final Icon ERROR_ICON = IconFactory.buildIcon("icon-error.png");

    private static final String[] COLUMN_HEADERS = {"", "  Class", ""};
    private static final Class<?>[] COLUMN_CLASSES = {Boolean.class, JLabel.class, String.class};

    private final CgenPanel cgen;
    private final JTable table;
    private final CheckBoxHeader checkBoxHeader;
    private AbstractTableModel tableModel;
    private ValidationResult lastValidationResult;

    public CgenArtefactSelectorPanel(CgenPanel cgen) {
        this.cgen = cgen;
        this.checkBoxHeader = new CheckBoxHeader();
        this.table = new JTable();

        initLayout();
    }

    public void startup() {
        initBindings();
        classSelectedAction();
    }

    public JTable getTable() {
        return table;
    }

    private void initLayout() {
        table.setRowHeight(22);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBackground(UIManager.getColor("Table.selectionBackground"));
        table.getTableHeader().setDefaultRenderer(renderer);

        JScrollPane tablePanel = new JScrollPane(
                table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // set some minimal preferred size, so that it is smaller than other forms used in
        // the dialog... this way we get the right automated overall size
        tablePanel.setPreferredSize(new Dimension(300, 200));

        setLayout(new BorderLayout());
        add(tablePanel, BorderLayout.CENTER);
    }

    void initBindings() {
        checkBoxHeader.addActionListener(e -> checkAllAction());

        tableModel = new AbstractTableModel() {
            public int getRowCount() {
                return cgen.getClasses().size();
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
                if (col == 0) return cgen.isSelected(item);
                if (col == 1) return getItemName(item);
                return getProblem(item);
            }

            public void setValueAt(Object value, int row, int col) {
                if (col == 0) {
                    cgen.setSelected(getItem(row), (Boolean) value);
                    classSelectedAction();
                }
            }

            private Object getItem(int row) {
                return cgen.getClasses().toArray()[row];
            }
        };

        table.setModel(tableModel);

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setHeaderRenderer(checkBoxHeader);
        columnModel.getColumn(1).setCellRenderer(new IconCellRenderer());
        columnModel.getColumn(2).setCellRenderer(new IconCellRenderer());

        TableSizer.sizeColumns(table, Boolean.TRUE, "XXXXXXXXXXXXXXXXXXXXXXXXXX", "XX");
    }

    void classSelectedAction() {
        int selectedCount = cgen.getSelectedEntitiesSize()
                + cgen.getSelectedEmbeddablesSize()
                + (cgen.isDataMapSelected() ? 1 : 0);
        int totalClasses = cgen.getClasses().size();
        checkBoxHeader.setSelected(selectedCount >= totalClasses);
        cgen.updateGenerateButton();
        cgen.updateSelectedEntities();
        cgen.getStandardModeController().updateTemplateEditorButtons();
        repaint();
    }

    private void checkAllAction() {
        if (cgen.updateSelection(checkBoxHeader.isSelected() ? o -> true : o -> false)) {
            tableModel.fireTableDataChanged();
            cgen.updateSelectedEntities();
            cgen.updateGenerateButton();
            cgen.getStandardModeController().updateTemplateEditorButtons();
        }
    }

    public void validate(Collection<? extends ConfigurationNode> classes) {
        CgenValidator validator = new CgenValidator();
        this.lastValidationResult = validator.getValidationResult(classes);
    }

    public JLabel getProblem(Object obj) {
        String name = null;
        if (obj instanceof ObjEntity oe) {
            name = oe.getName();
        } else if (obj instanceof Embeddable emb) {
            name = emb.getClassName();
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
        if (obj instanceof Embeddable emb) {
            className = emb.getClassName();
            icon = IconFactory.iconForObject(new Embeddable());
        } else if (obj instanceof ObjEntity oe) {
            className = oe.getName();
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
