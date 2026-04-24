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

package org.apache.cayenne.modeler.ui.dbgen;

import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.validationbrowser.ValidationResultBrowserController;
import org.apache.cayenne.modeler.ui.datasourcewizard.DataSourceWizardController;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.DBGeneratorDefaults;
import org.apache.cayenne.modeler.util.DbAdapterInfo;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


public class DBGeneratorOptionsController extends ChildController<ProjectController> {

    protected DBGeneratorOptionsView view;
    private boolean updatingAdapterCombo;

    protected DBConnectionInfo connectionInfo;
    protected Collection<DataMap> dataMaps;
    protected DBGeneratorDefaults generatorDefaults;
    protected Collection<DbGenerator> generators;
    protected String textForSQL;

    protected TableSelectorController tables;

    public DBGeneratorOptionsController(ProjectController parent, String title, Collection<DataMap> dataMaps) {
        super(parent);

        this.dataMaps = dataMaps;
        this.tables = new TableSelectorController(parent);
        this.view = new DBGeneratorOptionsView(tables.getView());
        this.connectionInfo = new DBConnectionInfo();
        // DataSource may not be initialized, so warn connection wizard
        this.connectionInfo.setAllowDataSourceFailure(true);
        this.generatorDefaults = new DBGeneratorDefaults(parent
                .getProjectPreferences()
                .node("DbGenerator"));

        this.view.setTitle(title);
        initController();
        connectionInfo.setDbAdapter((String) view.getAdapters().getSelectedItem());

        tables.updateTables(dataMaps);
        prepareGenerator();
        generatorDefaults.configureGenerator(generators);
        createSQL();
        refreshView();
    }

    @Override
    public Component getView() {
        return view;
    }

    protected void initController() {

        DefaultComboBoxModel<String> adapterModel = new DefaultComboBoxModel<>(
                DbAdapterInfo.getStandardAdapters());
        view.getAdapters().setModel(adapterModel);
        view.getAdapters().setSelectedIndex(0);

        view.getAdapters().addActionListener(e -> {
            if (updatingAdapterCombo) return;
            Object sel = view.getAdapters().getSelectedItem();
            connectionInfo.setDbAdapter("org.apache.cayenne.dba.JdbcAdapter".equals(sel) ? null : (String) sel);
            refreshSQLAction();
        });

        view.getCreateFK().addActionListener(e -> { generatorDefaults.setCreateFK(view.getCreateFK().isSelected()); refreshSQLAction(); });
        view.getCreatePK().addActionListener(e -> { generatorDefaults.setCreatePK(view.getCreatePK().isSelected()); refreshSQLAction(); });
        view.getCreateTables().addActionListener(e -> { generatorDefaults.setCreateTables(view.getCreateTables().isSelected()); refreshSQLAction(); });
        view.getDropPK().addActionListener(e -> { generatorDefaults.setDropPK(view.getDropPK().isSelected()); refreshSQLAction(); });
        view.getDropTables().addActionListener(e -> { generatorDefaults.setDropTables(view.getDropTables().isSelected()); refreshSQLAction(); });

        view.getGenerateButton().addActionListener(e -> generateSchemaAction());
        view.getSaveSqlButton().addActionListener(e -> storeSQLAction());
        view.getCancelButton().addActionListener(e -> closeAction());

        // refresh SQL if different tables were selected
        view.getTabs().addChangeListener(e -> {
            if (view.getTabs().getSelectedIndex() == 0) {
                // this assumes that some tables where checked/unchecked... not very efficient
                refreshGeneratorAction();
            }
        });
    }

    /**
     * Creates new internal DbGenerator instance.
     */
    protected void prepareGenerator() {
        try {
            DbAdapter adapter = connectionInfo.makeAdapter(getApplication().getClassLoadingService());
            generators = new ArrayList<>();
            for (DataMap dataMap : dataMaps) {
                this.generators.add(new DbGenerator(
                        adapter,
                        dataMap,
                        tables.getExcludedTables(),
                        null,
                        NoopJdbcEventLogger.getInstance()));
            }
        } catch (Exception ex) {
            reportError("Error loading adapter", ex);
        }
    }

    /**
     * Returns SQL statements generated for selected schema generation options.
     */
    protected void createSQL() {
        // convert them to string representation for display
        StringBuilder buf = new StringBuilder();
        for (DbGenerator generator : generators) {
            Iterator<String> it = generator.configuredStatements().iterator();
            String batchTerminator = generator.getAdapter().getBatchTerminator();

            String lineEnd = (batchTerminator != null)
                    ? "\n" + batchTerminator + "\n\n"
                    : "\n\n";

            while (it.hasNext()) {
                buf.append(it.next()).append(lineEnd);
            }
        }

        textForSQL = buf.toString();
    }

    protected void refreshView() {
        view.setEnabled(connectionInfo != null);

        view.getCreateFK().setSelected(generatorDefaults.createFK);
        view.getCreatePK().setSelected(generatorDefaults.createPK);
        view.getCreateTables().setSelected(generatorDefaults.createTables);
        view.getDropPK().setSelected(generatorDefaults.dropPK);
        view.getDropTables().setSelected(generatorDefaults.dropTables);

        view.getSql().setText(textForSQL);
    }

    // ===============
    // Actions
    // ===============

    /**
     * Starts options dialog.
     */
    public void startupAction() {
        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }

    public void refreshGeneratorAction() {
        prepareGenerator();
        refreshSQLAction();
    }

    /**
     * Updates a text area showing generated SQL.
     */
    public void refreshSQLAction() {
        // sync combo to reflect current connectionInfo (e.g. after generateSchemaAction replaces it)
        updatingAdapterCombo = true;
        try {
            String adapter = connectionInfo.getDbAdapter();
            view.getAdapters().setSelectedItem(adapter != null ? adapter : "org.apache.cayenne.dba.JdbcAdapter");
        } finally {
            updatingAdapterCombo = false;
        }
        connectionInfo.setDbAdapter((String) view.getAdapters().getSelectedItem());
        prepareGenerator();
        generatorDefaults.configureGenerator(generators);
        createSQL();
        view.getSql().setText(textForSQL);
    }

    /**
     * Performs configured schema operations via DbGenerator.
     */
    public void generateSchemaAction() {

        DataSourceWizardController connectWizard = new DataSourceWizardController(parent, "Generate DB Schema: Connect to Database");
        if (!connectWizard.startupAction()) {
            return;
        }

        this.connectionInfo = connectWizard.getConnectionInfo();

        refreshGeneratorAction();

        Collection<ValidationResult> failures = new ArrayList<>();

        // sanity check...
        for (DbGenerator generator : generators) {
            if (generator.isEmpty(true)) {
                JOptionPane.showMessageDialog(view, "Nothing to generate.");
                return;
            }

            try {
                generator.runGenerator(connectWizard.getDataSource());
                failures.add(generator.getFailures());
            } catch (Throwable th) {
                reportError("Schema Generation Error", th);
            }
        }

        if (failures.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Schema Generation Complete.");
        } else {
            new ValidationResultBrowserController(this)
                    .startupAction(
                            "Schema Generation Complete",
                            "Schema generation finished. The following problem(s) were ignored.",
                            failures);
        }
    }

    /**
     * Allows user to save generated SQL in a file.
     */
    public void storeSQLAction() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setDialogTitle("Save SQL Script");

        File projectDir = new File(getApplication()
                .getProject()
                .getConfigurationResource()
                .getURL()
                .getPath());
        fc.setCurrentDirectory(projectDir);
        if (fc.showSaveDialog(view) == JFileChooser.APPROVE_OPTION) {
            refreshGeneratorAction();
            try {
                File file = fc.getSelectedFile();
                FileWriter fw = new FileWriter(file);
                PrintWriter pw = new PrintWriter(fw);
                pw.print(textForSQL);
                pw.flush();
                pw.close();
            } catch (IOException ex) {
                reportError("Error Saving SQL", ex);
            }
        }
    }

    public void closeAction() {
        view.dispose();
    }
}
