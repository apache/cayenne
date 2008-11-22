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

package org.apache.cayenne.modeler.dialog.pref;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cayenne.swing.control.FileChooser;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class TemplateCreatorView extends JDialog {

    protected JTextField templateName;
    protected FileChooser templateChooser;

    protected JButton okButton;
    protected JButton cancelButton;

    public TemplateCreatorView(JDialog parent) {
        super(parent, "New Template");

        this.templateName = new JTextField(20);
        this.templateChooser = new FileChooser("Select Template File", true, false);
        templateChooser.setExistingOnly(true);
        templateChooser.setColumns(20);
        this.okButton = new JButton("Create");
        this.cancelButton = new JButton("Cancel");

        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "right:pref, 3dlu, pref, 3dlu, pref:grow",
                "p, 3dlu, p"));
        builder.setDefaultDialogBorder();

        builder.addLabel("Name:", cc.xy(1, 1));
        builder.add(templateName, cc.xy(3, 1));
        builder.addLabel("Template File:", cc.xy(1, 3));
        builder.add(templateChooser, cc.xywh(3, 3, 3, 1));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(okButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getOkButton() {
        return okButton;
    }

    public JTextField getTemplateName() {
        return templateName;
    }

    public FileChooser getTemplateChooser() {
        return templateChooser;
    }
}
