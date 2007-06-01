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
package org.objectstyle.cayenne.modeler.dialog.db;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.objectstyle.cayenne.access.DbLoader;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.util.CayenneDialog;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog for selecting database reverse-engineering parameters.
 */
public class DbLoaderOptionsDialog extends CayenneDialog {

    public static final int CANCEL = 0;
    public static final int SELECT = 1;

    protected JLabel schemaLabel;
    protected JComboBox schemaSelector;
    protected JTextField tableNamePatternField;
    protected JCheckBox loadProcedures;
    protected JTextField procNamePatternField;
    protected JLabel procedureLabel;
    protected JButton selectButton;
    protected JButton cancelButton;
    protected int choice;

    /**
     * Creates and initializes new ChooseSchemaDialog.
     */
    public DbLoaderOptionsDialog(Collection schemas, String dbUserName,
            boolean loadProcedures) {
        super(Application.getFrame(), "Reengineer DB Schema: Select Options");

        init();
        initController();
        initFromModel(schemas, dbUserName, loadProcedures);

        pack();
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setModal(true);
        centerWindow();
    }

    /** Sets up the graphical components. */
    protected void init() {

        // create widgets...
        selectButton = new JButton("Continue");
        cancelButton = new JButton("Cancel");
        schemaSelector = new JComboBox();
        tableNamePatternField = new JTextField();
        procNamePatternField = new JTextField();
        loadProcedures = new JCheckBox();

        // assemble
        FormLayout layout = new FormLayout(
                "right:pref, 3dlu, fill:max(170dlu;pref):grow",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        schemaLabel = builder.append("Select Schema:", schemaSelector);
        builder.append("Table Name Pattern:", tableNamePatternField);
        builder.append("Load Procedures:", loadProcedures);
        procedureLabel = builder.append("Procedure Name Pattern:", procNamePatternField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(selectButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    protected void initController() {
        selectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                processSelect();
            }
        });

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                processCancel();
            }
        });

        loadProcedures.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                procNamePatternField.setEnabled(loadProcedures.isSelected());
                procedureLabel.setEnabled(loadProcedures.isSelected());
            }
        });
    }

    protected void initFromModel(
            Collection schemas,
            String dbUserName,
            boolean shouldLoadProcedures) {

        this.choice = CANCEL;
        this.tableNamePatternField.setText(DbLoader.WILDCARD);
        this.loadProcedures.setSelected(shouldLoadProcedures);
        this.procNamePatternField.setText(DbLoader.WILDCARD);
        this.procNamePatternField.setEnabled(shouldLoadProcedures);
        this.procedureLabel.setEnabled(shouldLoadProcedures);

        boolean showSchemaSelector = schemas != null && !schemas.isEmpty();
        schemaSelector.setVisible(showSchemaSelector);
        schemaLabel.setVisible(showSchemaSelector);

        if (showSchemaSelector) {

            schemaSelector.setModel(new DefaultComboBoxModel(schemas.toArray()));

            // select schema belonging to the user
            if (dbUserName != null) {
                Iterator it = schemas.iterator();
                while (it.hasNext()) {
                    String schema = (String) it.next();
                    if (dbUserName.equalsIgnoreCase(schema)) {
                        schemaSelector.setSelectedItem(schema);
                        break;
                    }
                }
            }
        }
    }

    public int getChoice() {
        return choice;
    }

    private void processSelect() {
        choice = SELECT;
        hide();
    }

    private void processCancel() {
        choice = CANCEL;
        hide();
    }

    /**
     * Returns selected schema.
     */
    public String getSelectedSchema() {
        String schema = (String) schemaSelector.getSelectedItem();
        return "".equals(schema) ? null : schema;
    }

    /**
     * Returns the tableNamePattern.
     */
    public String getTableNamePattern() {
        return "".equals(tableNamePatternField.getText()) ? null : tableNamePatternField
                .getText();
    }

    public boolean isLoadingProcedures() {
        return loadProcedures.isSelected();
    }

    /**
     * Returns the procedure name pattern.
     */
    public String getProcedureNamePattern() {
        return "".equals(procNamePatternField.getText()) ? null : procNamePatternField
                .getText();
    }
}