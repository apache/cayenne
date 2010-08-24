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

package org.apache.cayenne.modeler.dialog.db;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.util.CayenneDialog;
import org.apache.cayenne.modeler.util.PanelFactory;

/**
 */
public class DbLoaderMergeDialog extends CayenneDialog {

    protected DbLoaderHelper helper;
    protected JCheckBox rememberSelection;
    protected JLabel message;
    protected JButton overwriteButton;
    protected JButton skipButton;
    protected JButton stopButton;

    public DbLoaderMergeDialog(CayenneModelerFrame owner) {
        super(owner);
        init();
        initController();
    }

    private void init() {
        // create widgets
        this.rememberSelection = new JCheckBox("Remember my decision for other entities.");
        this.rememberSelection.setSelected(true);

        this.overwriteButton = new JButton("Overwrite");
        this.skipButton = new JButton("Skip");
        this.stopButton = new JButton("Stop");
        this.message = new JLabel("DataMap already contains this table. Overwrite?");

        // assemble
        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        messagePanel.add(message);

        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        checkPanel.add(rememberSelection);

        JPanel buttons = PanelFactory.createButtonPanel(new JButton[] {
                skipButton, overwriteButton, stopButton
        });

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(messagePanel, BorderLayout.NORTH);
        contentPane.add(checkPanel, BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);

        setModal(true);
        setResizable(false);
        setSize(250, 150);
        setTitle("DbEntity Already Exists");
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }

    private void initController() {
        overwriteButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateModel(true, false);
            }
        });

        skipButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateModel(false, false);
            }
        });

        stopButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateModel(false, true);
            }
        });
    }

    private void updateModel(boolean overwrite, boolean stop) {
        if (helper != null) {
            helper.setOverwritePreferenceSet(rememberSelection.isSelected());
            helper.setOverwritingEntities(overwrite);
            helper.setStoppingReverseEngineering(stop);
        }

        this.setVisible(false);
    }

    public void initFromModel(DbLoaderHelper helper, String tableName) {
        this.helper = helper;
        this.message.setText("DataMap already contains table '"
                + tableName
                + "'. Overwrite?");

        validate();
        pack();
    }
}
