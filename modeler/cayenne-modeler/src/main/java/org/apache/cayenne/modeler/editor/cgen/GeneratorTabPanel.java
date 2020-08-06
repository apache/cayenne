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

import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.util.Objects;

/**
 * @since 4.1
 */
public class GeneratorTabPanel extends JPanel {

    private JComboBox<String> generationMode;
    private CardLayout modeLayout;
    private JPanel modesPanel;

    public GeneratorTabPanel(String[] modeNames, Component[] modePanels) {
        setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        this.generationMode = new JComboBox<>(modeNames);
        this.modeLayout = new CardLayout();
        this.modesPanel = new JPanel(modeLayout);

        generationMode.addItemListener(e -> modeLayout.show(modesPanel, Objects.requireNonNull(generationMode.getSelectedItem()).toString()));

        // assemble
        FormLayout layout = new FormLayout("right:77dlu, 3dlu, fill:240, fill:300dlu:grow", "");
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
}
