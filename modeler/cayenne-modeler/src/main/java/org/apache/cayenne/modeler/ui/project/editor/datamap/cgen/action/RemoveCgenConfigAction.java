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
package org.apache.cayenne.modeler.ui.project.editor.datamap.cgen.action;

import org.apache.cayenne.gen.CgenConfigList;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.AppAction;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

public class RemoveCgenConfigAction extends AppAction {

    private final JComboBox<String> configurationsComboBox;
    private final Supplier<CgenConfigList> cgenConfigListProvider;
    private final Supplier<CgenConfiguration> cgenConfigurationProvider;

    public RemoveCgenConfigAction(Application application, JComboBox<String> configurationsComboBox,
                                  Supplier<CgenConfigList> cgenConfigListProvider,
                                  Supplier<CgenConfiguration> cgenConfigurationProvider) {
        super(application, "Remove Cgen Configuration", "Remove configuration");
        this.configurationsComboBox = configurationsComboBox;
        this.cgenConfigListProvider = cgenConfigListProvider;
        this.cgenConfigurationProvider = cgenConfigurationProvider;
        setAlwaysOn(true);
    }

    @Override
    public String getIconName() {
        return "icon-trash.png";
    }

    @Override
    public void performAction(ActionEvent e) {
        int result = JOptionPane.showConfirmDialog(app.getFrame(),
                "Configuration will be remove\n               Are you sure?",
                "Delete cgenConfiguration",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            if (configurationsComboBox.getItemCount() > 1) {
                CgenConfigList cgenConfigList = cgenConfigListProvider.get();
                cgenConfigList.removeByName(cgenConfigurationProvider.get().getName());
                configurationsComboBox.removeItem(configurationsComboBox.getSelectedItem());
                configurationsComboBox.setSelectedIndex(0);
            } else {
                JOptionPane.showMessageDialog(app.getFrame(), "At least one configuration must exist");
            }
        }
    }
}
