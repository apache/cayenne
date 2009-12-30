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

package org.apache.cayenne.modeler.dialog.codegen;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.apache.cayenne.modeler.Application;

/**
 */
public class CodeGeneratorDialog extends JDialog {

    protected JTabbedPane tabs;

    protected JButton generateButton;
    protected JButton cancelButton;
    protected JLabel classesCount;

    public CodeGeneratorDialog(Component generatorPanel, Component entitySelectorPanel) {
        super(Application.getFrame());
        
        this.tabs = new JTabbedPane(SwingConstants.TOP);
        this.generateButton = new JButton("Generate");
        this.cancelButton = new JButton("Cancel");
        this.classesCount = new JLabel("No classes selected");
        classesCount.setFont(classesCount.getFont().deriveFont(10f));

        // assemble

        tabs.addTab("Code Generator", generatorPanel);
        tabs.addTab("Classes", entitySelectorPanel);

        JPanel messages = new JPanel(new BorderLayout());
        messages.add(classesCount, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(classesCount);
        buttons.add(Box.createHorizontalStrut(50));
        buttons.add(cancelButton);
        buttons.add(generateButton);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(tabs, BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);

        setTitle("Code Generation");
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getGenerateButton() {
        return generateButton;
    }

    public JLabel getClassesCount() {
        return classesCount;
    }
}
