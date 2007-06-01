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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;

import org.objectstyle.cayenne.modeler.ClassLoadingService;
import org.objectstyle.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.objectstyle.cayenne.modeler.pref.DBConnectionInfo;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.swing.BindingBuilder;
import org.objectstyle.cayenne.swing.ObjectBinding;

/**
 * A subclass of ConnectionWizard that tests configured DataSource, but does not keep an
 * open connection.
 * 
 * @author Andrei Adamchik
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

        this.view = new DataSourceWizardView(this);
        this.view.setTitle(title);
        this.altDataSource = altDataSource;
        this.altDataSourceKey = altDataSourceKey;
        this.connectionInfo = new DBConnectionInfo();

        initBindings();
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
        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.show();

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
        prefs.startupAction();

        refreshDataSources();
    }

    public Component getView() {
        return view;
    }

    protected void refreshDataSources() {
        this.dataSources = getApplication().getPreferenceDomain().getDetailsMap(
                DBConnectionInfo.class);

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