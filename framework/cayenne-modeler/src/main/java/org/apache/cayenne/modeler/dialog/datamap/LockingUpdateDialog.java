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

package org.apache.cayenne.modeler.dialog.datamap;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class LockingUpdateDialog extends JDialog {

    protected JCheckBox entities;
    protected JCheckBox attributes;
    protected JCheckBox relationships;

    protected JButton cancelButton;
    protected JButton updateButton;

    public LockingUpdateDialog() {

        this.entities = new JCheckBox("Update all Entities");
        this.attributes = new JCheckBox("Update all Attributes");
        this.relationships = new JCheckBox("Update all Relationships");

        this.cancelButton = new JButton("Cancel");
        this.updateButton = new JButton("Update");

        // check all by default until we start storing this in preferences.
        entities.setSelected(true);
        attributes.setSelected(true);
        relationships.setSelected(true);

        // do layout...

        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "left:max(180dlu;pref)",
                "p, 3dlu, p, 3dlu, p, 3dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.add(entities, cc.xy(1, 1));
        builder.add(attributes, cc.xy(1, 3));
        builder.add(relationships, cc.xy(1, 5));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(updateButton);
        buttonPanel.add(cancelButton);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(builder.getPanel(), BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getUpdateButton() {
        return updateButton;
    }

    public JCheckBox getAttributes() {
        return attributes;
    }

    public JCheckBox getEntities() {
        return entities;
    }

    public JCheckBox getRelationships() {
        return relationships;
    }
}
