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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.swing.components.TopBorder;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * @since 4.1
 */
public class GeneratorTabPanel extends JPanel {

    private JComboBox<String> generationMode;
    private CardLayout modeLayout;
    private JPanel modesPanel;

    private JButton generateButton;

    public GeneratorTabPanel(String[] modeNames, Component[] modePanels) {
        setLayout(new BorderLayout());
        this.generateButton = new JButton("Generate");
        generateButton.setIcon(ModelerUtil.buildIcon("icon-gen_java.png"));
        generateButton.setPreferredSize(new Dimension(180, 30));
        generateButton.setEnabled(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(TopBorder.create());
        buttons.add(Box.createHorizontalStrut(50));
        buttons.add(generateButton);
        add(buttons, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        this.generationMode = new JComboBox<>(modeNames);
        this.modeLayout = new CardLayout();
        this.modesPanel = new JPanel(modeLayout);

        generationMode.addItemListener(e -> modeLayout.show(modesPanel, Objects.requireNonNull(generationMode.getSelectedItem()).toString()));

        // assemble
        FormLayout layout = new FormLayout("right:77dlu, 3dlu, fill:300, fill:300dlu:grow", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.append("Type:", generationMode, 1);
        builder.appendSeparator();

        for (int i = 0; i < modeNames.length; i++) {
            modesPanel.add(modePanels[i], modeNames[i]);
        }

        panel.setLayout(new BorderLayout());
        panel.add(builder.getPanel(), BorderLayout.NORTH);
        panel.add(modesPanel, BorderLayout.CENTER);

        add(panel, BorderLayout.CENTER);
    }

    public JComboBox getGenerationMode() {
        return generationMode;
    }

    public JButton getGenerateButton() {
        return generateButton;
    }
}
