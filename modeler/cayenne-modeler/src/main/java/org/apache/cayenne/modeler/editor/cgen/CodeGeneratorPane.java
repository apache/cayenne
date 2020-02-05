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

package org.apache.cayenne.modeler.editor.cgen;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.util.ModelerUtil;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * @since 4.1
 */
public class CodeGeneratorPane extends JPanel {

    private JButton generateButton;
    private JCheckBox checkAll;
    private JLabel checkAllLabel;

    public CodeGeneratorPane(Component generatorPanel, Component entitySelectorPanel) {
        super();
        this.setLayout(new BorderLayout());

        JPanel toolBarPanel = new JPanel();
        toolBarPanel.setLayout(new BorderLayout());

        FormLayout layout = new FormLayout(
                "fill:110", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        this.generateButton = new JButton("Generate");
        generateButton.setIcon(ModelerUtil.buildIcon("icon-gen_java.png"));
        generateButton.setEnabled(false);
        builder.append(generateButton);
        toolBarPanel.add(builder.getPanel(), BorderLayout.EAST);

        this.checkAll = new JCheckBox();
        this.checkAllLabel = new JLabel("Check All Classes");
        checkAll.addItemListener(event -> {
            if (checkAll.isSelected()) {
                checkAllLabel.setText("Uncheck All Classess");
            } else {
                checkAllLabel.setText("Check All Classes");
            }
        });
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        topPanel.add(checkAll);
        topPanel.add(checkAllLabel);
        toolBarPanel.add(topPanel, BorderLayout.WEST);

        add(toolBarPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JScrollPane scrollPane = new JScrollPane(
                generatorPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(150, 400));

        // assemble
        splitPane.setRightComponent(scrollPane);
        splitPane.setLeftComponent(entitySelectorPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    public JButton getGenerateButton() {
        return generateButton;
    }

    public JCheckBox getCheckAll() {
        return checkAll;
    }
}
