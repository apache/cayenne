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
import org.apache.cayenne.modeler.toolkit.WidgetFactory;
import org.apache.cayenne.modeler.toolkit.combo.AutoCompletion;
import org.apache.cayenne.modeler.toolkit.text.CayenneUndoableTextField;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.util.NameGeneratorPreferences;

import javax.swing.*;
import java.util.Vector;

public class ReverseEngineeringConfigPanel extends JPanel {

    private static final String DATA_FIELDS_LAYOUT = "right:pref, 3dlu, fill:235dlu";

    private JComboBox<String> strategyCombo;
    private CayenneUndoableTextField meaningfulPk;
    private CayenneUndoableTextField stripFromTableNames;
    private JCheckBox skipRelationshipsLoading;
    private JCheckBox skipPrimaryKeyLoading;
    private JCheckBox forceDataMapCatalog;
    private JCheckBox forceDataMapSchema;
    private JCheckBox useJava7Types;

    private CayenneUndoableTextField tableTypes;

    private final ProjectController controller;
    private final DbImportView dbImportView;

    ReverseEngineeringConfigPanel(ProjectController controller, DbImportView dbImportView) {
        this.controller = controller;
        this.dbImportView = dbImportView;
        initFormElements();
        initListeners();
        buildView();
    }

    private void buildView() {
        FormLayout panelLayout = new FormLayout(DATA_FIELDS_LAYOUT);
        DefaultFormBuilder panelBuilder = new DefaultFormBuilder(panelLayout);
        panelBuilder.setDefaultDialogBorder();

        panelBuilder.append("Tables with Meaningful PK Pattern:", meaningfulPk);
        panelBuilder.append("Strip from table names:", stripFromTableNames);
        panelBuilder.append("Skip relationships loading:", skipRelationshipsLoading);
        panelBuilder.append("Skip primary key loading:", skipPrimaryKeyLoading);
        panelBuilder.append("Force datamap catalog:", forceDataMapCatalog);
        panelBuilder.append("Force datamap schema:", forceDataMapSchema);
        panelBuilder.append("Use java.util.Date type:", useJava7Types);
        panelBuilder.append("Naming strategy:", strategyCombo);
        panelBuilder.append("Table types:", tableTypes);

        add(panelBuilder.getPanel());
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
        DataMap dataMap = controller.getSelectedDataMap();
        return controller.getApplication().getMetaData().get(dataMap, ReverseEngineering.class);
    }

    void initStrategy(ReverseEngineering reverseEngineering) {
        Vector<String> arr = NameGeneratorPreferences
                .getInstance()
                .getLastUsedStrategies();
        strategyCombo.setModel(new DefaultComboBoxModel<>(arr));
        strategyCombo.setSelectedItem(reverseEngineering.getNamingStrategy());
    }

    private void initFormElements() {
        strategyCombo = WidgetFactory.createComboBox();
        AutoCompletion.enable(strategyCombo, false, true);
        strategyCombo.setToolTipText("Naming strategy to use");

        meaningfulPk = new CayenneUndoableTextField(controller.getApplication().getUndoManager());
        meaningfulPk.setToolTipText("<html>Regular expression to filter tables with meaningful primary keys.<br>" +
                "Multiple expressions divided by comma can be used.<br>" +
                "Example: <b>^table1|^table2|^prefix.*|table_name</b></html>");
        meaningfulPk.addCommitListener(text -> {
            getReverseEngineeringBySelectedMap().setMeaningfulPkTables(text);
            if (!dbImportView.isInitFromModel()) {
                controller.setDirty(true);
            }
        });

        stripFromTableNames = new CayenneUndoableTextField(controller.getApplication().getUndoManager());
        stripFromTableNames.setToolTipText("<html>Regex that matches the part of the table name that needs to be stripped off " +
                "when generating ObjEntity name</html>");
        stripFromTableNames.addCommitListener(text -> {
            getReverseEngineeringBySelectedMap().setStripFromTableNames(text);
            if (!dbImportView.isInitFromModel()) {
                controller.setDirty(true);
            }
        });

        tableTypes = new CayenneUndoableTextField(controller.getApplication().getUndoManager());
        tableTypes.setToolTipText("<html>Default types to import is TABLE and VIEW.");
        tableTypes.addCommitListener(this::applyTableTypes);

        skipRelationshipsLoading = new JCheckBox();
        skipRelationshipsLoading.setToolTipText("<html>Whether to load relationships.</html>");
        skipPrimaryKeyLoading = new JCheckBox();
        skipPrimaryKeyLoading.setToolTipText("<html>Whether to load primary keys.</html>");
        forceDataMapCatalog = new JCheckBox();
        forceDataMapCatalog.setToolTipText("<html>Automatically tagging each DbEntity with the actual DB catalog/schema" +
                "(default behavior) may sometimes be undesirable.<br>  If this is the case then setting <b>forceDataMapCatalog</b> " +
                "to <b>true</b> will set DbEntity catalog to one in the DataMap.</html>");
        forceDataMapSchema = new JCheckBox();
        forceDataMapSchema.setToolTipText("<html>Automatically tagging each DbEntity with the actual DB catalog/schema " +
                "(default behavior) may sometimes be undesirable.<br> If this is the case then setting <b>forceDataMapSchema</b> " +
                "to <b>true</b> will set DbEntity schema to one in the DataMap.</html>");
        useJava7Types = new JCheckBox();
        useJava7Types.setToolTipText("<html>Use <b>java.util.Date</b> for all columns with <i>DATE/TIME/TIMESTAMP</i> types.<br>" +
                "By default <b>java.time.*</b> types will be used.</html>");
    }

    private void initListeners() {
        skipRelationshipsLoading.addActionListener(e -> {
            getReverseEngineeringBySelectedMap().setSkipRelationshipsLoading(skipRelationshipsLoading.isSelected());
            if (!dbImportView.isInitFromModel()) {
                controller.setDirty(true);
            }
        });
        skipPrimaryKeyLoading.addActionListener(e -> {
            getReverseEngineeringBySelectedMap().setSkipPrimaryKeyLoading(skipPrimaryKeyLoading.isSelected());
            if (!dbImportView.isInitFromModel()) {
                controller.setDirty(true);
            }
        });
        forceDataMapCatalog.addActionListener(e -> {
            getReverseEngineeringBySelectedMap().setForceDataMapCatalog(forceDataMapCatalog.isSelected());
            if (!dbImportView.isInitFromModel()) {
                controller.setDirty(true);
            }
        });
        forceDataMapSchema.addActionListener(e -> {
            getReverseEngineeringBySelectedMap().setForceDataMapSchema(forceDataMapSchema.isSelected());
            if (!dbImportView.isInitFromModel()) {
                controller.setDirty(true);
            }
        });
        useJava7Types.addActionListener(e -> {
            getReverseEngineeringBySelectedMap().setUseJava7Types(useJava7Types.isSelected());
            if (!dbImportView.isInitFromModel()) {
                controller.setDirty(true);
            }
        });
        strategyCombo.addActionListener(e -> {
            String strategy = (String) ReverseEngineeringConfigPanel.this.getStrategyCombo().getSelectedItem();
            checkStrategy(strategy);
            getReverseEngineeringBySelectedMap().setNamingStrategy(strategy);
            NameGeneratorPreferences.getInstance().addToLastUsedStrategies(strategy);
            if (!dbImportView.isInitFromModel()) {
                controller.setDirty(true);
            }
        });
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
                    controller.getApplication().getFrameController().getView(),
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
                controller.setDirty(true);
            }
        }
    }

    JComboBox<String> getStrategyCombo() {
        return strategyCombo;
    }

    CayenneUndoableTextField getMeaningfulPk() {
        return meaningfulPk;
    }

    CayenneUndoableTextField getStripFromTableNames() {
        return stripFromTableNames;
    }

    JCheckBox getSkipRelationshipsLoading() {
        return skipRelationshipsLoading;
    }

    JCheckBox getSkipPrimaryKeyLoading() {
        return skipPrimaryKeyLoading;
    }

    JCheckBox getForceDataMapCatalog() {
        return forceDataMapCatalog;
    }

    JCheckBox getForceDataMapSchema() {
        return forceDataMapSchema;
    }

    JCheckBox getUseJava7Types() {
        return useJava7Types;
    }

    CayenneUndoableTextField getTableTypes() {
        return tableTypes;
    }

}
