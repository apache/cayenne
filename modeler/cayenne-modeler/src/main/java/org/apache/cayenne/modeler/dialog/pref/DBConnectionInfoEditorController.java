/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.dialog.pref;

import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.mvc.RootController;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.DbAdapterInfo;
import org.apache.cayenne.modeler.util.TextBinder;

import javax.swing.*;
import java.awt.*;

/**
 * A reusable editor for DBConnectionInfo object.
 */
public class DBConnectionInfoEditorController extends ChildController<RootController> {

    // transient placeholder to display disabled form
    private static final DBConnectionInfo emptyInfo = new DBConnectionInfo();

    protected DBConnectionInfoEditorView view;
    protected DBConnectionInfo connectionInfo;

    public DBConnectionInfoEditorController(RootController parent) {
        super(parent);

        this.view = new DBConnectionInfoEditorView();
        this.view.setEnabled(false);

        DefaultComboBoxModel adapterModel = new DefaultComboBoxModel(DbAdapterInfo.getStandardAdapters());
        view.getAdapters().setModel(adapterModel);
        view.getAdapters().setSelectedIndex(0);

        TextBinder.bind(view.getUserName(), v -> {
            DBConnectionInfo ci = connectionInfo;
            if (ci != null) ci.setUserName(v);
        });
        TextBinder.bind(view.getPassword(), v -> {
            DBConnectionInfo ci = connectionInfo;
            if (ci != null) ci.setPassword(v);
        });
        TextBinder.bind(view.getDriver(), v -> {
            DBConnectionInfo ci = connectionInfo;
            if (ci != null) ci.setJdbcDriver(v);
        });
        TextBinder.bind(view.getUrl(), v -> {
            DBConnectionInfo ci = connectionInfo;
            if (ci != null) ci.setUrl(v);
        });

        view.getAdapters().addActionListener(e -> {
            DBConnectionInfo ci = connectionInfo;
            if (ci != null) {
                Object sel = view.getAdapters().getSelectedItem();
                ci.setDbAdapter("Automatic".equals(sel) ? null : (String) sel);
            }
        });
    }

    @Override
    public Component getView() {
        return view;
    }

    public void setConnectionInfo(DBConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;

        view.setEnabled(connectionInfo != null);

        DBConnectionInfo ci = connectionInfo != null ? connectionInfo : emptyInfo;
        view.getUserName().setText(ci.getUserName());
        view.getPassword().setText(ci.getPassword());
        view.getDriver().setText(ci.getJdbcDriver());
        view.getUrl().setText(ci.getUrl());
        view.getAdapters().setSelectedItem(ci.getDbAdapter() != null ? ci.getDbAdapter() : "Automatic");
    }
}
