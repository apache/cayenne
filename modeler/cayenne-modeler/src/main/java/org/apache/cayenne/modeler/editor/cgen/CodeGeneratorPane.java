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

package org.apache.cayenne.modeler.editor.cgen;

import org.apache.cayenne.swing.components.TopBorder;

import javax.swing.*;
import java.awt.*;

/**
 */
public class CodeGeneratorPane extends JSplitPane {

    protected JSplitPane splitPane;

    protected JButton generateButton;
    protected JLabel classesCount;

    public CodeGeneratorPane(Component generatorPanel, Component entitySelectorPanel) {
        super();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        this.generateButton = new JButton("Generate");
        this.classesCount = new JLabel("No classes selected");
        classesCount.setFont(classesCount.getFont().deriveFont(10f));

        JScrollPane scrollPane = new JScrollPane(
                generatorPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(800, 400));

        // assemble
        splitPane.setRightComponent(scrollPane);
        splitPane.setLeftComponent(entitySelectorPanel);

        JPanel messages = new JPanel(new BorderLayout());
        messages.add(classesCount, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(TopBorder.create());
        buttons.add(classesCount);
        buttons.add(Box.createHorizontalStrut(50));
        buttons.add(generateButton);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    public JButton getGenerateButton() {
        return generateButton;
    }

    public JLabel getClassesCount() {
        return classesCount;
    }
}
