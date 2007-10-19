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

import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.merge.DbMerger;
import org.apache.cayenne.merge.ExecutingMergerContext;
import org.apache.cayenne.merge.MergerContext;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ValidationResultBrowser;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.validation.ValidationResult;

/**
 */
public class MergerOptions extends CayenneController {

    protected MergerOptionsView view;
    // protected ObjectBinding[] optionBindings;
    protected ObjectBinding sqlBinding;

    protected DBConnectionInfo connectionInfo;
    protected DataMap dataMap;
    protected DbAdapter adapter;
    protected String textForSQL;

    protected DbMerger merger;
    protected MergerTokenSelectorController tokens;

    public MergerOptions(ProjectController parent, String title,
            DBConnectionInfo connectionInfo, DataMap dataMap) {
        super(parent);

        this.dataMap = dataMap;
        this.tokens = new MergerTokenSelectorController(parent);
        this.view = new MergerOptionsView(tokens.getView());
        this.connectionInfo = connectionInfo;
        /*
         * TODO:? this.generatorDefaults = (DBGeneratorDefaults) parent
         * .getPreferenceDomainForProject() .getDetail("DbGenerator",
         * DBGeneratorDefaults.class, true);
         */
        this.view.setTitle(title);
        initController();

        // tables.updateTables(dataMap);
        prepareMigrator();
        // generatorDefaults.configureGenerator(generator);
        createSQL();
        refreshView();
    }

    public Component getView() {
        return view;
    }

    /*
     * public DBGeneratorDefaults getGeneratorDefaults() { return generatorDefaults; }
     */

    public String getTextForSQL() {
        return textForSQL;
    }

    protected void initController() {

        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        sqlBinding = builder.bindToTextArea(view.getSql(), "textForSQL");

        /*
         * optionBindings = new ObjectBinding[5]; optionBindings[0] =
         * builder.bindToStateChangeAndAction(view.getCreateFK(),
         * "generatorDefaults.createFK", "refreshSQLAction()"); optionBindings[1] =
         * builder.bindToStateChangeAndAction(view.getCreatePK(),
         * "generatorDefaults.createPK", "refreshSQLAction()"); optionBindings[2] =
         * builder.bindToStateChangeAndAction(view.getCreateTables(),
         * "generatorDefaults.createTables", "refreshSQLAction()"); optionBindings[3] =
         * builder.bindToStateChangeAndAction(view.getDropPK(),
         * "generatorDefaults.dropPK", "refreshSQLAction()"); optionBindings[4] =
         * builder.bindToStateChangeAndAction(view.getDropTables(),
         * "generatorDefaults.dropTables", "refreshSQLAction()");
         */

        builder.bindToAction(view.getGenerateButton(), "generateSchemaAction()");
        builder.bindToAction(view.getSaveSqlButton(), "storeSQLAction()");
        builder.bindToAction(view.getCancelButton(), "closeAction()");

        // refresh SQL if different tables were selected
        view.getTabs().addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (view.getTabs().getSelectedIndex() == 1) {
                    // this assumes that some tables where checked/unchecked... not very
                    // efficient
                    refreshGeneratorAction();
                }
            }
        });
    }

    /**
     * check database and create the {@link List} of {@link MergerToken}s
     */
    protected void prepareMigrator() {
        try {
            adapter = connectionInfo.makeAdapter(getApplication()
                    .getClassLoadingService());
            tokens.setMergerFactory(adapter.mergerFactory());
            merger = new DbMerger();
            List mergerTokens = merger.createMergeTokens(adapter, connectionInfo
                    .makeDataSource(getApplication().getClassLoadingService()), dataMap);
            tokens.setTokens(mergerTokens);
        }
        catch (Exception ex) {
            reportError("Error loading adapter", ex);
        }
    }

    /**
     * Returns SQL statements generated for selected schema generation options.
     */
    protected void createSQL() {
        // convert them to string representation for display
        final StringBuffer buf = new StringBuffer();

        Iterator it = tokens.getSelectedTokens().iterator();
        String batchTerminator = adapter.getBatchTerminator();

        final String lineEnd = (batchTerminator != null) ? "\n"
                + batchTerminator
                + "\n\n" : "\n\n";

        MergerContext context = new MergerContext() {

            public void executeSql(String sql) {
                buf.append(sql);
                buf.append(lineEnd);
            }

            public DbAdapter getAdapter() {
                return adapter;
            }

            public DataMap getDataMap() {
                return dataMap;
            }

            public ValidationResult getValidationResult() {
                return new ValidationResult();
            }

        };

        while (it.hasNext()) {
            MergerToken token = (MergerToken) it.next();
            token.execute(context);
            // buf.append(token.createSql(adapter)).append(lineEnd);
        }

        textForSQL = buf.toString();
    }

    protected void refreshView() {

        /*
         * for (int i = 0; i < optionBindings.length; i++) {
         * optionBindings[i].updateView(); }
         */

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
        // prepareMigrator();
        refreshSQLAction();
    }

    /**
     * Updates a text area showing generated SQL.
     */
    public void refreshSQLAction() {
        // sync generator with defaults, make SQL, then sync the view...
        // generatorDefaults.configureGenerator(generator);
        createSQL();
        sqlBinding.updateView();
    }

    /**
     * Performs configured schema operations via DbGenerator.
     */
    public void generateSchemaAction() {
        refreshGeneratorAction();

        // sanity check...
        List tokensToMigrate = tokens.getSelectedTokens();
        if (tokensToMigrate.isEmpty()) {
            JOptionPane.showMessageDialog(getView(), "Nothing to migrate.");
            return;
        }

        try {
            DataSource dataSource = connectionInfo.makeDataSource(getApplication()
                    .getClassLoadingService());
            // generator.runGenerator(dataSource);

            MergerContext mergerContext = new ExecutingMergerContext(
                    dataMap,
                    dataSource,
                    adapter);
            for (Iterator it = tokensToMigrate.iterator(); it.hasNext();) {
                MergerToken tok = (MergerToken) it.next();
                tok.execute(mergerContext);
            }
            ValidationResult failures = mergerContext.getValidationResult();

            if (failures == null || !failures.hasFailures()) {
                JOptionPane.showMessageDialog(getView(), "Migration Complete.");
            }
            else {
                new ValidationResultBrowser(this).startupAction(
                        "Migration Complete",
                        "Migration finished. The following problem(s) were ignored.",
                        failures);
            }
        }
        catch (Throwable th) {
            reportError("Migration Error", th);
        }
    }

    /**
     * Allows user to save generated SQL in a file.
     */
    public void storeSQLAction() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setDialogTitle("Save SQL Script");

        File projectDir = Application.getProject().getProjectDirectory();

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
}
