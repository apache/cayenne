/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.action.ValidateAction;
import org.objectstyle.cayenne.modeler.util.CayenneDialog;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.PanelFactory;
import org.objectstyle.cayenne.project.validator.ValidationInfo;
import org.objectstyle.cayenne.project.validator.Validator;

/** 
 * Dialog for displaying validation errors.
 * 
 * @author Michael Misha Shengaout
 * @author Andrei Adamchik
 */
public class ValidatorDialog extends CayenneDialog implements ActionListener {

    protected static ValidatorDialog instance;

    public static final Color WARNING_COLOR = new Color(245, 194, 194);
    public static final Color ERROR_COLOR = new Color(237, 121, 121);

    protected ProjectController mediator;
    protected Validator validator;
    protected JTable messages;
    protected JButton closeBtn;

    public static synchronized void showDialog(
        CayenneModelerFrame editor,
        ProjectController mediator,
        Validator val) {

        closeValidationDialog();
        instance = new ValidatorDialog(editor, mediator, val);
    }

    public static synchronized void showDialog(
        CayenneModelerFrame editor,
        ProjectController mediator,
        Validator val,
        String message) {

        closeValidationDialog();
        instance = new ValidatorDialog(editor, mediator, val, message);
    }

    public static synchronized void showValidationSuccess(
        CayenneModelerFrame editor,
        ProjectController mediator,
        Validator val) {
        closeValidationDialog();
        JOptionPane.showMessageDialog(
            Application.getFrame(),
            "Project passed validation successfully.");
    }

    protected static synchronized void closeValidationDialog() {
        if (instance != null) {
            instance.dispose();
        }
        instance = null;
    }

    protected ValidatorDialog(
        CayenneModelerFrame editor,
        ProjectController mediator,
        Validator validator) {
        this(editor, mediator, validator, "Validation Problems");
    }

    protected ValidatorDialog(
        CayenneModelerFrame editor,
        ProjectController mediator,
        Validator validator,
        String warning) {
        super(editor, warning, false);
        this.mediator = mediator;
        this.validator = validator;

        init();

        this
            .messages
            .getSelectionModel()
            .addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                showFailedObject();
            }
        });

        this.closeBtn.addActionListener(this);

        // this even handler is needed to show failed object
        // when the user clicks on an already selected row
        this.messages.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = messages.rowAtPoint(e.getPoint());

                // if this happens to be a selected row, re-run object selection
                if (row >= 0 && messages.getSelectedRow() == row) {
                    showFailedObject();
                }
            }
        });

        this.pack();
        this.centerWindow();
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setVisible(true);
    }

    protected void init() {
        getContentPane().setLayout(new BorderLayout());

        JLabel description =
            CayenneWidgetFactory.createLabel(
                "Click on any row below to go to the object that has a validation problem.");
        description.setFont(description.getFont().deriveFont(10));
        description.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        getContentPane().add(description, BorderLayout.NORTH);

        ValidatorTableModel model =
            new ValidatorTableModel(validator.validationResults());
        messages = new ValidatorTable(model);
        messages.setRowHeight(25);
        messages.setRowMargin(3);
        messages.setCellSelectionEnabled(false);
        messages.setRowSelectionAllowed(true);
        messages.getColumnModel().getColumn(0).setPreferredWidth(100);
        messages.getColumnModel().getColumn(1).setPreferredWidth(400);

        JButton revalidateBtn =
            new JButton(
                Application.getFrame().getAction(ValidateAction.getActionName()));
        revalidateBtn.setText("Refresh");
        closeBtn = new JButton("Close");
        JPanel panel =
            PanelFactory.createTablePanel(
                messages,
                new JButton[] { revalidateBtn, closeBtn });
        getContentPane().add(panel, BorderLayout.CENTER);
    }

    protected void showFailedObject() {
        if (messages.getSelectedRow() >= 0) {
            ValidatorTableModel model = (ValidatorTableModel) messages.getModel();
            ValidationInfo obj = model.getValue(messages.getSelectedRow());
            ValidationDisplayHandler.getErrorMsg(obj).displayField(
                mediator,
                super.getParentEditor());
        }
    }

    public void actionPerformed(ActionEvent e) {
        this.setVisible(false);
        this.dispose();
    }

    class ValidatorTable extends JTable {
        protected final Dimension preferredSize = new Dimension(500, 300);

        protected CellRenderer errorRenderer;
        protected CellRenderer errorMsgRenderer;

        protected CellRenderer warnRenderer;
        protected CellRenderer warnMsgRenderer;

        public ValidatorTable(TableModel model) {
            super(model);

            errorRenderer = new CellRenderer(ERROR_COLOR, true);
            errorMsgRenderer = new CellRenderer(ERROR_COLOR, false);
            warnRenderer = new CellRenderer(WARNING_COLOR, true);
            warnMsgRenderer = new CellRenderer(WARNING_COLOR, false);
        }

        /**
         * @see javax.swing.JTable#getCellRenderer(int, int)
         */
        public TableCellRenderer getCellRenderer(int row, int column) {
            if (row < 0 || row >= validator.validationResults().size()) {
                return super.getCellRenderer(row, column);
            }

            ValidationInfo rowObj =
                (ValidationInfo) validator.validationResults().get(row);
            return (rowObj.getSeverity() == ValidationInfo.ERROR)
                ? ((column == 0) ? errorRenderer : errorMsgRenderer)
                : ((column == 0) ? warnRenderer : warnMsgRenderer);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return preferredSize;
        }

    }

    class ValidatorTableModel extends AbstractTableModel {
        List validationObjects;

        public ValidatorTableModel(List validationObjects) {
            this.validationObjects = validationObjects;
        }

        public String getColumnName(int col) {
            if (col == 0)
                return "Severity";
            else if (col == 1)
                return "Error Message";
            else
                return "";
        }

        public int getRowCount() {
            return validationObjects.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int row, int col) {
            ValidationInfo msg = (ValidationInfo) validationObjects.get(row);
            if (col == 0) {
                if (msg.getSeverity() == ValidationDisplayHandler.ERROR)
                    return "ERROR";
                else
                    return "WARNING";
            } else if (col == 1) {
                return msg.getMessage();
            } else
                return "";
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public ValidationInfo getValue(int row) {
            return (ValidationInfo) validationObjects.get(row);
        }
    }

    public class CellRenderer extends DefaultTableCellRenderer {
        protected Font rendererFont;

        public CellRenderer(Color bg, boolean bold) {
            if (bg != null) {
                setBackground(bg);
            }

            if (bold && getFont() != null) {
                rendererFont = getFont().deriveFont(Font.BOLD);
            }
        }

        /**
         * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)
         */
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
            Component comp =
                super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            if (rendererFont != null) {
                comp.setFont(rendererFont);
            }

            return comp;
        }

        /**
         * Returns the rendererFont.
         * @return Font
         */
        public Font getRendererFont() {
            return rendererFont;
        }

        /**
         * Sets the rendererFont.
         * @param rendererFont The rendererFont to set
         */
        public void setRendererFont(Font rendererFont) {
            this.rendererFont = rendererFont;
        }
    }
}
