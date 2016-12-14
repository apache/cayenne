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

package org.apache.cayenne.modeler.dialog.db.load;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DbActionOptionsDialog;
import org.apache.cayenne.modeler.util.NameGeneratorPreferences;

import java.util.Collection;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/**
 * Dialog for selecting database reverse-engineering parameters.
 */
public class DbLoaderOptionsDialog extends DbActionOptionsDialog {

    private JTextField tableNamePatternField;
    private JTextField meaningfulPk;
    private JTextField procNamePatternField;
    private JComboBox<String> strategyCombo;
    protected String strategy;

    /**
     * Creates and initializes new ChooseSchemaDialog.
     */
    public DbLoaderOptionsDialog(Collection<String> catalogs, Collection<String> schemas,
                                 String dbCatalog, String currentSchema) {
        super(Application.getFrame(), "Reengineer DB Schema: Select Options",
                catalogs, schemas, dbCatalog, currentSchema);
    }

    @Override
    protected void initForm(DefaultFormBuilder builder) {
        tableNamePatternField = new JTextField();
        tableNamePatternField.setToolTipText("<html>Regular expression to filter table names.<br>" +
                "Default expression <b>.*</b> includes all tables.</html>");
        procNamePatternField = new JTextField();
        procNamePatternField.setToolTipText("<html>Regular expression to filter stored procedures names.<br>" +
                "Default expression <b>.*</b> includes all stored procedures.</html>");
        meaningfulPk = new JTextField();
        meaningfulPk.setToolTipText("<html>Regular expression to filter tables with meaningful primary keys.<br>" +
                "Multiple expressions divided by comma can be used.<br>" +
                "Example: <b>^table1|^table2|^prefix.*|table_name</b></html>");
        strategyCombo = new JComboBox<>();
        strategyCombo.setEditable(true);

        builder.append("Table Name Pattern:", tableNamePatternField);
        builder.append("Procedure Name Pattern:", procNamePatternField);
        builder.append("Naming Strategy:", strategyCombo);
        builder.append("Tables with Meaningful PK Pattern:", meaningfulPk);
    }

    protected void initFromModel(Collection<String> catalogs, Collection<String> schemas, String currentCatalog, String currentSchema) {
        super.initFromModel(catalogs, schemas, currentCatalog, currentSchema);

        this.tableNamePatternField.setText(WILDCARD_PATTERN);
        this.procNamePatternField.setText(WILDCARD_PATTERN);

        Vector<String> arr = NameGeneratorPreferences
                .getInstance()
                .getLastUsedStrategies();
        strategyCombo.setModel(new DefaultComboBoxModel<>(arr));
    }

    String getTableNamePattern() {
        return "".equals(tableNamePatternField.getText()) ? null : tableNamePatternField
                .getText();
    }

    String getMeaningfulPk() {
        return "".equals(meaningfulPk.getText()) ? null : meaningfulPk
                .getText();
    }

    String getProcedureNamePattern() {
        return "".equals(procNamePatternField.getText()) ? null : procNamePatternField
                .getText();
    }

    String getNamingStrategy() {
        return (String) strategyCombo.getSelectedItem();
    }
}
