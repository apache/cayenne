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

package org.apache.cayenne.modeler.ui.preferences.dbconnector.duplicator;

import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.ui.preferences.dbconnector.DBConnectorPreferencesController;
import org.apache.cayenne.modeler.dbconnector.DBConnector;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Map;


public class DBConnectorDuplicatorController extends ChildController<DBConnectorPreferencesController> {

    protected final DBConnectorDuplicatorView view;
    protected final Map<String, DBConnector> connectors;
    protected final String prototypeKey;
    protected boolean canceled;

    private String enteredName;

    public DBConnectorDuplicatorController(DBConnectorPreferencesController parent, String prototypeKey) {
        super(parent);
        this.connectors = parent.getConnectors();
        this.prototypeKey = prototypeKey;
        this.view = new DBConnectorDuplicatorView(
                "Create a copy of \"" + prototypeKey + "\"",
                this,
                suggestName());
    }

    private String suggestName() {
        String suggestion = prototypeKey + "0";
        for (int i = 1; i <= connectors.size(); i++) {
            suggestion = prototypeKey + i;
            if (!connectors.containsKey(suggestion)) {
                break;
            }
        }
        return suggestion;
    }

    public Component getView() {
        return view;
    }

    void okClicked(String name) {
        if (name == null || name.isEmpty()) {
            JOptionPane.showMessageDialog(
                    view,
                    "Enter Connector Name",
                    null,
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (connectors.containsKey(name)) {
            JOptionPane.showMessageDialog(
                    view,
                    "'" + name + "' is already in use, enter a different name",
                    null,
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.enteredName = name;
        this.canceled = false;
        view.dispose();
    }

    void cancelClicked() {
        canceled = true;
        view.dispose();
    }

    /**
     * Pops up a dialog and blocks current thread until the dialog is closed.
     */
    public DBConnector startupAction() {
        // this should handle closing via ESC
        canceled = true;

        view.setModal(true);
        view.pack();
        view.setResizable(false);
        makeCloseableOnEscape();
        centerView();

        view.setVisible(true);
        return createConnector();
    }

    public String getName() {
        return enteredName;
    }

    protected DBConnector createConnector() {
        if (canceled) {
            return null;
        }

        DBConnector prototype = connectors.get(prototypeKey);
        DBConnector connector = parent.create(enteredName);

        prototype.copyTo(connector);
        return connector;
    }

}
