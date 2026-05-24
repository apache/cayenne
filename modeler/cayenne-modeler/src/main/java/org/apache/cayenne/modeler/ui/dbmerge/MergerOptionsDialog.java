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

package org.apache.cayenne.modeler.ui.dbmerge;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.merge.DataMapMerger;
import org.apache.cayenne.dbsync.merge.context.MergeDirection;
import org.apache.cayenne.dbsync.merge.context.MergerContext;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.db.AbstractToDbToken;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.NoStemStemmer;
import org.apache.cayenne.dbsync.reverse.dbimport.DefaultDbImportAction;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoader;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoaderConfiguration;
import org.apache.cayenne.dbsync.reverse.dbload.DefaultModelMergeDelegate;
import org.apache.cayenne.dbsync.reverse.dbload.LoggingDbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.dbload.ModelMergeDelegate;
import org.apache.cayenne.dbsync.reverse.dbload.ProxyModelMergeDelegate;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.pref.adapters.FileChooserPrefs;
import org.apache.cayenne.modeler.pref.dbconnector.DBConnector;
import org.apache.cayenne.modeler.dbconnector.DBConnectorFactory;
import org.apache.cayenne.modeler.event.model.DataMapEvent;
import org.apache.cayenne.modeler.toolkit.border.TopBorder;
import org.apache.cayenne.modeler.toolkit.text.CMTextArea;
import org.apache.cayenne.modeler.toolkit.ProjectDialog;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.validation.ValidationDialog;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.validation.ValidationResult;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Modal wizard for altering the database schema to match a DataMap. Two tabs:
 * Operations (per-token include/exclude via {@link MergerTokenSelector}) and Generated SQL.
 */
public class MergerOptionsDialog extends ProjectDialog {

    private final DataMap dataMap;
    private final DBConnector connectionInfo;
    private final String defaultCatalog;
    private final String defaultSchema;
    private final MergerTokenFactoryProvider mergerTokenFactoryProvider;

    private final MergerTokenSelector tokens;
    private final CMTextArea sqlPreview;
    private final JTabbedPane tabs;
    private final JButton generateButton;
    private final JButton cancelButton;
    private final JButton saveSqlButton;

    private DbAdapter adapter;
    private String textForSQL;

    public MergerOptionsDialog(ProjectSession session, Window owner,
                               String title, DBConnector connectionInfo, DataMap dataMap,
                               String defaultCatalog, String defaultSchema,
                               MergerTokenFactoryProvider mergerTokenFactoryProvider) {
        super(session, owner, title, ModalityType.APPLICATION_MODAL);
        this.dataMap = dataMap;
        this.connectionInfo = connectionInfo;
        this.defaultCatalog = defaultCatalog;
        this.defaultSchema = defaultSchema;
        this.mergerTokenFactoryProvider = mergerTokenFactoryProvider;

        this.tokens = new MergerTokenSelector();
        this.sqlPreview = new CMTextArea();
        this.sqlPreview.setEditable(false);
        this.sqlPreview.setLineWrap(true);
        this.sqlPreview.setWrapStyleWord(true);

        this.tabs = new JTabbedPane(SwingConstants.TOP);
        this.tabs.setFocusable(false);
        this.generateButton = new JButton("Migrate");
        this.cancelButton = new JButton("Cancel");
        this.saveSqlButton = new JButton("Save SQL");

        initLayout();
        initBindings();

        prepareMigrator();
        createSQL();
        sqlPreview.setText(textForSQL);
    }

    private void initLayout() {
        getRootPane().setDefaultButton(generateButton);

        JPanel sqlTextPanel = new JPanel(new BorderLayout());
        sqlTextPanel.add(new JScrollPane(
                sqlPreview,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(50dlu;pref):grow",
                "p, 9dlu, p, 3dlu, fill:40dlu:grow"));
        builder.addSeparator("Generated SQL", cc.xywh(1, 3, 1, 1));
        builder.add(sqlTextPanel, cc.xy(1, 5));
        builder.setBorder(Borders.DIALOG_BORDER);

        tabs.addTab("Operations", new JScrollPane(
                tokens,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        tabs.addTab("Generated SQL", builder.getPanel());

        // pack() needs a sane preferred size for a decent default
        tabs.setPreferredSize(new Dimension(600, 350));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveSqlButton);
        buttons.add(Box.createHorizontalStrut(20));
        buttons.add(cancelButton);
        buttons.add(generateButton);
        buttons.setBorder(TopBorder.create());

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(tabs, BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);
    }

    private void initBindings() {
        sqlPreview.addCommitListener(s -> textForSQL = s);

        generateButton.addActionListener(e -> generateSchemaAction());
        saveSqlButton.addActionListener(e -> storeSQLAction());
        cancelButton.addActionListener(e -> dispose());

        // refresh SQL when the user switches to the SQL tab
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) {
                refreshSQLAction();
            }
        });
    }

    private void prepareMigrator() {
        try {
            adapter = new DBConnectorFactory(app.getClassLoader()).makeAdapter(connectionInfo, app.getDbAdapterFactory());

            MergerTokenFactory mergerTokenFactory = mergerTokenFactoryProvider.get(adapter);
            tokens.setMergerTokenFactory(mergerTokenFactory);

            FiltersConfig filters = FiltersConfig.create(defaultCatalog, defaultSchema, TableFilter.everything(),
                    PatternFilter.INCLUDE_NOTHING);

            DataMapMerger merger = DataMapMerger.builder(mergerTokenFactory)
                    .filters(filters)
                    .build();

            DbLoaderConfiguration config = new DbLoaderConfiguration();
            config.setFiltersConfig(filters);

            DataSource dataSource = new DBConnectorFactory(app.getClassLoader()).makeDataSource(connectionInfo);

            DataMap dbImport;
            try (Connection conn = dataSource.getConnection()) {
                dbImport = new DbLoader(adapter, conn,
                        config,
                        new LoggingDbLoaderDelegate(LoggerFactory.getLogger(DbLoader.class)),
                        new DefaultObjectNameGenerator(NoStemStemmer.getInstance()))
                        .load();
            } catch (SQLException e) {
                throw new CayenneRuntimeException("Can't doLoad dataMap from db.", e);
            }

            tokens.setTokens(merger.createMergeTokens(dataMap, dbImport));
        } catch (Exception ex) {
            reportError("Error loading adapter", ex);
        }
    }

    private void createSQL() {
        StringBuilder buf = new StringBuilder();

        Iterator<MergerToken> it = tokens.getSelectedTokens().iterator();
        String batchTerminator = adapter.getBatchTerminator();
        String lineEnd = batchTerminator != null ? "\n" + batchTerminator + "\n\n" : "\n\n";

        while (it.hasNext()) {
            MergerToken token = it.next();
            if (token instanceof AbstractToDbToken tdb) {
                for (String sql : tdb.createSql(adapter)) {
                    buf.append(sql);
                    buf.append(lineEnd);
                }
            }
        }

        textForSQL = buf.toString();
    }

    private void refreshSQLAction() {
        createSQL();
        sqlPreview.setText(textForSQL);
    }

    private void generateSchemaAction() {
        refreshSQLAction();

        List<MergerToken> tokensToMigrate = tokens.getSelectedTokens();
        if (tokensToMigrate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nothing to migrate.");
            return;
        }

        DataSource dataSource;
        try {
            dataSource = new DBConnectorFactory(app.getClassLoader()).makeDataSource(connectionInfo);
        } catch (SQLException ex) {
            reportError("Migration Error", ex);
            return;
        }

        Collection<ObjEntity> loadedObjEntities = new LinkedList<>();

        MergerContext mergerContext = MergerContext.builder(dataMap)
                .syntheticDataNode(dataSource, adapter)
                .delegate(createDelegate(loadedObjEntities))
                .build();

        boolean modelChanged = applyTokens(tokensToMigrate, mergerContext);

        DefaultDbImportAction.flattenManyToManyRelationships(
                dataMap,
                loadedObjEntities,
                mergerContext.getNameGenerator());

        notifyProjectModified(modelChanged);

        reportFailures(mergerContext);

        if (tokens.isReverse()) {
            app.getUndoManager().discardAllEdits();
        }
    }

    private ModelMergeDelegate createDelegate(Collection<ObjEntity> loadedObjEntities) {
        return new ProxyModelMergeDelegate(new DefaultModelMergeDelegate()) {
            @Override
            public void objEntityAdded(ObjEntity ent) {
                loadedObjEntities.add(ent);
                super.objEntityAdded(ent);
            }
        };
    }

    private boolean applyTokens(List<MergerToken> tokensToMigrate, MergerContext mergerContext) {
        boolean modelChanged = false;

        try {
            for (MergerToken tok : tokensToMigrate) {
                int numOfFailuresBefore = getFailuresCount(mergerContext);

                tok.execute(mergerContext);

                if (!modelChanged && tok.getDirection().equals(MergeDirection.TO_MODEL)) {
                    modelChanged = true;
                }
                if (numOfFailuresBefore == getFailuresCount(mergerContext)) {
                    // looks like the token executed without failures
                    tokens.removeToken(tok);
                }
            }
        } catch (Throwable th) {
            reportError("Migration Error", th);
        }

        return modelChanged;
    }

    private int getFailuresCount(MergerContext mergerContext) {
        return mergerContext.getValidationResult().getFailures().size();
    }

    private void reportFailures(MergerContext mergerContext) {
        ValidationResult failures = mergerContext.getValidationResult();
        if (failures == null || !failures.hasFailures()) {
            JOptionPane.showMessageDialog(this, "Migration Complete.");
        } else {
            new ValidationDialog(
                    app,
                    app.getFrame(),
                    "Migration Complete",
                    "Migration finished. The following problem(s) were ignored.",
                    failures).open();
        }
    }

    private void notifyProjectModified(boolean modelChanged) {
        if (!modelChanged) {
            return;
        }

        // mark the model as unsaved
        Project project = app.getFrame().getProjectSession().project();
        project.setModified(true);
        session.setDirty(true);
        session.fireDataMapEvent(DataMapEvent.ofRemove(app.getFrame(), dataMap));
        session.fireDataMapEvent(DataMapEvent.ofAdd(app.getFrame(), dataMap));
    }

    private void storeSQLAction() {
        FileChooserPrefs prefs = new FileChooserPrefs(app.getPrefsManager().uiNode("merger/lastSqlDir"));
        File file = app.getFileChooser(this, "Save SQL Script").saveFile(prefs, null);
        if (file != null) {
            refreshSQLAction();
            try (FileWriter fw = new FileWriter(file); PrintWriter pw = new PrintWriter(fw)) {
                pw.print(textForSQL);
            } catch (IOException ex) {
                reportError("Error Saving SQL", ex);
            }
        }
    }
}
