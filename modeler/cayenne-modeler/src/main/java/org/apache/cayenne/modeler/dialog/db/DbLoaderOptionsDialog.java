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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.CayenneDialog;
import org.apache.cayenne.modeler.util.NameGeneratorPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Vector;

/**
 * Dialog for selecting database reverse-engineering parameters.
 */
public class DbLoaderOptionsDialog extends CayenneDialog {

    public static final String WILDCARD_PATTERN = ".*";

    public static final int CANCEL = 0;
    public static final int SELECT = 1;

    protected JLabel catalogLabel;
    protected JComboBox catalogSelector;
    protected JLabel schemaLabel;
    protected JComboBox schemaSelector;
    protected JTextField tableNamePatternField;
    protected JTextField meaningfulPk;
    protected JTextField procNamePatternField;
    protected JLabel procedureLabel;
    protected JButton selectButton;
    protected JButton cancelButton;


    protected JComboBox strategyCombo;
    protected String strategy;
    protected int choice;

    /**
     * Creates and initializes new ChooseSchemaDialog.
     */
    public DbLoaderOptionsDialog(Collection<String> schemas, Collection<String> catalogs, String currentSchema,
                                 String dbCatalog) {
        super(Application.getFrame(), "Reengineer DB Schema: Select Options");

        init();
        initController();
        initFromModel(schemas, catalogs, currentSchema, dbCatalog);

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
        catalogSelector = new JComboBox();
        schemaSelector = new JComboBox();
        tableNamePatternField = new JTextField();
        procNamePatternField = new JTextField();
        meaningfulPk = new JTextField();
        strategyCombo = new JComboBox();
        strategyCombo.setEditable(true);

        // assemble
        FormLayout layout = new FormLayout(
                "right:pref, 3dlu, fill:max(170dlu;pref):grow",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        catalogLabel = builder.append("Select Catalog:", catalogSelector);
        schemaLabel = builder.append("Select Schema:", schemaSelector);
        builder.append("Table Name Pattern:", tableNamePatternField);
        procedureLabel = builder.append("Procedure Name Pattern:", procNamePatternField);
        builder.append("Naming Strategy:", strategyCombo);
        builder.append("Tables with Meaningful PK Pattern:", meaningfulPk);

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
    }

    protected void initFromModel(
            Collection<String> schemas,
            Collection<String> catalogs,
            String currentSchema,
            String currentCatalog) {

        this.choice = CANCEL;
        this.tableNamePatternField.setText(WILDCARD_PATTERN);
        this.procNamePatternField.setText(WILDCARD_PATTERN);

        Vector<String> arr = NameGeneratorPreferences
                .getInstance()
                .getLastUsedStrategies();
        strategyCombo.setModel(new DefaultComboBoxModel(arr));

        boolean showSchemaSelector = schemas != null && !schemas.isEmpty();
        schemaSelector.setVisible(showSchemaSelector);
        schemaLabel.setVisible(showSchemaSelector);

        if (showSchemaSelector) {

            schemaSelector.setModel(new DefaultComboBoxModel(schemas.toArray()));

            if (currentSchema != null) {
                for (String schema : schemas) {
                    if (currentSchema.equalsIgnoreCase(schema)) {
                        schemaSelector.setSelectedItem(schema);
                        break;
                    }
                }
            }
        }

        boolean showCatalogSelector = catalogs != null && !catalogs.isEmpty();
        catalogSelector.setVisible(showCatalogSelector);
        catalogLabel.setVisible(showCatalogSelector);

        if (showCatalogSelector) {
            catalogSelector.setModel(new DefaultComboBoxModel(catalogs.toArray()));

            if (currentCatalog != null && !currentCatalog.isEmpty()) {
                for (String catalog : catalogs) {
                    if (currentCatalog.equalsIgnoreCase(catalog)) {
                        catalogSelector.setSelectedItem(catalog);
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
        strategy = (String) strategyCombo.getSelectedItem();

        choice = SELECT;
        setVisible(false);
    }

    private void processCancel() {
        choice = CANCEL;
        setVisible(false);
    }

    /**
     * Returns selected catalog.
     */
    public String getSelectedCatalog() {
        String catalog = (String) catalogSelector.getSelectedItem();
        return "".equals(catalog) ? null : catalog;
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

    public String getMeaningfulPk() {
        return "".equals(meaningfulPk.getText()) ? null : meaningfulPk
                .getText();
    }

    /**
     * Returns the procedure name pattern.
     */
    public String getProcedureNamePattern() {
        return "".equals(procNamePatternField.getText()) ? null : procNamePatternField
                .getText();
    }

    /**
     * Returns configured naming strategy
     */
    public String getNamingStrategy() {
        return strategy;
    }
}
