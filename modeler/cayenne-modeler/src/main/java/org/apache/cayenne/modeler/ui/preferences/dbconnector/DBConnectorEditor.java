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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.dbconnector.DBConnector;
import org.apache.cayenne.modeler.toolkit.text.CMPasswordField;
import org.apache.cayenne.modeler.toolkit.text.CMTextField;
import org.apache.cayenne.modeler.util.DbAdapterInfo;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Reusable panel for editing a {@link DBConnector} (driver, URL, credentials, adapter).
 * The bound connector is set via {@link #setConnector(DBConnector)}; field edits are
 * pushed back into it on commit.
 */
public class DBConnectorEditor extends JPanel {

    private static final String AUTOMATIC = "Automatic";

    private final JComboBox<String> adapters;
    private final CMTextField driver;
    private final CMTextField url;
    private final CMTextField userName;
    private final CMPasswordField password;

    private final Collection<JLabel> labels;

    private DBConnector connector;

    public DBConnectorEditor() {
        this.adapters = new JComboBox<>();
        this.driver = new CMTextField();
        this.url = new CMTextField();
        this.userName = new CMTextField();
        this.password = new CMPasswordField();
        this.labels = new ArrayList<>();

        adapters.setEditable(true);
        adapters.setModel(new DefaultComboBoxModel<>(DbAdapterInfo.getStandardAdapters()));
        adapters.setSelectedIndex(0);

        initLayout();
        initBindings();
        setEnabled(false);
    }

    public void setConnector(DBConnector connector) {
        this.connector = connector;
        if (connector == null) {
            clear();
        } else {
            showConnector(
                    connector.getUserName(),
                    connector.getPassword(),
                    connector.getJdbcDriver(),
                    connector.getUrl(),
                    connector.getDbAdapter());
        }
    }

    private void initLayout() {
        FormLayout layout = new FormLayout("right:pref, 3dlu, fill:160dlu:grow", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        labels.add(builder.append("JDBC Driver:", driver));
        labels.add(builder.append("DB URL:", url));
        labels.add(builder.append("User Name:", userName));
        labels.add(builder.append("Password:", password));
        labels.add(builder.append("Adapter (optional):", adapters));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initBindings() {
        userName.addCommitListener(v -> { if (connector != null) connector.setUserName(v); });
        password.addCommitListener(v -> { if (connector != null) connector.setPassword(v); });
        driver.addCommitListener(v -> { if (connector != null) connector.setJdbcDriver(v); });
        url.addCommitListener(v -> { if (connector != null) connector.setUrl(v); });
        adapters.addActionListener(e -> {
            if (connector == null) return;
            Object sel = adapters.getSelectedItem();
            connector.setDbAdapter(AUTOMATIC.equals(sel) ? null : (String) sel);
        });
    }

    private void showConnector(String userName, String password, String driver, String url, String adapter) {
        this.userName.setText(userName != null ? userName : "");
        this.password.setText(password != null ? password : "");
        this.driver.setText(driver != null ? driver : "");
        this.url.setText(url != null ? url : "");
        this.adapters.setSelectedItem(adapter != null ? adapter : AUTOMATIC);
        setEnabled(true);
    }

    private void clear() {
        userName.setText("");
        password.setText("");
        driver.setText("");
        url.setText("");
        adapters.setSelectedItem(AUTOMATIC);
        setEnabled(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            super.setEnabled(enabled);
            for (JLabel label : labels) {
                label.setEnabled(enabled);
            }
            adapters.setEnabled(enabled);
            driver.setEnabled(enabled);
            url.setEnabled(enabled);
            userName.setEnabled(enabled);
            password.setEnabled(enabled);
        }
    }
}
