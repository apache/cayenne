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

package org.apache.cayenne.modeler.ui.dbgen;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.toolkit.border.TopBorder;
import org.apache.cayenne.modeler.util.DbAdapterInfo;

/**
 * Wizard for generating the database from the data map.
 */
public class DBGeneratorOptionsView extends JDialog {

    private static final String JDBC_ADAPTER = "org.apache.cayenne.dba.JdbcAdapter";

    private final JTextArea sql;
    private final JCheckBox dropTables;
    private final JCheckBox createTables;
    private final JCheckBox createFK;
    private final JCheckBox createPK;
    private final JCheckBox dropPK;
    private final JComboBox<String> adapters;

    // True while the controller is pushing a programmatic adapter selection;
    // suppresses the action listener so it doesn't recurse back into the controller.
    private boolean updatingAdapterProgrammatically;

    public DBGeneratorOptionsView(DBGeneratorOptionsController controller,
                                  Component tablesView,
                                  boolean createFK, boolean createPK, boolean createTables,
                                  boolean dropPK, boolean dropTables) {

        // create widgets — set initial state before wiring listeners so we
        // don't fire spurious refresh events during construction.
        this.createFK = new JCheckBox("Create FK Support");
        this.createFK.setSelected(createFK);
        this.createPK = new JCheckBox("Create Primary Key Support");
        this.createPK.setSelected(createPK);
        this.createTables = new JCheckBox("Create Tables");
        this.createTables.setSelected(createTables);
        this.dropPK = new JCheckBox("Drop Primary Key Support");
        this.dropPK.setSelected(dropPK);
        this.dropTables = new JCheckBox("Drop Tables");
        this.dropTables.setSelected(dropTables);

        JButton generateButton = new JButton("Generate");
        getRootPane().setDefaultButton(generateButton);
        JButton cancelButton = new JButton("Cancel");
        JButton saveSqlButton = new JButton("Save SQL");

        JTabbedPane tabs = new JTabbedPane(SwingConstants.TOP);
        this.adapters = new JComboBox<>();
        adapters.setEditable(true);
        adapters.setModel(new DefaultComboBoxModel<>(DbAdapterInfo.getStandardAdapters()));
        adapters.setSelectedIndex(0);

        this.sql = new JTextArea();
        sql.setEditable(false);
        sql.setLineWrap(true);
        sql.setWrapStyleWord(true);

        // wire listeners
        this.createFK.addActionListener(e -> controller.refreshSqlClicked());
        this.createPK.addActionListener(e -> controller.refreshSqlClicked());
        this.createTables.addActionListener(e -> controller.refreshSqlClicked());
        this.dropPK.addActionListener(e -> controller.refreshSqlClicked());
        this.dropTables.addActionListener(e -> controller.refreshSqlClicked());

        adapters.addActionListener(e -> {
            if (updatingAdapterProgrammatically) return;
            Object sel = adapters.getSelectedItem();
            controller.adapterChanged(JDBC_ADAPTER.equals(sel) ? null : (String) sel);
        });

        generateButton.addActionListener(e -> controller.generateClicked());
        saveSqlButton.addActionListener(e -> controller.saveSqlClicked());
        cancelButton.addActionListener(e -> controller.cancelClicked());

        // refresh SQL if different tables were selected
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 0) {
                controller.sqlTabSelected();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                controller.windowClosed();
            }
        });

        // assemble
        JPanel optionsPane = new JPanel(new GridLayout(3, 2));
        optionsPane.add(this.dropTables);
        optionsPane.add(this.createTables);
        optionsPane.add(new JLabel());
        optionsPane.add(this.createFK);
        optionsPane.add(this.dropPK);
        optionsPane.add(this.createPK);

        JPanel sqlTextPanel = new JPanel(new BorderLayout());
        sqlTextPanel.add(new JScrollPane(
                sql,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        JPanel adapterPanel = new JPanel(new BorderLayout());
        adapterPanel.add(adapters);

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(50dlu;pref):grow",
                "p, 3dlu, p, 9dlu, p, 3dlu, p, 3dlu, p, 3dlu, fill:40dlu:grow"));
        builder.addSeparator("Options", cc.xywh(1, 1, 1, 1));
        builder.add(optionsPane, cc.xy(1, 3, "left,fill"));
        builder.addSeparator("Adapter", cc.xywh(1, 5, 1, 1));
        builder.add(adapterPanel, cc.xy(1, 7));
        builder.addSeparator("Generated SQL", cc.xywh(1, 9, 1, 1));
        builder.add(sqlTextPanel, cc.xy(1, 11));
        builder.setBorder(Borders.DIALOG_BORDER);

        tabs.addTab("SQL Options", builder.getPanel());
        tabs.addTab("Tables", new JScrollPane(
                tablesView,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        // we need the right preferred size so that dialog "pack()" produces decent
        // default size...
        tabs.setPreferredSize(new Dimension(450, 500));
        tabs.setFocusable(false);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.add(saveSqlButton);
        buttonsPanel.add(Box.createHorizontalStrut(20));
        buttonsPanel.add(cancelButton);
        buttonsPanel.add(generateButton);
        buttonsPanel.setBorder(TopBorder.create());

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(tabs, BorderLayout.CENTER);
        contentPane.add(buttonsPanel, BorderLayout.SOUTH);
    }

    public void setSqlPreview(String sql) {
        this.sql.setText(sql);
    }

    public void selectAdapter(String adapter) {
        String display = adapter != null ? adapter : JDBC_ADAPTER;
        updatingAdapterProgrammatically = true;
        try {
            adapters.setSelectedItem(display);
        } finally {
            updatingAdapterProgrammatically = false;
        }
    }

    public String getSelectedAdapter() {
        Object sel = adapters.getSelectedItem();
        return sel != null ? sel.toString() : null;
    }

    public boolean isCreateFkSelected() {
        return createFK.isSelected();
    }

    public boolean isCreatePkSelected() {
        return createPK.isSelected();
    }

    public boolean isCreateTablesSelected() {
        return createTables.isSelected();
    }

    public boolean isDropPkSelected() {
        return dropPK.isSelected();
    }

    public boolean isDropTablesSelected() {
        return dropTables.isSelected();
    }
}
