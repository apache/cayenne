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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.WindowConstants;

import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;

/**
 * A subclass of ConnectionWizard that tests configured DataSource, but does not keep an
 * open connection.
 * 
 */
public class DataSourceWizard extends CayenneController {

    protected DataSourceWizardView view;

    protected DBConnectionInfo altDataSource;
    protected String altDataSourceKey;
    protected ObjectBinding dataSourceBinding;
    protected Map dataSources;

    protected String dataSourceKey;

    // this object is a clone of an object selected from the dropdown, as we need to allow
    // local temporary modifications
    protected DBConnectionInfo connectionInfo;

    protected boolean canceled;

    public DataSourceWizard(CayenneController parent, String title,
            String altDataSourceKey, DBConnectionInfo altDataSource) {
        super(parent);

        this.view = createView();
        this.view.setTitle(title);
        this.altDataSource = altDataSource;
        this.altDataSourceKey = altDataSourceKey;
        this.connectionInfo = new DBConnectionInfo();

        initBindings();
    }
    
    /**
     * Creates swing dialog for this wizard 
     */
    protected DataSourceWizardView createView() {
        return new DataSourceWizardView(this); 
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        dataSourceBinding = builder.bindToComboSelection(
                view.getDataSources(),
                "dataSourceKey");

        builder.bindToAction(view.getCancelButton(), "cancelAction()");
        builder.bindToAction(view.getOkButton(), "okAction()");
        builder.bindToAction(view.getConfigButton(), "dataSourceConfigAction()");
    }

    public String getDataSourceKey() {
        return dataSourceKey;
    }

    public void setDataSourceKey(String dataSourceKey) {
        this.dataSourceKey = dataSourceKey;

        // update a clone object that will be used to obtain connection...
        DBConnectionInfo currentInfo = (DBConnectionInfo) dataSources.get(dataSourceKey);
        if (currentInfo != null) {
            currentInfo.copyTo(connectionInfo);
        }
        else {
            connectionInfo = new DBConnectionInfo();
        }

        view.getConnectionInfo().setConnectionInfo(connectionInfo);
    }

    /**
     * Main action method that pops up a dialog asking for user selection. Returns true if
     * the selection was confirmed, false - if canceled.
     */
    public boolean startupAction() {
        this.canceled = true;

        refreshDataSources();

        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);

        return !canceled;
    }

    public DBConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    /**
     * Tests that the entered information is valid and can be used to open a conneciton.
     * Does not store the open connection.
     */
    public void okAction() {
        DBConnectionInfo info = getConnectionInfo();
        ClassLoadingService classLoader = getApplication().getClassLoadingService();

        // try making an adapter...
        try {
            info.makeAdapter(classLoader);
        }
        catch (Throwable th) {
            reportError("DbAdapter Error", th);
            return;
        }

        // doing connection testing...
        // attempt opening the connection, and close it right away
        try {
            Connection connection = info.makeDataSource(classLoader).getConnection();
            try {
                connection.close();
            }
            catch (SQLException ex) {
                // ignore close error
            }
        }
        catch (Throwable th) {
            reportError("Connection Error", th);
            return;
        }

        // set success flag, and unblock the caller...
        canceled = false;
        view.dispose();
    }

    public void cancelAction() {
        canceled = true;
        view.dispose();
    }

    /**
     * Opens preferences panel to allow configuration of DataSource presets.
     */
    public void dataSourceConfigAction() {
        PreferenceDialog prefs = new PreferenceDialog(this);
        prefs.showDataSourceEditorAction(dataSourceKey);
        refreshDataSources();
    }

    public Component getView() {
        return view;
    }

    protected void refreshDataSources() {
        this.dataSources = getApplication().getPreferenceDomain().getDetailsMap(
                DBConnectionInfo.class);

        // 1.2 migration fix - update data source adapter names
        Iterator it = dataSources.values().iterator();

        final String _12package = "org.objectstyle.cayenne.";
        while (it.hasNext()) {
            DBConnectionInfo info = (DBConnectionInfo) it.next();
            if (info.getDbAdapter() != null && info.getDbAdapter().startsWith(_12package)) {
                info.setDbAdapter("org.apache.cayenne."
                        + info.getDbAdapter().substring(_12package.length()));
                info.getObjectContext().commitChanges();
            }
        }

        if (altDataSourceKey != null
                && !dataSources.containsKey(altDataSourceKey)
                && altDataSource != null) {
            dataSources.put(altDataSourceKey, altDataSource);
        }

        Object[] keys = dataSources.keySet().toArray();
        Arrays.sort(keys);
        view.getDataSources().setModel(new DefaultComboBoxModel(keys));

        if (getDataSourceKey() == null) {
            String key = null;

            if (altDataSourceKey != null) {
                key = altDataSourceKey;
            }
            else if (keys.length > 0) {
                key = keys[0].toString();
            }

            setDataSourceKey(key);
            dataSourceBinding.updateView();
        }
    }
}
