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

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.swing.components.TopBorder;

import javax.swing.*;
import java.awt.*;

/**
 */
public class CodeGeneratorDialog extends JDialog {

    private JButton generateButton;
    protected JButton cancelButton;
    private JLabel classesCount;

    CodeGeneratorDialog(Component generatorPanel, Component entitySelectorPanel) {
        super(Application.getFrame());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setFocusable(false);

        this.generateButton = new JButton("Generate");
        getRootPane().setDefaultButton(generateButton);

        this.cancelButton = new JButton("Cancel");
        this.classesCount = new JLabel("No classes selected");
        classesCount.setFont(classesCount.getFont().deriveFont(10f));


        JScrollPane scrollPane = new JScrollPane(
                generatorPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(630, 500));

        splitPane.setLeftComponent(entitySelectorPanel);
        splitPane.setRightComponent(scrollPane);

        JPanel messages = new JPanel(new BorderLayout());
        messages.add(classesCount, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(TopBorder.create());
        buttons.add(classesCount);
        buttons.add(Box.createHorizontalStrut(50));
        buttons.add(cancelButton);
        buttons.add(generateButton);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(splitPane, BorderLayout.CENTER);
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
