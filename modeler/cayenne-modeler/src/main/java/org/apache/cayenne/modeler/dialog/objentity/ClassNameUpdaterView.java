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


package org.apache.cayenne.modeler.dialog.objentity;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class ClassNameUpdaterView extends JDialog {

    protected JCheckBox clientClass;
    protected JCheckBox serverClass;

    protected JButton updateButton;
    protected JButton cancelButton;

    public ClassNameUpdaterView() {

        serverClass = new JCheckBox();
        clientClass = new JCheckBox();

        // make invisible by default
        serverClass.setVisible(false);
        clientClass.setVisible(false);

        updateButton = new JButton("Update");
        cancelButton = new JButton("Cancel");

        // assemble

        FormLayout layout = new FormLayout("left:200dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append("Update class name to match current entity name?");
        builder.append(serverClass);
        builder.append(clientClass);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(updateButton);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(builder.getPanel(), BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);

        setTitle("Update Entity Class Name");
    }

    public JCheckBox getClientClass() {
        return clientClass;
    }

    public JCheckBox getServerClass() {
        return serverClass;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getUpdateButton() {
        return updateButton;
    }
}
