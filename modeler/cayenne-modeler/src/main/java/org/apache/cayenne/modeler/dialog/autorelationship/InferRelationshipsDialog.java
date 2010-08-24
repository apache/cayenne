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
package org.apache.cayenne.modeler.dialog.autorelationship;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.NamingStrategyPreferences;

public class InferRelationshipsDialog extends JDialog {
    public static final int SELECT = 1;
    public static final int CANCEL = 0;
    protected int choice;

    protected JButton generateButton;
    protected JButton cancelButton;
    protected JLabel entityCount;
    protected JLabel strategyLabel;

    protected JComboBox strategyCombo;

    public InferRelationshipsDialog(Component entitySelectorPanel) {
        super(Application.getFrame());
        this.generateButton = new JButton("Create DbRelationships");
        this.cancelButton = new JButton("Cancel");
        this.entityCount = new JLabel("No DbRelationships selected");
        entityCount.setFont(entityCount.getFont().deriveFont(10f));

        this.strategyCombo = new JComboBox();
        strategyCombo.setEditable(true);
        this.strategyLabel = new JLabel("Naming Strategy:  ");

        // assemble
        JPanel strategyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        strategyPanel.add(strategyLabel);
        strategyPanel.add(strategyCombo);

        JPanel messages = new JPanel(new BorderLayout());
        messages.add(entityCount, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(entityCount);
        buttons.add(Box.createHorizontalStrut(50));
        buttons.add(cancelButton);
        buttons.add(generateButton);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(strategyPanel, BorderLayout.NORTH);
        contentPane.add(entitySelectorPanel, BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);

        this.choice = CANCEL;

        strategyCombo.setModel(new DefaultComboBoxModel(
                NamingStrategyPreferences.getInstance().getLastUsedStrategies()));

        setTitle("Infer Relationships");
    }

    public int getChoice() {
        return choice;
    }

    public void setChoice(int choice) {
        this.choice = choice;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getGenerateButton() {
        return generateButton;
    }

    public JLabel getEntityCount() {
        return entityCount;
    }

    public JComboBox getStrategyCombo() {
        return strategyCombo;
    }
}