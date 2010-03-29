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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import org.apache.cayenne.modeler.Application;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Wizard for altering the database to match the data map.
 */
public class MergerOptionsView extends JDialog {

    protected JTextArea sql;
    protected JButton generateButton;
    protected JButton cancelButton;
    protected JButton saveSqlButton;

    protected Component tables;
    protected JTabbedPane tabs;

    public MergerOptionsView(Component tables) {
        super(Application.getFrame());
        
        // create widgets
        this.generateButton = new JButton("Migrate");
        this.cancelButton = new JButton("Close");
        this.saveSqlButton = new JButton("Save SQL");

        this.tables = tables;
        this.tabs = new JTabbedPane(SwingConstants.TOP);
        this.sql = new JTextArea();
        sql.setEditable(false);
        sql.setLineWrap(true);
        sql.setWrapStyleWord(true);

        JPanel sqlTextPanel = new JPanel(new BorderLayout());
        sqlTextPanel.add(new JScrollPane(
                sql,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(50dlu;pref):grow",
                "p, 9dlu, p, 3dlu, fill:40dlu:grow"));
        builder.setDefaultDialogBorder();
        builder.addSeparator("Generated SQL", cc.xywh(1, 3, 1, 1));
        builder.add(sqlTextPanel, cc.xy(1, 5));

        tabs.addTab("Operations", new JScrollPane(
                tables,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        tabs.addTab("Generated SQL", builder.getPanel());

        // we need the right preferred size so that dialog "pack()" produces decent
        // default size...
        tabs.setPreferredSize(new Dimension(600, 350));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveSqlButton);
        buttons.add(Box.createHorizontalStrut(20));
        buttons.add(cancelButton);
        buttons.add(generateButton);

        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(tabs, BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JTabbedPane getTabs() {
        return tabs;
    }

    /*
     * public JCheckBox getCreateFK() { return createFK; } public JCheckBox getCreatePK() {
     * return createPK; } public JCheckBox getCreateTables() { return createTables; }
     * public JCheckBox getDropPK() { return dropPK; } public JCheckBox getDropTables() {
     * return dropTables; }
     */

    public JButton getGenerateButton() {
        return generateButton;
    }

    public JButton getSaveSqlButton() {
        return saveSqlButton;
    }

    public JTextArea getSql() {
        return sql;
    }
}
