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
package org.apache.cayenne.modeler.editor;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.ModelerUtil;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * @since 4.1
 */
class GeneratorsPanel extends JPanel {

    private final DataMap dataMap;
    private final Class<?> type;
    private final String icon;

    private JCheckBox checkConfig;
    private JButton toConfigButton;

    GeneratorsPanel(DataMap dataMap, String icon, Class<?> type) {
        this.type = type;
        this.icon = icon;
        this.dataMap = dataMap;
        initView();
    }

    private void initView(){
        setLayout(new BorderLayout());
        FormLayout layout = new FormLayout(
                "left:pref, 4dlu, fill:70dlu, 3dlu, fill:120, 3dlu, fill:120", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        this.checkConfig = new JCheckBox();
        JLabel dataMapLabel = new JLabel(dataMap.getName());
        dataMapLabel.setToolTipText(dataMap.getName());
        DataChannelMetaData metaData = Application.getInstance().getMetaData();
        this.toConfigButton = new JButton("Edit Config");
        if(metaData.get(dataMap, type) == null) {
            if(type == ReverseEngineering.class) {
                checkConfig.setEnabled(false);
            }
        }
        this.toConfigButton.setIcon(ModelerUtil.buildIcon(icon));
        builder.append(checkConfig, dataMapLabel, toConfigButton);
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    JCheckBox getCheckConfig() {
        return checkConfig;
    }

    JButton getToConfigButton() {
        return toConfigButton;
    }

    DataMap getDataMap() {
        return dataMap;
    }
}
