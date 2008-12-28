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

import javax.swing.DefaultComboBoxModel;

import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.DbAdapterInfo;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;

/**
 * A reusable editor for DBConnectionInfo object.
 * 
 */
public class DBConnectionInfoEditor extends CayenneController {

    // transient placeholder to display disabled form
    private static final DBConnectionInfo emptyInfo = new DBConnectionInfo();

    protected DBConnectionInfoEditorView view;
    protected DBConnectionInfo connectionInfo;
    protected ObjectBinding[] bindings;

    public DBConnectionInfoEditor(CayenneController parent) {
        super(parent);

        this.view = new DBConnectionInfoEditorView();
        initBindings();
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        this.view.setEnabled(false);

        DefaultComboBoxModel adapterModel = new DefaultComboBoxModel(DbAdapterInfo
                .getStandardAdapters());
        view.getAdapters().setModel(adapterModel);
        view.getAdapters().setSelectedIndex(0);

        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        bindings = new ObjectBinding[5];

        bindings[0] = builder.bindToTextField(
                view.getUserName(),
                "connectionInfo.userName");
        bindings[1] = builder.bindToTextField(
                view.getPassword(),
                "connectionInfo.password");
        bindings[2] = builder.bindToTextField(
                view.getDriver(),
                "connectionInfo.jdbcDriver");
        bindings[3] = builder.bindToTextField(view.getUrl(), "connectionInfo.url");

        bindings[4] = builder.bindToComboSelection(
                view.getAdapters(),
                "connectionInfo.dbAdapter", "Automatic");
    }

    public DBConnectionInfo getConnectionInfo() {
        return connectionInfo != null ? connectionInfo : emptyInfo;
    }

    public void setConnectionInfo(DBConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
        refreshView();
    }

    protected void refreshView() {
        getView().setEnabled(connectionInfo != null);

        for (ObjectBinding binding : bindings) {
            binding.updateView();
        }
    }
}
