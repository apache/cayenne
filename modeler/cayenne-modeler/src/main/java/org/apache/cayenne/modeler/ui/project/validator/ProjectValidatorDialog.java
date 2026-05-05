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

package org.apache.cayenne.modeler.ui.project.validator;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.display.TablePopupHandler;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.toolkit.ProjectDialog;
import org.apache.cayenne.modeler.ui.action.DisableValidationInspectionAction;
import org.apache.cayenne.modeler.ui.action.ShowValidationOptionAction;
import org.apache.cayenne.modeler.ui.action.ValidateAction;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.project.validation.Inspection;
import org.apache.cayenne.project.validation.ProjectValidationFailure;
import org.apache.cayenne.validation.ValidationFailure;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Collections;
import java.util.List;

/**
 * Non-modal dialog displaying project validation problems. Each row is clickable to
 * navigate to the offending model element via {@link ValidationDisplayHandler}.
 */
public class ProjectValidatorDialog extends ProjectDialog {

    public static final Color WARNING_COLOR = new Color(245, 194, 194);

    private final JTable problemsTable;
    private final JButton refreshButton;
    private final JButton closeButton;

    private List<ValidationFailure> validationObjects = Collections.emptyList();

    public ProjectValidatorDialog(ProjectSession session, Window owner) {
        super(session, owner, "Validation Problems", ModalityType.MODELESS);
        this.problemsTable = new JTable();
        this.refreshButton = new JButton("Refresh");
        this.closeButton = new JButton("Close");

        initLayout();
        initBindings();

        // TODO: use preferences
        setSize(450, 350);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        centerOnOwner();
        makeCloseableOnEscape();
    }

    public void showOnFailures(List<ValidationFailure> failures) {
        validationObjects = failures;
        problemsTable.setModel(new ValidatorTableModel());
        setVisible(true);
    }

    public static void showOnSuccess(Application application) {
        JOptionPane.showMessageDialog(application.getFrame(), "Cayenne project is valid.");
    }

    private void initLayout() {
        problemsTable.setRowHeight(25);
        problemsTable.setRowMargin(3);
        problemsTable.setCellSelectionEnabled(false);
        problemsTable.setRowSelectionAllowed(true);
        problemsTable.setTableHeader(null);
        problemsTable.setDefaultRenderer(ValidationFailure.class, new ValidationRenderer());
        problemsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        GlobalActions globalActions = app().getActionManager();
        JPopupMenu popup = new JPopupMenu();
        popup.add(globalActions.getAction(ShowValidationOptionAction.class).buildMenu());
        popup.add(globalActions.getAction(DisableValidationInspectionAction.class).buildMenu());
        TablePopupHandler.install(problemsTable, popup);

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
    }

    private void initBindings() {
        closeButton.addActionListener(e -> {
            setVisible(false);
            dispose();
        });
        refreshButton.addActionListener(e ->
                app().getActionManager().getAction(ValidateAction.class).actionPerformed(e));
        problemsTable.getSelectionModel().addListSelectionListener(e -> fireFailedObjectSelection());
        problemsTable.getSelectionModel().addListSelectionListener(new ContextMenuSelectionListener());
    }

    private void fireFailedObjectSelection() {
        if (problemsTable.getSelectedRow() < 0) {
            return;
        }
        ValidationFailure failure = (ValidationFailure) problemsTable.getModel()
                .getValueAt(problemsTable.getSelectedRow(), 0);
        JFrame frame = app().getFrame();
        ValidationDisplayHandler.getErrorMsg(failure, session()).displayField(session(), frame);
    }

    private void onSelectionChanged(ValidationFailure failure) {
        Inspection inspection = failure instanceof ProjectValidationFailure
                ? ((ProjectValidationFailure) failure).getInspection()
                : null;
        GlobalActions globalActions = app().getActionManager();
        globalActions.getAction(DisableValidationInspectionAction.class)
                .putInspection(inspection)
                .setEnabled(inspection != null);
        globalActions.getAction(ShowValidationOptionAction.class)
                .putInspection(inspection);
    }

    private class ValidatorTableModel extends AbstractTableModel {

        public int getRowCount() {
            return validationObjects.size();
        }

        public int getColumnCount() {
            return 1;
        }

        public Object getValueAt(int row, int col) {
            return validationObjects.get(row);
        }

        public String getColumnName(int column) {
            return " ";
        }

        public Class<?> getColumnClass(int columnIndex) {
            return ValidationFailure.class;
        }
    }

    private static class ValidationRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            if (value != null) {
                ValidationFailure info = (ValidationFailure) value;
                value = info.getDescription();
                setToolTipText(info.getDescription());
            }
            setBackground(WARNING_COLOR);
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private class ContextMenuSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int index = problemsTable.getSelectedRow();
            if (e.getValueIsAdjusting() || index < 0) {
                onSelectionChanged(null);
                return;
            }
            ValidationFailure failure = (ValidationFailure) problemsTable.getModel().getValueAt(index, 0);
            onSelectionChanged(failure);
        }
    }
}
