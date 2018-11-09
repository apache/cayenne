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

package org.apache.cayenne.modeler.editor.cgen.domain;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.ModelerUtil;

import javax.swing.*;
import java.awt.*;

public class CgenPanel extends JPanel {

    private JCheckBox checkConfig;
    private JLabel dataMapLabel;
    private JButton toConfigButton;
    private DataMap dataMap;

    public CgenPanel(DataMap dataMap) {
        setLayout(new BorderLayout());
        FormLayout layout = new FormLayout(
                "left:pref, 4dlu, fill:50dlu, 3dlu, fill:120", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        this.dataMap = dataMap;
        this.checkConfig = new JCheckBox();
        this.dataMapLabel = new JLabel(dataMap.getName());
        DataChannelMetaData metaData = Application.getInstance().getMetaData();
        this.toConfigButton = new JButton();
        if(metaData.get(dataMap, CgenConfiguration.class) != null) {
            this.toConfigButton.setText("Edit Config");
        } else {
            this.toConfigButton.setText("Create Config");
        }
        this.toConfigButton.setIcon(ModelerUtil.buildIcon("icon-datamap.png"));

        builder.append(checkConfig, dataMapLabel, toConfigButton);
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public JCheckBox getCheckConfig() {
        return checkConfig;
    }

    public JButton getToConfigButton() {
        return toConfigButton;
    }

    public JLabel getDataMapLabel() {
        return dataMapLabel;
    }

    public DataMap getDataMap() {
        return dataMap;
    }
}
