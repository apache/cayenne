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

import java.sql.Connection;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.apache.cayenne.access.reveng.NamingStrategy;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.modeler.ModelerPreferences;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.CayenneController;

/**
 * A component for choosing a DataSource. Users can choose from the DataSources configured
 * in preferences, and one extra set of connection settings. This object will create and
 * keep open a JDBC connection. It is caller responsibility to dispose of it properly.
 * 
 */
// TODO: after refactoring DbLoader to accept a DataSource instead of connection this
// dialog should be merged with superclass - DataSourceWizard.
public class ConnectionWizard extends DataSourceWizard {
    /**
     * Preference to store latest strategies
     */
    private static final String STRATEGIES_PREFERENCE = "recent.preferences";
    
    /**
     * Naming strategies to appear in combobox by default
     */
    private static final Vector<String> PREDEFINED_STRATEGIES = new Vector<String>();
    static {
        PREDEFINED_STRATEGIES.add("org.apache.cayenne.access.reveng.BasicNamingStrategy");
        PREDEFINED_STRATEGIES.add("org.apache.cayenne.modeler.util.SmartNamingStrategy");
    };

    protected Connection connection;
    protected DbAdapter adapter;
    
    protected NamingStrategy strategy;

    public ConnectionWizard(CayenneController parent, String title,
            String altDataSourceKey, DBConnectionInfo altDataSource) {
        super(parent, title, altDataSourceKey, altDataSource);
    }
    
    protected DataSourceWizardView createView() {
        ConnectionWizardView view = new ConnectionWizardView(this);
        
        ModelerPreferences pref = ModelerPreferences.getPreferences();
        Vector<?> arr = pref.getVector(STRATEGIES_PREFERENCE, PREDEFINED_STRATEGIES);
        
        JComboBox strategyCombo = view.getStrategyComboBox();
        strategyCombo.setModel(new DefaultComboBoxModel(arr));
        
        return view;
    }

    /**
     * Overrides superclass to keep an open connection around for the caller's use.
     */
    public void okAction() {
        // build connection and adapter...

        DBConnectionInfo info = getConnectionInfo();
        ClassLoadingService classLoader = getApplication().getClassLoadingService();

        try {
            this.adapter = info.makeAdapter(classLoader);
        }
        catch (Throwable th) {
            reportError("DbAdapter Error", th);
            return;
        }

        try {
            this.connection = info.makeDataSource(classLoader).getConnection();
        }
        catch (Throwable th) {
            reportError("Connection Error", th);
            return;
        }
        
        try {
            String strategyClass = (String) 
                ((ConnectionWizardView) view).getStrategyComboBox().getSelectedItem();
            
            this.strategy = (NamingStrategy) classLoader.loadClass(strategyClass).newInstance();
            
            /**
             * Be user-friendly and update preferences with specified strategy
             */
            ModelerPreferences pref = ModelerPreferences.getPreferences();
            Vector arr = pref.getVector(STRATEGIES_PREFERENCE, PREDEFINED_STRATEGIES);
            
            //move to top
            arr.remove(strategyClass);
            arr.add(0, strategyClass);
            
            pref.setProperty(STRATEGIES_PREFERENCE, arr);
        }
        catch (Throwable th) {
            reportError("Naming Strategy Initialization Error", 
                    new Exception("Naming Strategy Initialization Error: " + th.getMessage()));
            return;
        }

        // set success flag, and unblock the caller...
        canceled = false;
        view.dispose();
    }

    /**
     * Returns configured DB connection.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Returns configured DbAdapter.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }
    
    /**
     * Returns configured naming strategy
     */
    public NamingStrategy getNamingStrategy() {
        return strategy;
    }
}
