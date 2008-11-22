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

import javax.swing.JOptionPane;

import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.pref.PreferenceEditor;
import org.apache.cayenne.swing.BindingBuilder;

/**
 */
public class DataSourceDuplicator extends CayenneController {

    protected DataSourceDuplicatorView view;
    protected PreferenceEditor editor;
    protected Domain domain;
    protected boolean canceled;
    protected Map dataSources;
    protected String prototypeKey;

    public DataSourceDuplicator(DataSourcePreferences parent, String prototypeKey) {
        super(parent);
        this.view = new DataSourceDuplicatorView("Create a copy of \""
                + prototypeKey
                + "\"");
        this.editor = parent.getEditor();
        this.domain = parent.getDataSourceDomain();
        this.dataSources = parent.getDataSources();
        this.prototypeKey = prototypeKey;

        String suggestion = prototypeKey + "0";
        for (int i = 1; i <= dataSources.size(); i++) {
            suggestion = prototypeKey + i;
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

        DBConnectionInfo prototype = (DBConnectionInfo) dataSources.get(prototypeKey);
        DBConnectionInfo dataSource = (DBConnectionInfo) editor.createDetail(
                domain,
                getName(),
                DBConnectionInfo.class);

        prototype.copyTo(dataSource);
        return dataSource;
    }

}
