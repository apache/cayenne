/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.dialog.db;

import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.loader.DbLoaderConfiguration;
import org.apache.cayenne.access.loader.DefaultDbLoaderDelegate;
import org.apache.cayenne.access.loader.filters.CatalogFilter;
import org.apache.cayenne.access.loader.filters.SchemaFilter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbimport.FiltersConfigBuilder;
import org.apache.cayenne.dbimport.ReverseEngineering;
import org.apache.cayenne.dbimport.ReverseEngineeringLoaderException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.db.model.DBCatalog;
import org.apache.cayenne.modeler.dialog.db.model.DBColumn;
import org.apache.cayenne.modeler.dialog.db.model.DBElement;
import org.apache.cayenne.modeler.dialog.db.model.DBEntity;
import org.apache.cayenne.modeler.dialog.db.model.DBModel;
import org.apache.cayenne.modeler.dialog.db.model.DBProcedure;
import org.apache.cayenne.modeler.dialog.db.model.DBSchema;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.modeler.dialog.pref.TreeEditor;
import org.apache.cayenne.modeler.dialog.pref.XMLFileEditor;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A component for performing reverse engineering. Users can choose required dataMap and execute
 * reverse engineering. Also they can see tree view of db objects clicking on sync button.
 */
public class ReverseEngineeringController extends CayenneController {
    private static final Log LOGGER = LogFactory.getLog(ReverseEngineeringController.class);

    protected ProjectController projectController;
    protected ReverseEngineeringView view;
    protected Map<String, DataMapViewModel> reverseEngineeringMap;
    protected DBModel dbModel;

    protected Connection connection;
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
        this.connection = connectionInfo.makeDataSource(classLoader).getConnection();
        this.adapter = connectionInfo.makeAdapter(classLoader);
    }

    public void syncAction() throws SQLException {
        final TreeEditor treeEditor = view.getTreeEditor();
        XMLFileEditor xmlFileEditor = view.getXmlFileEditor();
        xmlFileEditor.removeAlertMessage();
        try {
            buildDBProperties();

            DbLoader dbLoader = new DbLoader(connection, adapter, new DefaultDbLoaderDelegate()) {
                @Override
                public DataMap load(DbLoaderConfiguration config) throws SQLException {
                    DataMap dataMap = new DataMap();
                    Map<String, Procedure> procedureMap = loadProcedures(dataMap, config);
                    load(dataMap, config, procedureMap);
                    return dataMap;
                }

                public void load(DataMap dataMap, DbLoaderConfiguration config, Map<String, Procedure> procedureMap
                )
                        throws SQLException {
                    LOGGER.info("Schema loading...");

                    String[] types = config.getTableTypes();
                    if (types == null || types.length == 0) {
                        types = getDefaultTableTypes();
                    }

                    treeEditor.setRoot(dataSourceKey);
                    dbModel = new DBModel(dataSourceKey);
                    boolean catalogSetted = false;
                    DBElement currentDBCatalog = null;
                    DBElement currentDBSchema = null;

                    for (CatalogFilter catalog : config.getFiltersConfig().catalogs) {
                        for (SchemaFilter schema : catalog.schemas) {
                            boolean entityChecked = false;
                            List<DbEntity> entityList = createTableLoader(catalog.name, schema.name, schema.tables).loadDbEntities(
                                    dataMap, config, types);
                            DbEntity entityFromLoader = entityList.get(0);

                            if (entityFromLoader != null) {
                                if (catalogSetted == false && entityFromLoader.getCatalog() != null) {
                                    currentDBCatalog = new DBCatalog(entityFromLoader.getCatalog());
                                    dbModel.addElement(currentDBCatalog);
                                    catalogSetted = true;
                                }

                                if (entityFromLoader.getSchema() != null) {
                                    currentDBSchema = new DBSchema(entityFromLoader.getSchema());
                                    if(currentDBCatalog != null) {
                                        currentDBCatalog.addElement(currentDBSchema);
                                    } else {
                                        dbModel.addElement(currentDBSchema);
                                    }
                                }
                                entityChecked = true;
                            }

                            if (!entityChecked && !procedureMap.isEmpty()) {
                                Map.Entry<String, Procedure> entry = procedureMap.entrySet().iterator().next();
                                Procedure procedure = entry.getValue();

                                if (catalogSetted && procedure.getCatalog() != null) {
                                    currentDBCatalog = new DBCatalog(procedure.getCatalog());
                                    dbModel.addElement(currentDBCatalog);
                                    catalogSetted = true;
                                }

                                if (procedure.getSchema() != null) {
                                    currentDBSchema = new DBSchema(procedure.getSchema());
                                    if(currentDBCatalog != null) {
                                        currentDBCatalog.addElement(currentDBSchema);
                                    } else {
                                        dbModel.addElement(currentDBSchema);
                                    }
                                }
                            }

                            DBEntity currentDBEntity;
                            if (currentDBSchema != null) {
                                for (DbEntity dbEntity : entityList) {
                                    currentDBEntity = new DBEntity(dbEntity.getName());
                                    currentDBSchema.addElement(currentDBEntity);
                                    for (DbAttribute dbColumn : dbEntity.getAttributes()) {
                                        currentDBEntity.addElement(new DBColumn(dbColumn.getName()));
                                    }
                                }
                            } else {
                                for (DbEntity dbEntity : entityList) {
                                    currentDBEntity = new DBEntity(dbEntity.getName());
                                    for (DbAttribute dbColumn : dbEntity.getAttributes()) {
                                        currentDBEntity.addElement(new DBColumn(dbColumn.getName()));
                                    }
                                    currentDBCatalog.addElement(currentDBEntity);
                                }
                            }

                            if (currentDBSchema != null) {
                                for (Map.Entry<String, Procedure> entry : procedureMap.entrySet()) {
                                    currentDBSchema.addElement(new DBProcedure(entry.getValue().getName()));
                                }
                            } else {
                                for (Map.Entry<String, Procedure> entry : procedureMap.entrySet()) {
                                    currentDBCatalog.addElement(new DBProcedure(entry.getValue().getName()));
                                }
                            }
                            currentDBSchema = null;
                        }
                        catalogSetted = false;
                        currentDBCatalog = null;
                    }
                }
            };

            ReverseEngineering reverseEngineering = xmlFileEditor.convertTextIntoReverseEngineering();

            FiltersConfigBuilder filtersConfigBuilder = new FiltersConfigBuilder(reverseEngineering);
            DbLoaderConfiguration dbLoaderConfiguration = new DbLoaderConfiguration();
            dbLoaderConfiguration.setFiltersConfig(filtersConfigBuilder.filtersConfig());

            dbLoader.load(dbLoaderConfiguration);

            String mapName = projectController.getCurrentDataMap().getName();

            DataMapViewModel dataMapViewModel = new DataMapViewModel(mapName);
            dataMapViewModel.setReverseEngineeringTree(dbModel);
            dataMapViewModel.setReverseEngineeringText(xmlFileEditor.getView().getEditorPane().getText());
            reverseEngineeringMap.put(mapName, dataMapViewModel);
            treeEditor.convertTreeViewIntoTreeNode(dbModel);
        } catch (ReverseEngineeringLoaderException e) {
            xmlFileEditor.addAlertMessage(e.getMessage());
        } catch (Exception e) {
            xmlFileEditor.addAlertMessage(e.getMessage());
        }
    }

    public void executeAction() {
        XMLFileEditor xmlFileEditor = view.getXmlFileEditor();
        xmlFileEditor.removeAlertMessage();
        try {
            buildDBProperties();

            ReverseEngineering reverseEngineering = xmlFileEditor.convertTextIntoReverseEngineering();

            final DbLoaderHelper helper = new DbLoaderHelper(
                    projectController,
                    connection,
                    adapter,
                    connectionInfo, reverseEngineering);
            Thread th = new Thread(new Runnable() {

                public void run() {
                    helper.execute();

                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            application.getUndoManager().discardAllEdits();
                        }
                    });
                }
            });

            th.start();
            view.setTempDataMap(projectController.getCurrentDataMap());
        } catch (ReverseEngineeringLoaderException e) {
            xmlFileEditor.addAlertMessage(e.getMessage());
        } catch (Exception e) {
            xmlFileEditor.addAlertMessage(e.getMessage());
        }
    }

    /**
     * Returns configured DB connection.
     */
    public Connection getConnection() {
        return connection;
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
