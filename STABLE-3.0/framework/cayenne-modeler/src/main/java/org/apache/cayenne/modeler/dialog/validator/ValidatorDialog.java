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

package org.apache.cayenne.modeler.dialog.validator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.action.ValidateAction;
import org.apache.cayenne.modeler.util.CayenneDialog;
import org.apache.cayenne.project.validator.ValidationInfo;
import org.apache.cayenne.project.validator.Validator;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog for displaying validation errors.
 * 
 */
public class ValidatorDialog extends CayenneDialog {

    protected static ValidatorDialog instance;

    public static final Color WARNING_COLOR = new Color(245, 194, 194);
    public static final Color ERROR_COLOR = new Color(237, 121, 121);

    protected JTable problemsTable;
    protected JButton closeButton;
    protected JButton refreshButton;
    protected List validationObjects;

    public static synchronized void showDialog(
            CayenneModelerFrame editor,
            Validator validator) {

        if (instance == null) {
            instance = new ValidatorDialog(editor);
            instance.centerWindow();
        }

        instance.refreshFromModel(validator);
        instance.setVisible(true);
    }

    public static synchronized void showValidationSuccess(
            CayenneModelerFrame editor,
            Validator val) {

        if (instance != null) {
            instance.dispose();
            instance = null;
        }

        JOptionPane
                .showMessageDialog(Application.getFrame(), "Cayenne project is valid.");
    }

    protected ValidatorDialog(CayenneModelerFrame editor) {
        super(editor, "Validation Problems", false);

        this.validationObjects = Collections.EMPTY_LIST;

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
        problemsTable.setDefaultRenderer(ValidationInfo.class, new ValidationRenderer());

        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:200dlu:grow",
                "pref, 3dlu, fill:40dlu:grow"));

        builder.setDefaultDialogBorder();

        builder
                .addLabel(
                        "Click on any row below to go to the object that has a validation problem:",
                        cc.xy(1, 1));
        builder.add(new JScrollPane(problemsTable), cc.xy(1, 3));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(refreshButton);
        buttons.add(closeButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        // TODO: use preferences
        setSize(450, 350);
    }

    private void initController() {

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        problemsTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {

                    public void valueChanged(ListSelectionEvent e) {
                        showFailedObject();
                    }
                });

        closeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });

        refreshButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Application
                        .getInstance()
                        .getAction(ValidateAction.getActionName())
                        .actionPerformed(e);
            }
        });

        this.problemsTable.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                int row = problemsTable.rowAtPoint(e.getPoint());

                // if this happens to be a selected row, re-run object selection
                if (row >= 0 && problemsTable.getSelectedRow() == row) {
                    showFailedObject();
                }
            }
        });
    }

    protected void refreshFromModel(Validator validator) {
        validationObjects = validator.validationResults();
        problemsTable.setModel(new ValidatorTableModel());
    }

    private void showFailedObject() {
        if (problemsTable.getSelectedRow() >= 0) {
            ValidationInfo obj = (ValidationInfo) problemsTable.getModel().getValueAt(
                    problemsTable.getSelectedRow(),
                    0);
            ValidationDisplayHandler.getErrorMsg(obj).displayField(
                    getMediator(),
                    super.getParentEditor());
        }
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

        public Class getColumnClass(int columnIndex) {
            return ValidationInfo.class;
        }
    }

    // a renderer for the error message
    class ValidationRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            boolean error = false;
            if (value != null) {
                ValidationInfo info = (ValidationInfo) value;
                error = info.getSeverity() == ValidationInfo.ERROR;
                value = (error) ? "Error: " + info.getMessage() : "Warning: "
                        + info.getMessage();
            }

            setBackground(error ? ERROR_COLOR : WARNING_COLOR);
            return super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);
        }
    }
}
