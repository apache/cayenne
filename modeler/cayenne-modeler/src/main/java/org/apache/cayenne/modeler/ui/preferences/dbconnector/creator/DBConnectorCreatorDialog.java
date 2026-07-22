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

package org.apache.cayenne.modeler.ui.preferences.dbconnector.creator;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.dbconnector.DBConnector;
import org.apache.cayenne.modeler.toolkit.AppDialog;
import org.apache.cayenne.modeler.ui.preferences.dbconnector.DBConnectorPrefsPanel;
import org.apache.cayenne.modeler.dbadapter.DbAdapters;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Map;

/**
 * Modal dialog for creating a new DB Connector entry — collects the name and an optional
 * adapter, defers the actual creation to {@link DBConnectorPrefsPanel#create(String)}.
 */
public class DBConnectorCreatorDialog extends AppDialog {

    private static final String NAME_PREFIX = "Connector";
    private static final String NO_ADAPTER = "Custom / Undefined";

    private final DBConnectorPrefsPanel preferences;
    private final Map<String, DBConnector> connectors;

    private final JTextField connectorName;
    private final JComboBox<String> adapters;
    private final JButton okButton;
    private final JButton cancelButton;

    private String enteredName;
    private String enteredAdapter;
    private boolean cancelled = true;

    public DBConnectorCreatorDialog(Application app, Window owner, DBConnectorPrefsPanel preferences) {
        super(app, owner, "Create New DB Connector", ModalityType.APPLICATION_MODAL);
        this.preferences = preferences;
        this.connectors = preferences.getConnectors();

        this.connectorName = new JTextField(suggestName());

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(DbAdapters.standardAdapters());
        model.insertElementAt(NO_ADAPTER, 0);
        this.adapters = new JComboBox<>(model);
        this.adapters.setSelectedIndex(0);

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
        for (int i = 1; i <= connectors.size() + 1; i++) {
            String candidate = NAME_PREFIX + i;
            if (!connectors.containsKey(candidate)) {
                return candidate;
            }
        }
        return NAME_PREFIX + (connectors.size() + 1);
    }

    private void initLayout() {
        getRootPane().setDefaultButton(okButton);

        FormLayout layout = new FormLayout(
                "right:pref, $lcgap, fill:max(50dlu;pref):grow",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.append("Name:", connectorName);
        builder.append("Adapter:", adapters);

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
        okButton.addActionListener(e -> {
            Object adapter = adapters.getSelectedItem();
            okClicked(connectorName.getText(), NO_ADAPTER.equals(adapter) ? null : (String) adapter);
        });
    }

    private void okClicked(String name, String adapter) {
        if (name == null || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter Connector Name", null, JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (connectors.containsKey(name)) {
            JOptionPane.showMessageDialog(this, "'" + name + "' is already in use, enter a different name", null, JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.enteredName = name;
        this.enteredAdapter = adapter;
        this.cancelled = false;
        dispose();
    }

    private DBConnector createConnector() {
        if (cancelled) {
            return null;
        }
        DBConnector connector = preferences.create(enteredName);
        if (enteredAdapter != null) {
            connector.setDbAdapter(enteredAdapter);
            connector.setJdbcDriver(Adapters.driver(enteredAdapter));
            connector.setUrl(Adapters.jdbcURL(enteredAdapter));
        }
        return connector;
    }
}
