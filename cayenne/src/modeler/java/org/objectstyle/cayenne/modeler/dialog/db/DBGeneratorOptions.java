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

import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.sql.DataSource;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.objectstyle.cayenne.access.DbGenerator;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.dialog.ValidationResultBrowser;
import org.objectstyle.cayenne.modeler.pref.DBConnectionInfo;
import org.objectstyle.cayenne.modeler.pref.DBGeneratorDefaults;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.swing.BindingBuilder;
import org.objectstyle.cayenne.swing.ObjectBinding;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * @author Andrei Adamchik
 */
public class DBGeneratorOptions extends CayenneController {

    protected DBGeneratorOptionsView view;
    protected ObjectBinding[] optionBindings;
    protected ObjectBinding sqlBinding;

    protected DBConnectionInfo connectionInfo;
    protected DataMap dataMap;
    protected DBGeneratorDefaults generatorDefaults;
    protected DbGenerator generator;
    protected String textForSQL;

    protected TableSelector tables;

    public DBGeneratorOptions(ProjectController parent, String title,
            DBConnectionInfo connectionInfo, DataMap dataMap) {
        super(parent);

        this.dataMap = dataMap;
        this.tables = new TableSelector(parent);
        this.view = new DBGeneratorOptionsView(tables.getView());
        this.connectionInfo = connectionInfo;
        this.generatorDefaults = (DBGeneratorDefaults) parent
                .getPreferenceDomainForProject()
                .getDetail("DbGenerator", DBGeneratorDefaults.class, true);

        this.view.setTitle(title);
        initController();

        tables.updateTables(dataMap);
        prepareGenerator();
        generatorDefaults.configureGenerator(generator);
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

        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        sqlBinding = builder.bindToTextArea(view.getSql(), "textForSQL");

        optionBindings = new ObjectBinding[5];
        optionBindings[0] = builder.bindToCheckbox(
                view.getCreateFK(),
                "generatorDefaults.createFK",
                "refreshSQLAction()");
        optionBindings[1] = builder.bindToCheckbox(
                view.getCreatePK(),
                "generatorDefaults.createPK",
                "refreshSQLAction()");
        optionBindings[2] = builder.bindToCheckbox(
                view.getCreateTables(),
                "generatorDefaults.createTables",
                "refreshSQLAction()");
        optionBindings[3] = builder.bindToCheckbox(
                view.getDropPK(),
                "generatorDefaults.dropPK",
                "refreshSQLAction()");
        optionBindings[4] = builder.bindToCheckbox(
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
            this.generator = new DbGenerator(adapter, dataMap, tables.getExcludedTables());
            this.generatorDefaults.adjustForAdapter(adapter);
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
        StringBuffer buf = new StringBuffer();
        Iterator it = generator.configuredStatements().iterator();
        String batchTerminator = generator.getAdapter().getBatchTerminator();

        String lineEnd = (batchTerminator != null)
                ? "\n" + batchTerminator + "\n\n"
                : "\n\n";

        while (it.hasNext()) {
            buf.append(it.next()).append(lineEnd);
        }

        textForSQL = buf.toString();
    }

    protected void refreshView() {

        for (int i = 0; i < optionBindings.length; i++) {
            optionBindings[i].updateView();
        }

        sqlBinding.updateView();
    }

    // ===============
    //    Actions
    // ===============

    /**
     * Starts options dialog.
     */
    public void startupAction() {
        view.pack();
        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.show();
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
        generatorDefaults.configureGenerator(generator);
        createSQL();
        sqlBinding.updateView();
    }

    /**
     * Performs configured schema operations via DbGenerator.
     */
    public void generateSchemaAction() {
        refreshGeneratorAction();

        // sanity check...
        if (generator.isEmpty(true)) {
            JOptionPane.showMessageDialog(getView(), "Nothing to generate.");
            return;
        }

        try {
            DataSource dataSource = connectionInfo.makeDataSource(getApplication()
                    .getClassLoadingService());
            generator.runGenerator(dataSource);

            ValidationResult failures = generator.getFailures();

            if (failures == null || !failures.hasFailures()) {
                JOptionPane.showMessageDialog(getView(), "Schema Generation Complete.");
            }
            else {
                new ValidationResultBrowser(this).startupAction(
                        "Schema Generation Complete",
                        "Schema generation finished. The following problem(s) were ignored.",
                        failures);
            }
        }
        catch (Throwable th) {
            reportError("Schema Generation Error", th);
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