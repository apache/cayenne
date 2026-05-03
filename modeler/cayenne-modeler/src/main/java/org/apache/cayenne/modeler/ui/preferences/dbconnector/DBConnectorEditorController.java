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

package org.apache.cayenne.modeler.ui.preferences.dbconnector;

import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.mvc.RootController;
import org.apache.cayenne.modeler.dbconnector.DBConnector;

import java.awt.Component;

/**
 * A reusable editor for DBConnectionInfo object.
 */
public class DBConnectorEditorController extends ChildController<RootController> {

    private final DBConnectorEditorView view;
    private DBConnector connector;

    public DBConnectorEditorController(RootController parent) {
        super(parent);
        this.view = new DBConnectorEditorView(this);
    }

    @Override
    public Component getView() {
        return view;
    }

    public void setConnector(DBConnector connector) {
        this.connector = connector;
        if (connector == null) {
            view.clear();
        } else {
            view.showConnector(
                    connector.getUserName(),
                    connector.getPassword(),
                    connector.getJdbcDriver(),
                    connector.getUrl(),
                    connector.getDbAdapter());
        }
    }

    void userNameChanged(String v) {
        if (connector != null) connector.setUserName(v);
    }

    void passwordChanged(String v) {
        if (connector != null) connector.setPassword(v);
    }

    void driverChanged(String v) {
        if (connector != null) connector.setJdbcDriver(v);
    }

    void urlChanged(String v) {
        if (connector != null) connector.setUrl(v);
    }

    void adapterChanged(String v) {
        if (connector != null) connector.setDbAdapter(v);
    }
}
