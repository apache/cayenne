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
package org.objectstyle.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import org.objectstyle.cayenne.conn.DriverDataSource;
import org.objectstyle.cayenne.modeler.pref.DBConnectionInfo;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.pref.PreferenceEditor;
import org.objectstyle.cayenne.swing.BindingBuilder;
import org.objectstyle.cayenne.util.Util;

/**
 * Editor for the local DataSources configured in preferences.
 * 
 * @author Andrei Adamchik
 */
public class DataSourcePreferences extends CayenneController {

    protected DataSourcePreferencesView view;
    protected PreferenceEditor editor;
    protected String dataSourceKey;
    protected Map dataSources;

    public DataSourcePreferences(PreferenceDialog parentController) {
        super(parentController);

        this.view = new DataSourcePreferencesView(this);
        this.editor = parentController.getEditor();

        // init view data
        this.dataSources = getDataSourceDomain().getDetailsMap(DBConnectionInfo.class);

        Object[] keys = dataSources.keySet().toArray();
        Arrays.sort(keys);
        DefaultComboBoxModel dataSourceModel = new DefaultComboBoxModel(keys);
        view.getDataSources().setModel(dataSourceModel);

        initBindings();

        // show first item
        if (keys.length > 0) {
            view.getDataSources().setSelectedIndex(0);
            editDataSourceAction();
        }
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);
        builder.bindToAction(view.getAddDataSource(), "newDataSourceAction()");
        builder
                .bindToAction(
                        view.getDuplicateDataSource(),
                        "duplicateDataSourceAction()");
        builder.bindToAction(view.getRemoveDataSource(), "removeDataSourceAction()");
        builder.bindToAction(view.getTestDataSource(), "testDataSourceAction()");

        builder.bindToComboSelection(view.getDataSources(), "dataSourceKey");
    }

    public Domain getDataSourceDomain() {
        return editor.editableInstance(getApplication().getPreferenceDomain());
    }

    public PreferenceEditor getEditor() {
        return editor;
    }

    public Map getDataSources() {
        return dataSources;
    }

    public String getDataSourceKey() {
        return dataSourceKey;
    }

    public void setDataSourceKey(String dataSourceKey) {
        this.dataSourceKey = dataSourceKey;
        editDataSourceAction();
    }

    public DBConnectionInfo getConnectionInfo() {
        return (DBConnectionInfo) dataSources.get(dataSourceKey);
    }

    /**
     * Shows a dialog to create new local DataSource configuration.
     */
    public void newDataSourceAction() {

        DataSourceCreator creatorWizard = new DataSourceCreator(this);
        DBConnectionInfo dataSource = creatorWizard.startupAction();

        if (dataSource != null) {
            dataSources.put(creatorWizard.getName(), dataSource);

            Object[] keys = dataSources.keySet().toArray();
            Arrays.sort(keys);
            view.getDataSources().setModel(new DefaultComboBoxModel(keys));
            view.getDataSources().setSelectedItem(creatorWizard.getName());
            editDataSourceAction();
        }
    }

    /**
     * Shows a dialog to duplicate an existing local DataSource configuration.
     */
    public void duplicateDataSourceAction() {
        Object selected = view.getDataSources().getSelectedItem();
        if (selected != null) {
            DataSourceDuplicator wizard = new DataSourceDuplicator(this, selected
                    .toString());
            DBConnectionInfo dataSource = wizard.startupAction();

            if (dataSource != null) {
                dataSources.put(wizard.getName(), dataSource);

                Object[] keys = dataSources.keySet().toArray();
                Arrays.sort(keys);
                view.getDataSources().setModel(new DefaultComboBoxModel(keys));
                view.getDataSources().setSelectedItem(wizard.getName());
                editDataSourceAction();
            }
        }
    }

    /**
     * Removes current DataSource.
     */
    public void removeDataSourceAction() {
        String key = getDataSourceKey();
        if (key != null) {
            editor.deleteDetail(getDataSourceDomain(), key);
            dataSources.remove(key);

            Object[] keys = dataSources.keySet().toArray();
            Arrays.sort(keys);
            view.getDataSources().setModel(new DefaultComboBoxModel(keys));
            editDataSourceAction(keys.length > 0 ? keys[0] : null);
        }
    }

    /**
     * Opens specified DataSource in the editor.
     */
    public void editDataSourceAction(Object dataSourceKey) {
        view.getDataSources().setSelectedItem(dataSourceKey);
        editDataSourceAction();
    }

    /**
     * Opens current DataSource in the editor.
     */
    public void editDataSourceAction() {
        this.view.getDataSourceEditor().setConnectionInfo(getConnectionInfo());
    }

    /**
     * Tries to establish a DB connection, reporting the status of this operation.
     */
    public void testDataSourceAction() {
        DBConnectionInfo currentDataSource = getConnectionInfo();
        if (currentDataSource == null) {
            return;
        }

        if (currentDataSource.getJdbcDriver() == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "No JDBC Driver specified",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (currentDataSource.getUrl() == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "No Database URL specified",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Class driverClass = getApplication().getClassLoadingService().loadClass(
                    currentDataSource.getJdbcDriver());
            Driver driver = (Driver) driverClass.newInstance();

            // connect via Cayenne DriverDataSource - it addresses some driver issues...
            Connection c = new DriverDataSource(
                    driver,
                    currentDataSource.getUrl(),
                    currentDataSource.getUserName(),
                    currentDataSource.getPassword()).getConnection();
            try {
                c.close();
            }
            catch (SQLException e) {
                // i guess we can ignore this...
            }

            JOptionPane.showMessageDialog(
                    null,
                    "Connected Successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Throwable th) {
            th = Util.unwindException(th);
            JOptionPane.showMessageDialog(null, "Error connecting to DB: "
                    + th.getLocalizedMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
    }

}