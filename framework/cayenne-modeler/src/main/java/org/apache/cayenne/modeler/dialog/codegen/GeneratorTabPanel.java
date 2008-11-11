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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 */
public class GeneratorTabPanel extends JPanel {

    protected JComboBox generationMode;
    protected CardLayout modeLayout;
    protected JPanel modesPanel;

    public GeneratorTabPanel(String[] modeNames, Component[] modePanels) {
        this.generationMode = new JComboBox(modeNames);
        this.modeLayout = new CardLayout();
        this.modesPanel = new JPanel(modeLayout);

        generationMode.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                modeLayout.show(modesPanel, generationMode.getSelectedItem().toString());
            }
        });

        // assemble
        FormLayout layout = new FormLayout("right:70dlu, 3dlu, fill:150dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.append("Type:", generationMode);
        builder.appendSeparator();

        for (int i = 0; i < modeNames.length; i++) {
            modesPanel.add(modePanels[i], modeNames[i]);
        }

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.NORTH);
        add(modesPanel, BorderLayout.CENTER);
    }

    public JComboBox getGenerationMode() {
        return generationMode;
    }
}
