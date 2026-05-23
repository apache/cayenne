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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.dbconnector.DBConnector;
import org.apache.cayenne.modeler.toolkit.AppDialog;
import org.apache.cayenne.modeler.ui.preferences.dbconnector.DBConnectorPrefsPanel;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Map;

/**
 * Modal dialog asking the user to name a duplicate of an existing DB Connector.
 */
public class DBConnectorDuplicatorDialog extends AppDialog {

    private final DBConnectorPrefsPanel preferences;
    private final Map<String, DBConnector> connectors;
    private final String prototypeKey;

    private final JTextField connectorName;
    private final JButton okButton;
    private final JButton cancelButton;

    private String enteredName;
    private boolean cancelled = true;

    public DBConnectorDuplicatorDialog(Application app, Window owner, DBConnectorPrefsPanel preferences, String prototypeKey) {
        super(app, owner, "Create a copy of \"" + prototypeKey + "\"", ModalityType.APPLICATION_MODAL);
        this.preferences = preferences;
        this.connectors = preferences.getConnectors();
        this.prototypeKey = prototypeKey;

        this.connectorName = new JTextField(suggestName());
        this.okButton = new JButton("Create");
        this.cancelButton = new JButton("Cancel");

        initLayout();
        initBindings();
        setResizable(false);
    }

    /**
     * Pops up the dialog, blocks until closed, and returns the new connector — or
     * null if the user cancelled.
     */
    public DBConnector startupAction() {
        open();
        return createConnector();
    }

    public String getName() {
        return enteredName;
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

    private void initLayout() {
        getRootPane().setDefaultButton(okButton);

        FormLayout layout = new FormLayout("right:pref, 3dlu, fill:250", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.append("Name:", connectorName);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(okButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    private void initBindings() {
        cancelButton.addActionListener(e -> {
            cancelled = true;
            dispose();
        });
        okButton.addActionListener(e -> okClicked(connectorName.getText()));
    }

    private void okClicked(String name) {
        if (name == null || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter Connector Name", null, JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (connectors.containsKey(name)) {
            JOptionPane.showMessageDialog(this, "'" + name + "' is already in use, enter a different name", null, JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.enteredName = name;
        this.cancelled = false;
        dispose();
    }

    private DBConnector createConnector() {
        if (cancelled) {
            return null;
        }
        DBConnector prototype = connectors.get(prototypeKey);
        DBConnector connector = preferences.create(enteredName);
        prototype.copyTo(connector);
        return connector;
    }
}
