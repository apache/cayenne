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

package org.apache.cayenne.modeler.dialog.validator;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.DisableValidationInspectionAction;
import org.apache.cayenne.modeler.action.ShowValidationOptionAction;
import org.apache.cayenne.modeler.action.ValidateAction;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.util.CayenneDialog;
import org.apache.cayenne.project.validation.Inspection;
import org.apache.cayenne.project.validation.ProjectValidationFailure;
import org.apache.cayenne.validation.ValidationFailure;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.List;

/**
 * Dialog for displaying validation errors.
 */
public class ValidatorDialog extends CayenneDialog {

    protected static ValidatorDialog instance;

    public static final Color WARNING_COLOR = new Color(245, 194, 194);
    public static final Color ERROR_COLOR = new Color(237, 121, 121);

    protected JTable problemsTable;
    protected JButton closeButton;
    protected JButton refreshButton;
    protected List<ValidationFailure> validationObjects;

    public static void showDialog(CayenneModelerFrame frame, List<ValidationFailure> list) {
        if (instance == null) {
            instance = new ValidatorDialog(frame);
            instance.centerWindow();
        }

        instance.refreshFromModel(list);
        instance.setVisible(true);
    }

    public static void showValidationSuccess(CayenneModelerFrame editor) {

        if (instance != null) {
            instance.dispose();
            instance = null;
        }

        JOptionPane.showMessageDialog(editor, "Cayenne project is valid.");
    }

    protected ValidatorDialog(CayenneModelerFrame editor) {
        super(editor, "Validation Problems", false);

        this.validationObjects = Collections.emptyList();

        initView();
        initController();
    }

    private void initView() {

        refreshButton = new JButton("Refresh");
        closeButton = new JButton("Close");

        problemsTable = new JTable();
        problemsTable.setRowHeight(25);
        problemsTable.setRowMargin(3);
        problemsTable.setCellSelectionEnabled(false);
        problemsTable.setRowSelectionAllowed(true);
        problemsTable.setTableHeader(null);
        problemsTable.setDefaultRenderer(ValidationFailure.class, new ValidationRenderer());

        ActionManager actionManager = Application.getInstance().getActionManager();
        JPopupMenu popup = new JPopupMenu();
        popup.add(actionManager.getAction(ShowValidationOptionAction.class).buildMenu());
        popup.add(actionManager.getAction(DisableValidationInspectionAction.class).buildMenu());
        TablePopupHandler.install(problemsTable, popup);

        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout("fill:200dlu:grow", "pref, 3dlu, fill:40dlu:grow"));

        builder.setDefaultDialogBorder();

        builder.addLabel("Click on any row below to go to the object that has a validation problem:", cc.xy(1, 1));
        builder.add(new JScrollPane(problemsTable), cc.xy(1, 3));

        getRootPane().setDefaultButton(refreshButton);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(closeButton);
        buttons.add(refreshButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        // TODO: use preferences
        setSize(450, 350);
    }

    private void initController() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        problemsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        problemsTable.getSelectionModel().addListSelectionListener(e -> showFailedObject());
        problemsTable.getSelectionModel().addListSelectionListener(new ContextMenuSelectionListener());

        closeButton.addActionListener(e -> {
            setVisible(false);
            dispose();
        });

        refreshButton.addActionListener(e -> Application.getInstance().getActionManager()
                .getAction(ValidateAction.class).actionPerformed(e));
    }

    protected void refreshFromModel(List<ValidationFailure> list) {
        validationObjects = list;
        problemsTable.setModel(new ValidatorTableModel());
    }

    private void showFailedObject() {
        if (problemsTable.getSelectedRow() < 0) {
            return;
        }
        ValidationFailure failure = (ValidationFailure) problemsTable.getModel()
                .getValueAt(problemsTable.getSelectedRow(), 0);
        ValidationDisplayHandler.getErrorMsg(failure).displayField(getMediator(), super.getParentEditor());
    }

    class ValidatorTableModel extends AbstractTableModel {

        public int getRowCount() {
            return validationObjects.size();
        }

        public int getColumnCount() {
            return 1;
        }

        public Object getValueAt(int row, int col) {
            return validationObjects.get(row);
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public String getColumnName(int column) {
            return " ";
        }

        public Class<?> getColumnClass(int columnIndex) {
            return ValidationFailure.class;
        }
    }

    // a renderer for the error message
    static class ValidationRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {

            boolean error = false;
            if (value != null) {
                ValidationFailure info = (ValidationFailure) value;
                value = info.getDescription();
                setToolTipText(info.getDescription());
            }

            setBackground(error ? ERROR_COLOR : WARNING_COLOR);
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    class ContextMenuSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int index = problemsTable.getSelectedRow();
            if (e.getValueIsAdjusting() || index < 0) {
                // not valid
                setActionsFor(null);
                return;
            }

            ValidationFailure failure = (ValidationFailure) problemsTable.getModel().getValueAt(index, 0);
            Inspection inspection = failure instanceof ProjectValidationFailure
                    ? ((ProjectValidationFailure) failure).getInspection()
                    : null;
            setActionsFor(inspection);
        }

        private void setActionsFor(Inspection inspection) {
            ActionManager actionManager = Application.getInstance().getActionManager();
            actionManager.getAction(DisableValidationInspectionAction.class)
                    .putInspection(inspection)
                    .setEnabled(inspection != null);
            actionManager.getAction(ShowValidationOptionAction.class)
                    .putInspection(inspection);
        }

    }
}
