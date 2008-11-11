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

import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.SRadioButton;
import org.scopemvc.view.swing.STextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 */
public class JGroupsConfigPanel extends SPanel {

    protected STextField multicastAddress;
    protected STextField multicastPort;
    protected STextField configURL;

    public JGroupsConfigPanel() {
        initView();
    }

    protected void initView() {
        setLayout(new BorderLayout());

        SRadioButton useDefaultConfig =
            new SRadioButton(
                CacheSyncConfigController.JGROUPS_DEFAULT_CONTROL,
                JGroupsConfigModel.USING_DEFAULT_CONFIG_SELECTOR);

        SRadioButton useConfigFile =
            new SRadioButton(
                CacheSyncConfigController.JGROUPS_URL_CONTROL,
                JGroupsConfigModel.USING_CONFIG_FILE_SELECTOR);

        ButtonGroup group = new ButtonGroup();
        group.add(useConfigFile);
        group.add(useDefaultConfig);

        multicastAddress = new STextField();
        multicastAddress.setSelector(JGroupsConfigModel.MCAST_ADDRESS_SELECTOR);

        multicastPort = new STextField(5);
        multicastPort.setSelector(JGroupsConfigModel.MCAST_PORT_SELECTOR);

        configURL = new STextField();
        configURL.setSelector(JGroupsConfigModel.JGROUPS_CONFIG_URL_SELECTOR);

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
}
