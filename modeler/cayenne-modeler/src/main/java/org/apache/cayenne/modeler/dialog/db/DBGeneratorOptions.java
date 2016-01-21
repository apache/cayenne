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

import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ValidationResultBrowser;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.DBGeneratorDefaults;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.DbAdapterInfo;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.validation.ValidationResult;

import javax.sql.DataSource;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 */
public class DBGeneratorOptions extends CayenneController {

    protected DBGeneratorOptionsView view;
    protected ObjectBinding[] optionBindings;
    protected ObjectBinding sqlBinding;
    protected ObjectBinding adapterBinding;

    protected DBConnectionInfo connectionInfo;
    protected Collection<DataMap> dataMaps;
    protected DBGeneratorDefaults generatorDefaults;
    protected Collection<DbGenerator> generators;
    protected String textForSQL;

    protected TableSelectorController tables;

    public DBGeneratorOptions(ProjectController parent, String title, Collection<DataMap> dataMaps) {
        super(parent);

        this.dataMaps = dataMaps;
        this.tables = new TableSelectorController(parent);
        this.view = new DBGeneratorOptionsView(tables.getView());
        this.connectionInfo = new DBConnectionInfo();
        this.generatorDefaults = new DBGeneratorDefaults(parent
                .getPreferenceForProject()
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

    public Component getView() {
        return view;
    }

    public DBGeneratorDefaults getGeneratorDefaults() {
        return generatorDefaults;
    }

    public String getTextForSQL() {
        return textForSQL;
    }

    protected void initController() {

        DefaultComboBoxModel adapterModel = new DefaultComboBoxModel(
                DbAdapterInfo.getStandardAdapters());
        view.getAdapters().setModel(adapterModel);
        view.getAdapters().setSelectedIndex(0);

        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        sqlBinding = builder.bindToTextArea(view.getSql(), "textForSQL");
        adapterBinding = builder.bindToComboSelection(
                view.getAdapters(),
                "connectionInfo.dbAdapter",
                "refreshSQLAction()",
                "org.apache.cayenne.dba.JdbcAdapter");

        optionBindings = new ObjectBinding[5];
        optionBindings[0] = builder.bindToStateChangeAndAction(
                view.getCreateFK(),
                "generatorDefaults.createFK",
                "refreshSQLAction()");
        optionBindings[1] = builder.bindToStateChangeAndAction(
                view.getCreatePK(),
                "generatorDefaults.createPK",
                "refreshSQLAction()");
        optionBindings[2] = builder.bindToStateChangeAndAction(
                view.getCreateTables(),
                "generatorDefaults.createTables",
                "refreshSQLAction()");
        optionBindings[3] = builder.bindToStateChangeAndAction(
                view.getDropPK(),
                "generatorDefaults.dropPK",
                "refreshSQLAction()");
        optionBindings[4] = builder.bindToStateChangeAndAction(
                view.getDropTables(),
                "generatorDefaults.dropTables",
                "refreshSQLAction()");

        builder.bindToAction(view.getGenerateButton(), "generateSchemaAction()");
        builder.bindToAction(view.getSaveSqlButton(), "storeSQLAction()");
        builder.bindToAction(view.getCancelButton(), "closeAction()");

        // refresh SQL if different tables were selected
        view.getTabs().addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (view.getTabs().getSelectedIndex() == 0) {
                    // this assumes that some tables where checked/unchecked... not very
                    // efficient
                    refreshGeneratorAction();
                }
            }
        });
    }

    /**
     * Creates new internal DbGenerator instance.
     */
    protected void prepareGenerator() {
        try {
            DbAdapter adapter = connectionInfo.makeAdapter(getApplication()
                    .getClassLoadingService());
            generators = new ArrayList<DbGenerator>();
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
        StringBuffer buf = new StringBuffer();
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
        getView().setEnabled(connectionInfo != null);

        for (ObjectBinding optionBinding : optionBindings) {
            optionBinding.updateView();
        }

        sqlBinding.updateView();
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
        // sync generator with defaults, make SQL, then sync the view...
        adapterBinding.updateView();
        connectionInfo.setDbAdapter((String) view.getAdapters().getSelectedItem());
        prepareGenerator();
        generatorDefaults.configureGenerator(generators);
        createSQL();
        sqlBinding.updateView();
    }

    /**
     * Performs configured schema operations via DbGenerator.
     */
    public void generateSchemaAction() {

        DataSourceController connectWizard = new DataSourceController(
                this.getParent(),
                "Generate DB Schema: Connect to Database",
                null,
                null);

        if (!connectWizard.startupAction()) {
            // canceled
            return;
        }

        this.connectionInfo = connectWizard.getConnectionInfo();

        refreshGeneratorAction();

        Collection<ValidationResult> failures = new ArrayList<ValidationResult>();

        // sanity check...
        for (DbGenerator generator : generators) {
            if (generator.isEmpty(true)) {
                JOptionPane.showMessageDialog(getView(), "Nothing to generate.");
                return;
            }

            try {

                DataSource dataSource = connectionInfo.makeDataSource(getApplication()
                        .getClassLoadingService());
                generator.runGenerator(dataSource);
                failures.add(generator.getFailures());
            } catch (Throwable th) {
                reportError("Schema Generation Error", th);
            }
        }

        if (failures.size() == 0) {
            JOptionPane.showMessageDialog(getView(), "Schema Generation Complete.");
        } else {
            new ValidationResultBrowser(this)
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

        if (projectDir != null) {
            fc.setCurrentDirectory(projectDir);
        }

        if (fc.showSaveDialog(getView()) == JFileChooser.APPROVE_OPTION) {
            refreshGeneratorAction();

            try {
                File file = fc.getSelectedFile();
                FileWriter fw = new FileWriter(file);
                PrintWriter pw = new PrintWriter(fw);
                pw.print(textForSQL);
                pw.flush();
                pw.close();
            }
            catch (IOException ex) {
                reportError("Error Saving SQL", ex);
            }
        }
    }

    public void closeAction() {
        view.dispose();
    }

    public DBConnectionInfo getConnectionInfo() {
        return this.connectionInfo;
    }

    public void setConnectionInfo(DBConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
        refreshView();
    }
}
