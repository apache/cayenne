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

package org.apache.cayenne.modeler.editor.dbimport;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Vector;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.NameGeneratorPreferences;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;

/**
 * @since 4.1
 */
public class ReverseEngineeringConfigPanel extends JPanel {

    private static final String DATA_FIELDS_LAYOUT = "right:pref, 3dlu, fill:235dlu";

    private JComboBox<String> strategyCombo;
    private TextAdapter meaningfulPk;
    private TextAdapter stripFromTableNames;
    private JCheckBox skipRelationshipsLoading;
    private JCheckBox skipPrimaryKeyLoading;
    private JCheckBox forceDataMapCatalog;
    private JCheckBox forceDataMapSchema;
    private JCheckBox usePrimitives;
    private JCheckBox useJava7Types;

    private ProjectController projectController;

    ReverseEngineeringConfigPanel(ProjectController projectController) {
        this.projectController = projectController;
        initFormElements();
        initListeners();
        buildView();
    }

    private void buildView() {
        FormLayout panelLayout = new FormLayout(DATA_FIELDS_LAYOUT);
        DefaultFormBuilder panelBuilder = new DefaultFormBuilder(panelLayout);
        panelBuilder.setDefaultDialogBorder();

        panelBuilder.append("Tables with Meaningful PK Pattern:", meaningfulPk.getComponent());
        panelBuilder.append("Strip from table names:", stripFromTableNames.getComponent());
        panelBuilder.append("Skip relationships loading:", skipRelationshipsLoading);
        panelBuilder.append("Skip primary key loading:", skipPrimaryKeyLoading);
        panelBuilder.append("Force datamap catalog:", forceDataMapCatalog);
        panelBuilder.append("Force datamap schema:", forceDataMapSchema);
        panelBuilder.append("Use Java primitive types:", usePrimitives);
        panelBuilder.append("Use java.util.Date type:", useJava7Types);
        panelBuilder.append("Naming strategy:", strategyCombo);

        add(panelBuilder.getPanel());
    }

    void fillCheckboxes(ReverseEngineering reverseEngineering) {
        skipRelationshipsLoading.setSelected(reverseEngineering.getSkipRelationshipsLoading());
        skipPrimaryKeyLoading.setSelected(reverseEngineering.getSkipPrimaryKeyLoading());
        forceDataMapCatalog.setSelected(reverseEngineering.isForceDataMapCatalog());
        forceDataMapSchema.setSelected(reverseEngineering.isForceDataMapSchema());
        usePrimitives.setSelected(reverseEngineering.isUsePrimitives());
        useJava7Types.setSelected(reverseEngineering.isUseJava7Types());
    }

    void initializeTextFields(ReverseEngineering reverseEngineering) {
        meaningfulPk.setText(reverseEngineering.getMeaningfulPkTables());
        stripFromTableNames.setText(reverseEngineering.getStripFromTableNames());
    }

    private ReverseEngineering getReverseEngineeringBySelectedMap() {
        DataMap dataMap = projectController.getCurrentDataMap();
        return projectController.getApplication().getMetaData().get(dataMap, ReverseEngineering.class);
    }

    void initStrategy(ReverseEngineering reverseEngineering) {
        Vector<String> arr = NameGeneratorPreferences
                .getInstance()
                .getLastUsedStrategies();
        strategyCombo.setModel(new DefaultComboBoxModel<>(arr));
        strategyCombo.setSelectedItem(reverseEngineering.getNamingStrategy());
    }

    private void initFormElements() {
        strategyCombo = Application.getWidgetFactory().createComboBox();
        AutoCompletion.enable(strategyCombo, false, true);
        strategyCombo.setToolTipText("Naming strategy to use");

        JTextField meaningfulPkField = new JTextField();
        meaningfulPkField.setToolTipText("<html>Regular expression to filter tables with meaningful primary keys.<br>" +
                "Multiple expressions divided by comma can be used.<br>" +
                "Example: <b>^table1|^table2|^prefix.*|table_name</b></html>");
        meaningfulPk = new TextAdapter(meaningfulPkField) {
            protected void updateModel(String text) {
                getReverseEngineeringBySelectedMap().setMeaningfulPkTables(text);
                projectController.setDirty(true);
            }
        };

        JTextField stripFromTableNamesField = new JTextField();
        stripFromTableNamesField.setToolTipText("<html>Regex that matches the part of the table name that needs to be stripped off " +
                "when generating ObjEntity name</html>");
        stripFromTableNames = new TextAdapter(stripFromTableNamesField) {
            protected void updateModel(String text) {
                getReverseEngineeringBySelectedMap().setStripFromTableNames(text);
                projectController.setDirty(true);
            }
        };

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
        usePrimitives = new JCheckBox();
        usePrimitives.setToolTipText("<html>Use primitive types (e.g. int) or Object types (e.g. java.lang.Integer)</html>");
    }

    private void initListeners() {
        skipRelationshipsLoading.addActionListener(e -> {
            getReverseEngineeringBySelectedMap().setSkipRelationshipsLoading(skipRelationshipsLoading.isSelected());
            projectController.setDirty(true);
        });
        skipPrimaryKeyLoading.addActionListener(e -> {
            getReverseEngineeringBySelectedMap().setSkipPrimaryKeyLoading(skipPrimaryKeyLoading.isSelected());
            projectController.setDirty(true);
        });
        forceDataMapCatalog.addActionListener(e -> {
            getReverseEngineeringBySelectedMap().setForceDataMapCatalog(forceDataMapCatalog.isSelected());
            projectController.setDirty(true);
        });
        forceDataMapSchema.addActionListener(e -> {
            getReverseEngineeringBySelectedMap().setForceDataMapSchema(forceDataMapSchema.isSelected());
            projectController.setDirty(true);
        });
        usePrimitives.addActionListener(e -> {
            getReverseEngineeringBySelectedMap().setUsePrimitives(usePrimitives.isSelected());
            projectController.setDirty(true);
        });
        useJava7Types.addActionListener(e -> {
            getReverseEngineeringBySelectedMap().setUseJava7Types(useJava7Types.isSelected());
            projectController.setDirty(true);
        });
        strategyCombo.addActionListener(e -> {
            String strategy = (String) ReverseEngineeringConfigPanel.this.getStrategyCombo().getSelectedItem();
            checkStrategy(strategy);
            getReverseEngineeringBySelectedMap().setNamingStrategy(strategy);
            NameGeneratorPreferences.getInstance().addToLastUsedStrategies(strategy);
            projectController.setDirty(true);
        });
    }

    private void checkStrategy(String strategy) {
        try{
            Thread.currentThread().getContextClassLoader().loadClass(strategy);
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    strategy + " not found. Please, add naming strategy to classpath.",
                    "Error",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    JComboBox<String> getStrategyCombo() {
        return strategyCombo;
    }

    TextAdapter getMeaningfulPk() {
        return meaningfulPk;
    }

    TextAdapter getStripFromTableNames() {
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

    JCheckBox getUsePrimitives() {
        return usePrimitives;
    }

    JCheckBox getUseJava7Types() {
        return useJava7Types;
    }

}
