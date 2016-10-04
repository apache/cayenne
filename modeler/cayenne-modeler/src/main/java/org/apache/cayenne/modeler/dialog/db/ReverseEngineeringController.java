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

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.db.DbLoader;
import org.apache.cayenne.dbsync.reverse.db.DbLoaderConfiguration;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.db.model.DbModel;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.modeler.dialog.pref.TreeEditor;
import org.apache.cayenne.modeler.dialog.pref.XMLFileEditor;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * A component for performing reverse engineering. Users can choose required dataMap and execute
 * reverse engineering. Also they can see tree view of db objects clicking on sync button.
 */
public class ReverseEngineeringController extends CayenneController {

    private static Log LOGGER = LogFactory.getLog(ReverseEngineeringController.class);

    protected ProjectController projectController;
    protected ReverseEngineeringView view;
    protected Map<String, DataMapViewModel> reverseEngineeringMap;
    protected DbModel dbModel;

    protected DataSource dataSource;
    protected DbAdapter adapter;

    protected DBConnectionInfo connectionInfo;
    protected ObjectBinding dataSourceBinding;
    protected Map dataSources;
    protected String dataSourceKey;
    protected boolean canceled;

    public ReverseEngineeringController(ProjectController controller,
                                        ReverseEngineeringView source) {
        super(controller);
        this.projectController = controller;
        this.view = source;

        this.connectionInfo = new DBConnectionInfo();
        this.reverseEngineeringMap = view.getReverseEngineeringViewMap();
        initBindings();
        refreshDataSources();
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);

        dataSourceBinding = builder.bindToComboSelection(view.getDataSources(), "dataSourceKey");

        builder.bindToAction(view.getSyncButton(), "syncAction()");
        builder.bindToAction(view.getExecuteButton(), "executeAction()");
        builder.bindToAction(view.getConfigButton(), "dataSourceConfigAction()");
    }

    private void buildDBProperties() throws Exception {
        ClassLoadingService classLoader = getApplication().getClassLoadingService();

        this.dataSource = connectionInfo.makeDataSource(classLoader);
        this.adapter = connectionInfo.makeAdapter(classLoader);
    }

    public void syncAction() throws SQLException {
        final TreeEditor treeEditor = view.getTreeEditor();
        XMLFileEditor xmlFileEditor = view.getXmlFileEditor();
        xmlFileEditor.removeAlertMessage();
        try {
            buildDBProperties();

            ReverseEngineering reverseEngineering = xmlFileEditor.convertTextIntoReverseEngineering();

            FiltersConfigBuilder filtersConfigBuilder = new FiltersConfigBuilder(reverseEngineering);
            DbLoaderConfiguration dbLoaderConfiguration = new DbLoaderConfiguration();
            dbLoaderConfiguration.setFiltersConfig(filtersConfigBuilder.build());

            try(Connection connection = dataSource.getConnection()) {
                DbLoader dbLoader = new ModelerDbLoader(this, treeEditor, connection);

                // TODO: counterintuitive... we never use the DataMap that we loaded...
                dbLoader.load(new DataMap(), dbLoaderConfiguration);
            }

            String mapName = projectController.getCurrentDataMap().getName();

            DataMapViewModel dataMapViewModel = new DataMapViewModel();
            dataMapViewModel.setReverseEngineeringTree(dbModel);
            dataMapViewModel.setReverseEngineeringText(xmlFileEditor.getView().getEditorPane().getText());
            reverseEngineeringMap.put(mapName, dataMapViewModel);
            treeEditor.convertTreeViewIntoTreeNode(dbModel);
        } catch (Exception e) {
            xmlFileEditor.addAlertMessage(e.getMessage());
        }
    }

    public void executeAction() {
        XMLFileEditor xmlFileEditor = view.getXmlFileEditor();
        xmlFileEditor.removeAlertMessage();
        try {
            buildDBProperties();

            final ReverseEngineering reverseEngineering = xmlFileEditor.convertTextIntoReverseEngineering();

            Thread th = new Thread(new Runnable() {

                public void run() {

                    try(Connection connection = dataSource.getConnection()) {
                        new DbLoaderHelper(
                                projectController,
                                connection,
                                adapter,
                                connectionInfo, reverseEngineering).execute();
                    }
                    catch (SQLException e) {
                        LOGGER.warn("Error on execute", e);
                    }

                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            application.getUndoManager().discardAllEdits();
                        }
                    });
                }
            });

            th.start();
            view.setTempDataMap(projectController.getCurrentDataMap());
        } catch (Exception e) {
            xmlFileEditor.addAlertMessage(e.getMessage());
        }
    }

    /**
     * Returns configured DbAdapter.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }


    public String getDataSourceKey() {
        return dataSourceKey;
    }

    public void setDataSourceKey(String dataSourceKey) {
        XMLFileEditor xmlFileEditor = view.getXmlFileEditor();
        xmlFileEditor.removeAlertMessage();

        this.dataSourceKey = dataSourceKey;

        DBConnectionInfo currentInfo = (DBConnectionInfo) dataSources.get(dataSourceKey);
        if (currentInfo != null) {
            currentInfo.copyTo(connectionInfo);
        } else {
            connectionInfo = new DBConnectionInfo();
        }
    }

    /**
     * Opens preferences panel to allow configuration of DataSource presets.
     */
    public void dataSourceConfigAction() {
        PreferenceDialog prefs = new PreferenceDialog(this);
        prefs.showDataSourceEditorAction(dataSourceKey);
        refreshDataSources();
    }

    public Component getView() {
        return view;
    }

    protected void refreshDataSources() {
        this.dataSources = getApplication().getCayenneProjectPreferences().getDetailObject(DBConnectionInfo.class)
                .getChildrenPreferences();

        // 1.2 migration fix - update data source adapter names
        Iterator it = dataSources.values().iterator();

        final String _12package = "org.objectstyle.cayenne.";
        while (it.hasNext()) {
            DBConnectionInfo info = (DBConnectionInfo) it.next();
            if (info.getDbAdapter() != null && info.getDbAdapter().startsWith(_12package)) {
                info.setDbAdapter("org.apache.cayenne." + info.getDbAdapter().substring(_12package.length()));
            }
        }

        Object[] keys = dataSources.keySet().toArray();
        Arrays.sort(keys);
        view.getDataSources().setModel(new DefaultComboBoxModel(keys));

        if (dataSources.isEmpty()) {
            dataSourceKey = null;
        }
        String key = null;
        if (keys.length > 0) {
            key = keys[0].toString();
        }
        setDataSourceKey(key);

        if (getDataSourceKey() == null) {
            dataSourceBinding.updateView();
        }
    }

}
