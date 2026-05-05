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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.dbconnector.DBConnector;
import org.apache.cayenne.modeler.toolkit.border.TopBorder;
import org.apache.cayenne.modeler.toolkit.ProjectDialog;
import org.apache.cayenne.modeler.ui.datasource.DataSourceDialog;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.validation.ValidationDialog;
import org.apache.cayenne.modeler.util.DbAdapterInfo;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Modal wizard for generating the database from a set of DataMaps. Two tabs: SQL options
 * (CRUD checkboxes + adapter + previewed SQL) and Tables (per-table include/exclude).
 */
public class DBGeneratorOptionsDialog extends ProjectDialog {

    private static final String JDBC_ADAPTER = "org.apache.cayenne.dba.JdbcAdapter";

    private final Collection<DataMap> dataMaps;
    private final TableSelectorPanel tables;
    private final DBGeneratorPrefs generatorDefaults;

    private final JTextArea sqlPreview;
    private final JCheckBox dropTables;
    private final JCheckBox createTables;
    private final JCheckBox createFK;
    private final JCheckBox createPK;
    private final JCheckBox dropPK;
    private final JComboBox<String> adapters;
    private final JTabbedPane tabs;
    private final JButton generateButton;
    private final JButton cancelButton;
    private final JButton saveSqlButton;

    private DBConnector connector;
    private Collection<DbGenerator> generators;
    private String textForSQL;

    // True while pushing a programmatic adapter selection — suppresses the action
    // listener so it doesn't recurse back into refreshSQLAction.
    private boolean updatingAdapterProgrammatically;

    public DBGeneratorOptionsDialog(ProjectSession session, Window owner,
                                    String title, Collection<DataMap> dataMaps) {
        super(session, owner, title, ModalityType.APPLICATION_MODAL);
        this.dataMaps = dataMaps;
        this.tables = new TableSelectorPanel(app);

        this.connector = new DBConnector();
        this.connector.setAllowDataSourceFailure(true);
        this.generatorDefaults = DBGeneratorPrefs.of(app.getPreferencesRepository(), session.project());

        // create widgets — set initial state before wiring listeners so we
        // don't fire spurious refresh events during construction.
        this.createFK = new JCheckBox("Create FK Support");
        this.createFK.setSelected(generatorDefaults.getCreateFK());
        this.createPK = new JCheckBox("Create Primary Key Support");
        this.createPK.setSelected(generatorDefaults.getCreatePK());
        this.createTables = new JCheckBox("Create Tables");
        this.createTables.setSelected(generatorDefaults.getCreateTables());
        this.dropPK = new JCheckBox("Drop Primary Key Support");
        this.dropPK.setSelected(generatorDefaults.getDropPK());
        this.dropTables = new JCheckBox("Drop Tables");
        this.dropTables.setSelected(generatorDefaults.getDropTables());

        this.generateButton = new JButton("Generate");
        this.cancelButton = new JButton("Cancel");
        this.saveSqlButton = new JButton("Save SQL");

        this.tabs = new JTabbedPane(SwingConstants.TOP);
        this.adapters = new JComboBox<>();
        this.adapters.setEditable(true);
        this.adapters.setModel(new DefaultComboBoxModel<>(DbAdapterInfo.getStandardAdapters()));
        this.adapters.setSelectedIndex(0);

        this.sqlPreview = new JTextArea();
        this.sqlPreview.setEditable(false);
        this.sqlPreview.setLineWrap(true);
        this.sqlPreview.setWrapStyleWord(true);

        connector.setDbAdapter(selectedAdapter());

        initLayout();
        initBindings();

        tables.updateTables(dataMaps);
        prepareGenerator();
        applyOptionsToGenerators();
        createSQL();
        setEnabled(connector != null);
        sqlPreview.setText(textForSQL);
    }

    private void initLayout() {
        getRootPane().setDefaultButton(generateButton);

        JPanel optionsPane = new JPanel(new GridLayout(3, 2));
        optionsPane.add(dropTables);
        optionsPane.add(createTables);
        optionsPane.add(new JLabel());
        optionsPane.add(createFK);
        optionsPane.add(dropPK);
        optionsPane.add(createPK);

        JPanel sqlTextPanel = new JPanel(new BorderLayout());
        sqlTextPanel.add(new JScrollPane(
                sqlPreview,
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
                tables,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        // pack() needs a sane preferred size for a decent default
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

    private void initBindings() {
        createFK.addActionListener(e -> refreshSQLAction());
        createPK.addActionListener(e -> refreshSQLAction());
        createTables.addActionListener(e -> refreshSQLAction());
        dropPK.addActionListener(e -> refreshSQLAction());
        dropTables.addActionListener(e -> refreshSQLAction());

        adapters.addActionListener(e -> {
            if (updatingAdapterProgrammatically) return;
            Object sel = adapters.getSelectedItem();
            connector.setDbAdapter(JDBC_ADAPTER.equals(sel) ? null : (String) sel);
            refreshSQLAction();
        });

        generateButton.addActionListener(e -> generateSchemaAction());
        saveSqlButton.addActionListener(e -> storeSQLAction());
        cancelButton.addActionListener(e -> dispose());

        // refresh SQL when the user switches back to the SQL Options tab
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 0) {
                refreshGeneratorAction();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                generatorDefaults.save(
                        createFK.isSelected(),
                        createPK.isSelected(),
                        createTables.isSelected(),
                        dropPK.isSelected(),
                        dropTables.isSelected());
            }
        });
    }

    private void applyOptionsToGenerators() {
        boolean cFK = createFK.isSelected();
        boolean cPK = createPK.isSelected();
        boolean cT = createTables.isSelected();
        boolean dPK = dropPK.isSelected();
        boolean dT = dropTables.isSelected();
        for (DbGenerator generator : generators) {
            generator.setShouldCreateFKConstraints(cFK);
            generator.setShouldCreatePKSupport(cPK);
            generator.setShouldCreateTables(cT);
            generator.setShouldDropPKSupport(dPK);
            generator.setShouldDropTables(dT);
        }
    }

    private void prepareGenerator() {
        try {
            DbAdapter adapter = connector.makeAdapter(app.getClassLoader(), app.getDbAdapterFactory());
            generators = new ArrayList<>();
            for (DataMap dataMap : dataMaps) {
                generators.add(new DbGenerator(
                        adapter,
                        dataMap,
                        tables.getExcludedTables(),
                        null,
                        NoopJdbcEventLogger.getInstance()));
            }
        } catch (Exception ex) {
            reportError("Error loading adapter", ex);
        }
    }

    private void createSQL() {
        StringBuilder buf = new StringBuilder();
        for (DbGenerator generator : generators) {
            Iterator<String> it = generator.configuredStatements().iterator();
            String batchTerminator = generator.getAdapter().getBatchTerminator();
            String lineEnd = (batchTerminator != null)
                    ? "\n" + batchTerminator + "\n\n"
                    : "\n\n";

            while (it.hasNext()) {
                buf.append(it.next()).append(lineEnd);
            }
        }
        textForSQL = buf.toString();
    }

    private void refreshGeneratorAction() {
        prepareGenerator();
        refreshSQLAction();
    }

    private void refreshSQLAction() {
        // sync combo to reflect current connector (e.g. after generateSchemaAction replaces it)
        String adapter = connector.getDbAdapter();
        selectAdapter(adapter);
        // mirror the displayed value back to the model (was: setSelectedItem returns "JdbcAdapter" placeholder when null)
        connector.setDbAdapter(adapter != null ? adapter : JDBC_ADAPTER);
        prepareGenerator();
        applyOptionsToGenerators();
        createSQL();
        sqlPreview.setText(textForSQL);
    }

    private void generateSchemaAction() {
        DataSourceDialog connectWizard = new DataSourceDialog(
                session,
                app.getFrame(),
                "Generate DB Schema: Connect to Database");
        connectWizard.open();
        if (connectWizard.isCanceled()) {
            return;
        }

        this.connector = connectWizard.getConnector();
        refreshGeneratorAction();

        Collection<ValidationResult> failures = new ArrayList<>();

        for (DbGenerator generator : generators) {
            if (generator.isEmpty(true)) {
                JOptionPane.showMessageDialog(this, "Nothing to generate.");
                return;
            }

            try {
                generator.runGenerator(connectWizard.getDataSource());
                failures.add(generator.getFailures());
            } catch (Throwable th) {
                reportError("Schema Generation Error", th);
            }
        }

        if (failures.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Schema Generation Complete.");
        } else {
            new ValidationDialog(
                    app,
                    app.getFrame(),
                    "Schema Generation Complete",
                    "Schema generation finished. The following problem(s) were ignored.",
                    failures).open();
        }
    }

    private void storeSQLAction() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setDialogTitle("Save SQL Script");

        File projectDir = new File(app
                .getFrame().getProjectSession().project()
                .getConfigurationResource()
                .getURL()
                .getPath());
        fc.setCurrentDirectory(projectDir);
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            refreshGeneratorAction();
            try {
                File file = fc.getSelectedFile();
                FileWriter fw = new FileWriter(file);
                PrintWriter pw = new PrintWriter(fw);
                pw.print(textForSQL);
                pw.flush();
                pw.close();
            } catch (IOException ex) {
                reportError("Error Saving SQL", ex);
            }
        }
    }

    private void selectAdapter(String adapter) {
        String display = adapter != null ? adapter : JDBC_ADAPTER;
        updatingAdapterProgrammatically = true;
        try {
            adapters.setSelectedItem(display);
        } finally {
            updatingAdapterProgrammatically = false;
        }
    }

    private String selectedAdapter() {
        Object sel = adapters.getSelectedItem();
        return sel != null ? sel.toString() : null;
    }
}
