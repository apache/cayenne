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

package org.apache.cayenne.modeler.ui.preferences.datasource;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.toolkit.text.CMPasswordField;
import org.apache.cayenne.modeler.toolkit.text.CMTextField;

/**
 * A generic panel for entering DataSource information.
 */
public class DBConnectionInfoEditorView extends JPanel {

    protected JComboBox adapters;
    protected CMTextField driver;
    protected CMTextField url;
    protected CMTextField userName;
    protected CMPasswordField password;

    protected Collection<JLabel> labels;

    protected DefaultFormBuilder builder;

    public DBConnectionInfoEditorView() {
        adapters = new JComboBox();
        adapters.setEditable(true);

        driver = new CMTextField();
        url = new CMTextField();
        userName = new CMTextField();
        password = new CMPasswordField();
        labels = new ArrayList<>();

        // assemble
        FormLayout layout = new FormLayout("right:pref, 3dlu, fill:160dlu:grow", "");
        builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        labels.add(builder.append("JDBC Driver:", driver));
        labels.add(builder.append("DB URL:", url));
        labels.add(builder.append("User Name:", userName));
        labels.add(builder.append("Password:", password));
        labels.add(builder.append("Adapter (optional):", adapters));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public JComboBox getAdapters() {
        return adapters;
    }

    public CMTextField getDriver() {
        return driver;
    }

    public CMPasswordField getPassword() {
        return password;
    }

    public CMTextField getUrl() {
        return url;
    }

    public CMTextField getUserName() {
        return userName;
    }

    /**
     * @return Builder of the view (to allow dynamic extending of the component)
     */
    public DefaultFormBuilder getBuilder() {
        return builder;
    }

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
