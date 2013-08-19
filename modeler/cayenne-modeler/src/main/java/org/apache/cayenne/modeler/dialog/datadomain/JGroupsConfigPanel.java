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

package org.apache.cayenne.modeler.dialog.datadomain;

import java.awt.BorderLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class JGroupsConfigPanel extends JPanel {

    protected JTextField multicastAddress;
    protected JTextField multicastPort;
    protected JTextField configURL;
    protected JRadioButton useDefaultConfig;
    protected JRadioButton useConfigFile;
    
    public JGroupsConfigPanel() {
        initView();
    }

    protected void initView() {
        setLayout(new BorderLayout());

        useDefaultConfig = new JRadioButton(CacheSyncConfigController.JGROUPS_DEFAULT_CONTROL);
        useDefaultConfig.setSelected(true);
        useConfigFile = new JRadioButton(CacheSyncConfigController.JGROUPS_URL_CONTROL);
        
        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(useConfigFile);
        radioGroup.add(useDefaultConfig);

        multicastAddress = new JTextField(20);
        multicastPort = new JTextField(5);
        configURL = new JTextField(20);

        // type form
        FormLayout layout = new FormLayout("right:150, 3dlu, left:200", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.appendSeparator("JavaGroups Settings");

        builder.append(useDefaultConfig);
        builder.nextLine();

        // "1" at the end would enforce spanning the text field to
        // the full width
        builder.append("Multicast Address:", multicastAddress, 1);
        builder.append("Multicast Port:", multicastPort);

        builder.nextLine();
        builder.append(useConfigFile);
        builder.nextLine();
        builder.append("JGroups Config File:", configURL, 1);

        add(builder.getPanel(), BorderLayout.NORTH);
    }

    public void showDefaultConfig() {
        multicastAddress.setEditable(true);
        multicastPort.setEditable(true);
        configURL.setEditable(false);
    }

    public void showCustomConfig() {
        multicastAddress.setEditable(false);
        multicastPort.setEditable(false);
        configURL.setEditable(true);
    }
    
    public JRadioButton getUseDefaultConfig() {
        return this.useDefaultConfig;
    }
    
    public void setUseDefaultConfig(JRadioButton button) {
        this.useDefaultConfig = button;
    }
    
    public JRadioButton getUseConfigFile() {
        return this.useConfigFile;
    }
}
