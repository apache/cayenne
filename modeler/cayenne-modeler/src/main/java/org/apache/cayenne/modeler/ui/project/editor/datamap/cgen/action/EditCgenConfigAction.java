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

public class EditCgenConfigAction extends AppAction {

    private final JComboBox<String> configurationsComboBox;
    private final Supplier<CgenConfigList> cgenConfigListProvider;
    private final Supplier<CgenConfiguration> cgenConfigurationProvider;

    public EditCgenConfigAction(Application application, JComboBox<String> configurationsComboBox,
                                Supplier<CgenConfigList> cgenConfigListProvider,
                                Supplier<CgenConfiguration> cgenConfigurationProvider) {
        super(application, "Edit Cgen Configuration", "Rename configuration");
        this.configurationsComboBox = configurationsComboBox;
        this.cgenConfigListProvider = cgenConfigListProvider;
        this.cgenConfigurationProvider = cgenConfigurationProvider;
        setAlwaysOn(true);
    }

    @Override
    public String getIconName() {
        return "icon-edit.png";
    }

    @Override
    public void performAction(ActionEvent e) {
        String name = JOptionPane.showInputDialog(
                app.getFrame(),
                "Type the new name for cgenConfiguration",
                configurationsComboBox.getSelectedItem());
        if (name != null) {
            CgenConfigList cgenConfigList = cgenConfigListProvider.get();
            if (!cgenConfigList.isExist(name) && !name.isEmpty()) {
                cgenConfigurationProvider.get().setName(name);
                configurationsComboBox.removeItem(configurationsComboBox.getSelectedItem());
                configurationsComboBox.addItem(name);
                configurationsComboBox.setSelectedItem(name);
            } else {
                JOptionPane.showMessageDialog(app.getFrame(),
                        "Can't rename configuration, name is already exist or empty");
            }
        }
    }
}
