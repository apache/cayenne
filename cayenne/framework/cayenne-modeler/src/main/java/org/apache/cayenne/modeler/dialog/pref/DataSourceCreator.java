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

package org.apache.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.AdapterMapping;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.DbAdapterInfo;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.pref.PreferenceEditor;
import org.apache.cayenne.swing.BindingBuilder;

/**
 */
public class DataSourceCreator extends CayenneController {

    private static final String NO_ADAPTER = "Custom / Undefined";

    protected DataSourceCreatorView view;
    protected PreferenceEditor editor;
    protected Domain domain;
    protected boolean canceled;
    protected Map dataSources;

    public DataSourceCreator(DataSourcePreferences parent) {
        super(parent);
        this.view = new DataSourceCreatorView((JDialog) SwingUtilities
                .getWindowAncestor(parent.getView()));
        this.editor = parent.getEditor();
        this.domain = parent.getDataSourceDomain();
        this.dataSources = parent.getDataSources();

        DefaultComboBoxModel model = new DefaultComboBoxModel(DbAdapterInfo
                .getStandardAdapters());
        model.insertElementAt(NO_ADAPTER, 0);
        this.view.getAdapters().setModel(model);
        this.view.getAdapters().setSelectedIndex(0);

        String suggestion = "DataSource0";
        for (int i = 1; i <= dataSources.size(); i++) {
            suggestion = "DataSource" + i;
            if (!dataSources.containsKey(suggestion)) {
                break;
            }
        }

        this.view.getDataSourceName().setText(suggestion);
        initBindings();
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);
        builder.bindToAction(view.getCancelButton(), "cancelAction()");
        builder.bindToAction(view.getOkButton(), "okAction()");
    }

    public void okAction() {
        if (getName() == null) {
            JOptionPane.showMessageDialog(
                    view,
                    "Enter DataSource Name",
                    null,
                    JOptionPane.WARNING_MESSAGE);
        }
        else if (dataSources.containsKey(getName())) {
            JOptionPane.showMessageDialog(
                    view,
                    "'" + getName() + "' is already in use, enter a different name",
                    null,
                    JOptionPane.WARNING_MESSAGE);
        }
        else {
            canceled = false;
            view.dispose();
        }
    }

    public void cancelAction() {
        canceled = true;
        view.dispose();
    }

    /**
     * Pops up a dialog and blocks current thread until the dialog is closed.
     */
    public DBConnectionInfo startupAction() {
        // this should handle closing via ESC
        canceled = true;

        view.setModal(true);
        view.pack();
        view.setResizable(false);
        makeCloseableOnEscape();
        centerView();

        view.setVisible(true);
        return createDataSource();
    }

    public String getName() {
        String name = view.getDataSourceName().getText();
        return (name.length() > 0) ? name : null;
    }

    protected DBConnectionInfo createDataSource() {
        if (canceled) {
            return null;
        }

        DBConnectionInfo dataSource = (DBConnectionInfo) editor.createDetail(
                domain,
                getName(),
                DBConnectionInfo.class);

        Object adapter = view.getAdapters().getSelectedItem();
        if (NO_ADAPTER.equals(adapter)) {
            adapter = null;
        }

        if (adapter != null) {
            String adapterString = adapter.toString();
            dataSource.setDbAdapter(adapterString);

            // guess adapter defaults...
            AdapterMapping defaultMap = getApplication().getAdapterMapping();
            dataSource.setJdbcDriver(defaultMap.jdbcDriverForAdapter(adapterString));
            dataSource.setUrl(defaultMap.jdbcURLForAdapter(adapterString));
        }

        return dataSource;
    }
}
