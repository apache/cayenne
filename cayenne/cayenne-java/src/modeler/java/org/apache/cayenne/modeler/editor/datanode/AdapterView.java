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

package org.apache.cayenne.modeler.editor.datanode;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class AdapterView extends JPanel {

    protected JTextField customAdapter;

    public AdapterView() {
        this.customAdapter = new JTextField();

        // assemble

        DefaultFormBuilder topPanelBuilder = new DefaultFormBuilder(new FormLayout(
                "right:pref, 3dlu, fill:200dlu",
                ""));
        topPanelBuilder.setDefaultDialogBorder();

        topPanelBuilder.appendSeparator("DbAdapter Configuration");
        topPanelBuilder.append("Custom Adapter (optional):", customAdapter);

        setLayout(new BorderLayout());
        add(topPanelBuilder.getPanel(), BorderLayout.CENTER);
    }

    public JTextField getCustomAdapter() {
        return customAdapter;
    }
}
