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

package org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.toolkit.combobox.AutoCompletion;
import org.apache.cayenne.modeler.toolkit.combobox.CMComboBox;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.NameGeneratorPreferences;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class ReverseEngineeringConfigPanel extends ProjectPanel {

    private static final String DATA_FIELDS_LAYOUT = "right:pref, 3dlu, fill:235dlu";

    private final JComboBox<String> strategyCombo;
    private final CMUndoableTextField meaningfulPk;
    private final CMUndoableTextField stripFromTableNames;
    private final JCheckBox skipRelationshipsLoading;
    private final JCheckBox skipPrimaryKeyLoading;
    private final JCheckBox forceDataMapCatalog;
    private final JCheckBox forceDataMapSchema;
    private final JCheckBox useJava7Types;
    private final CMUndoableTextField tableTypes;

    private final DbImportView dbImportView;

    public ReverseEngineeringConfigPanel(ProjectSession session, DbImportView dbImportView) {
        super(session);

        this.dbImportView = dbImportView;
        this.strategyCombo = new CMComboBox<>();
        this.meaningfulPk = new CMUndoableTextField(app.getUndoManager());
        this.stripFromTableNames = new CMUndoableTextField(app.getUndoManager());
        this.tableTypes = new CMUndoableTextField(app.getUndoManager());
        this.skipRelationshipsLoading = new JCheckBox();
        this.skipPrimaryKeyLoading = new JCheckBox();
        this.forceDataMapCatalog = new JCheckBox();
        this.forceDataMapSchema = new JCheckBox();
        this.useJava7Types = new JCheckBox();

        initLayout();
        initBindings();
    }

    private void initLayout() {
        AutoCompletion.enable(strategyCombo, false, true, session::getSelectedDataMap);
        strategyCombo.setToolTipText("Naming strategy to use");

        meaningfulPk.setToolTipText("<html>Regular expression to filter tables with meaningful primary keys.<br>" +
                "Multiple expressions divided by comma can be used.<br>" +
                "Example: <b>^table1|^table2|^prefix.*|table_name</b></html>");
        stripFromTableNames.setToolTipText("<html>Regex that matches the part of the table name that needs to be stripped off " +
                "when generating ObjEntity name</html>");
        tableTypes.setToolTipText("<html>Default types to import is TABLE and VIEW.");

        skipRelationshipsLoading.setToolTipText("<html>Whether to load relationships.</html>");
        skipPrimaryKeyLoading.setToolTipText("<html>Whether to load primary keys.</html>");
        forceDataMapCatalog.setToolTipText("<html>Automatically tagging each DbEntity with the actual DB catalog/schema" +
                "(default behavior) may sometimes be undesirable.<br>  If this is the case then setting <b>forceDataMapCatalog</b> " +
                "to <b>true</b> will set DbEntity catalog to one in the DataMap.</html>");
        forceDataMapSchema.setToolTipText("<html>Automatically tagging each DbEntity with the actual DB catalog/schema " +
                "(default behavior) may sometimes be undesirable.<br> If this is the case then setting <b>forceDataMapSchema</b> " +
                "to <b>true</b> will set DbEntity schema to one in the DataMap.</html>");
        useJava7Types.setToolTipText("<html>Use <b>java.util.Date</b> for all columns with <i>DATE/TIME/TIMESTAMP</i> types.<br>" +
                "By default <b>java.time.*</b> types will be used.</html>");

        setLayout(new BorderLayout());
        FormLayout formLayout = new FormLayout(DATA_FIELDS_LAYOUT, "");
        DefaultFormBuilder builder = new DefaultFormBuilder(formLayout);
        builder.setDefaultDialogBorder();
        builder.append("Naming Strategy:", strategyCombo);
        builder.append("Meaningful PK:", meaningfulPk);
        builder.append("Strip From Table Names:", stripFromTableNames);
        builder.append("Table Types:", tableTypes);
        builder.append("Skip Relationships:", skipRelationshipsLoading);
        builder.append("Skip Primary Keys:", skipPrimaryKeyLoading);
        builder.append("Force DataMap Catalog:", forceDataMapCatalog);
        builder.append("Force DataMap Schema:", forceDataMapSchema);
        builder.append("Use Java 7 Types:", useJava7Types);
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initBindings() {
        meaningfulPk.addCommitListener(text -> {
            if (!dbImportView.isInitFromModel()) {
                getReverseEngineeringBySelectedMap().setMeaningfulPkTables(text);
                session.setDirty(true);
            }
        });
        stripFromTableNames.addCommitListener(text -> {
            if (!dbImportView.isInitFromModel()) {
                getReverseEngineeringBySelectedMap().setStripFromTableNames(text);
                session.setDirty(true);
            }
        });
        tableTypes.addCommitListener(this::applyTableTypes);
        skipRelationshipsLoading.addActionListener(e -> {
            if (!dbImportView.isInitFromModel()) {
                getReverseEngineeringBySelectedMap().setSkipRelationshipsLoading(skipRelationshipsLoading.isSelected());
                session.setDirty(true);
            }
        });
        skipPrimaryKeyLoading.addActionListener(e -> {
            if (!dbImportView.isInitFromModel()) {
                getReverseEngineeringBySelectedMap().setSkipPrimaryKeyLoading(skipPrimaryKeyLoading.isSelected());
                session.setDirty(true);
            }
        });
        forceDataMapCatalog.addActionListener(e -> {
            if (!dbImportView.isInitFromModel()) {
                getReverseEngineeringBySelectedMap().setForceDataMapCatalog(forceDataMapCatalog.isSelected());
                session.setDirty(true);
            }
        });
        forceDataMapSchema.addActionListener(e -> {
            if (!dbImportView.isInitFromModel()) {
                getReverseEngineeringBySelectedMap().setForceDataMapSchema(forceDataMapSchema.isSelected());
                session.setDirty(true);
            }
        });
        useJava7Types.addActionListener(e -> {
            if (!dbImportView.isInitFromModel()) {
                getReverseEngineeringBySelectedMap().setUseJava7Types(useJava7Types.isSelected());
                session.setDirty(true);
            }
        });
        strategyCombo.addActionListener(e -> {
            String strategy = (String) strategyCombo.getSelectedItem();
            checkStrategy(strategy);
            if (!dbImportView.isInitFromModel()) {
                getReverseEngineeringBySelectedMap().setNamingStrategy(strategy);
                NameGeneratorPreferences.getInstance().addToLastUsedStrategies(app, strategy);
                session.setDirty(true);
            }
        });
    }

    void fillCheckboxes(ReverseEngineering reverseEngineering) {
        skipRelationshipsLoading.setSelected(reverseEngineering.getSkipRelationshipsLoading());
        skipPrimaryKeyLoading.setSelected(reverseEngineering.getSkipPrimaryKeyLoading());
        forceDataMapCatalog.setSelected(reverseEngineering.isForceDataMapCatalog());
        forceDataMapSchema.setSelected(reverseEngineering.isForceDataMapSchema());
        useJava7Types.setSelected(reverseEngineering.isUseJava7Types());
    }

    void initializeTextFields(ReverseEngineering reverseEngineering) {
        meaningfulPk.setText(reverseEngineering.getMeaningfulPkTables());
        stripFromTableNames.setText(reverseEngineering.getStripFromTableNames());
    }

    ReverseEngineering getReverseEngineeringBySelectedMap() {
        DataMap dataMap = session.getSelectedDataMap();
        return app.getMetaData().get(dataMap, ReverseEngineering.class);
    }

    void initStrategy(ReverseEngineering reverseEngineering) {
        Vector<String> arr = NameGeneratorPreferences
                .getInstance()
                .getLastUsedStrategies(app);
        strategyCombo.setModel(new DefaultComboBoxModel<>(arr));
        strategyCombo.setSelectedItem(reverseEngineering.getNamingStrategy());
    }



    private void checkStrategy(String strategy) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(strategy);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    strategy + " not found. Please, add naming strategy to classpath.",
                    "Error",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    void applyTableTypes(String text) {
        ReverseEngineering reverseEngineering = getReverseEngineeringBySelectedMap();
        if (text == null || text.isEmpty()) {
            String[] tableTypesFromReverseEngineering = reverseEngineering.getTableTypes();
            tableTypes.setText(String.join(",", tableTypesFromReverseEngineering));
            JOptionPane.showMessageDialog(
                    app.getFrame(),
                    "Table types field can't be empty.",
                    "Error setting table types",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            reverseEngineering.getTableTypesCollection().clear();
            String[] types = text.split("\\s*,\\s*");
            for (String type : types) {
                if (!type.isEmpty()) {
                    reverseEngineering.addTableType(type.trim());
                }
            }
            if (!dbImportView.isInitFromModel()) {
                session.setDirty(true);
                dbImportView.invalidateDbSchema();
            }
        }
    }

    CMUndoableTextField getTableTypes() {
        return tableTypes;
    }
}
