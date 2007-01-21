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

package org.apache.cayenne.modeler.dialog.db;

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

import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.CayenneDialog;

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
        setVisible(false);
    }

    private void processCancel() {
        choice = CANCEL;
        setVisible(false);
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
