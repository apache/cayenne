/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.dialog.validator;

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

import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.action.ValidateAction;
import org.objectstyle.cayenne.modeler.util.CayenneDialog;
import org.objectstyle.cayenne.project.validator.ValidationInfo;
import org.objectstyle.cayenne.project.validator.Validator;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog for displaying validation errors.
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
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
                "pref, 3dlu, top:40dlu:grow"));

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
