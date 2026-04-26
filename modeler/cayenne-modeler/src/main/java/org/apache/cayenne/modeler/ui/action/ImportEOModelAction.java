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

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.configuration.runtime.JNDIDataSourceFactory;
import org.apache.cayenne.configuration.runtime.XMLPoolingDataSourceFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.db2.DB2Adapter;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.firebird.FirebirdAdapter;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.apache.cayenne.dba.ingres.IngresAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.apache.cayenne.dba.sqlite.SQLiteAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.dba.sybase.SybaseAdapter;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.event.model.DbEntityEvent;
import org.apache.cayenne.modeler.event.model.ModelEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.display.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.display.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.model.DataNodeEvent;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.ui.errors.ErrorsController;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.util.FileFilters;
import org.apache.cayenne.wocompat.EOModelProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Action handler for WebObjects EOModel import function.
 *
 */
public class ImportEOModelAction extends ModelerAbstractAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportEOModelAction.class);

    private final Map<String, String> adaptersByEofPlugin;
    private final Map<String, String> adaptersByDriver;

    private JFileChooser eoModelChooser;

    public ImportEOModelAction(Application application) {
        super("Import EOModel", application);

        this.adaptersByEofPlugin = new HashMap<>();
        adaptersByEofPlugin.put("com.webobjects.jdbcadaptor.SybasePlugIn", SybaseAdapter.class.getName());
        adaptersByEofPlugin.put("com.webobjects.jdbcadaptor.MerantPlugIn", SQLServerAdapter.class.getName());
        adaptersByEofPlugin.put("com.webobjects.jdbcadaptor.MicrosoftPlugIn", SQLServerAdapter.class.getName());
        adaptersByEofPlugin.put("com.webobjects.jdbcadaptor.MySQLPlugIn", MySQLAdapter.class.getName());
        adaptersByEofPlugin.put("com.webobjects.jdbcadaptor.OraclePlugIn", OracleAdapter.class.getName());
        adaptersByEofPlugin.put("com.webobjects.jdbcadaptor.FrontbasePlugIn", FrontBaseAdapter.class.getName());
        adaptersByEofPlugin.put("PostgresqlPlugIn", PostgresAdapter.class.getName());

        this.adaptersByDriver = new HashMap<>();
        adaptersByDriver.put("oracle.jdbc.driver.OracleDriver", OracleAdapter.class.getName());
        adaptersByDriver.put("com.sybase.jdbc2.jdbc.SybDriver", SybaseAdapter.class.getName());
        adaptersByDriver.put("com.mysql.jdbc.Driver", MySQLAdapter.class.getName());
        adaptersByDriver.put("com.mysql.cj.jdbc.Driver", MySQLAdapter.class.getName());
        adaptersByDriver.put("com.ibm.db2.jcc.DB2Driver", DB2Adapter.class.getName());
        adaptersByDriver.put("org.hsqldb.jdbcDriver", HSQLDBAdapter.class.getName());
        adaptersByDriver.put("org.h2.Driver", H2Adapter.class.getName());
        adaptersByDriver.put("org.postgresql.Driver", PostgresAdapter.class.getName());
        adaptersByDriver.put("com.microsoft.sqlserver.jdbc.SQLServerDriver", SQLServerAdapter.class.getName());
        adaptersByDriver.put("org.apache.derby.jdbc.EmbeddedDriver", DerbyAdapter.class.getName());
        adaptersByDriver.put("jdbc.FrontBase.FBJDriver", FrontBaseAdapter.class.getName());
        adaptersByDriver.put("com.ingres.jdbc.IngresDriver", IngresAdapter.class.getName());
        adaptersByDriver.put("org.sqlite.JDBC", SQLiteAdapter.class.getName());
        adaptersByDriver.put("org.firebirdsql.jdbc.FBDriver", FirebirdAdapter.class.getName());
    }

    public String getIconName() {
        return "icon-eomodel.png";
    }

    public void performAction(ActionEvent event) {
        importEOModel();
    }

    /**
     * Allows user to select an EOModel, then imports it as a DataMap.
     */
    protected void importEOModel() {
        JFileChooser fileChooser = getEOModelChooser();
        int status = fileChooser.showOpenDialog(application.getFrameController().getView());

        if (status == JFileChooser.APPROVE_OPTION) {

            // save preferences
            FSPath lastDir = application
                    .getFrameController()
                    .getLastEOModelDirectory();
            lastDir.updateFromChooser(fileChooser);

            File file = fileChooser.getSelectedFile();
            if (file.isFile()) {
                file = file.getParentFile();
            }

            DataMap currentMap = getProjectController().getSelectedDataMap();

            try {
                URL url = file.toURI().toURL();

                EOModelProcessor processor = new EOModelProcessor();

                // load DataNode if we are not merging with an existing map
                if (currentMap == null) {
                    loadDataNode(processor.loadModeIndex(url));
                }

                // load DataMap
                DataMap map = processor.loadEOModel(url);
                addDataMap(map, currentMap);

            } catch (Exception ex) {
                LOGGER.info("EOModel Loading Exception", ex);
                ErrorsController.guiException(ex);
            }

        }
    }

    protected void loadDataNode(Map eomodelIndex) {
        // if this is JDBC or JNDI node and connection dictionary is specified, load a
        // DataNode, otherwise ignore it (meaning that pre 5.* EOModels will not have a
        // node).

        String adapter = (String) eomodelIndex.get("adaptorName");
        Map<?, ?> connection = (Map) eomodelIndex.get("connectionDictionary");

        if (adapter != null && connection != null) {
            CreateNodeAction nodeBuilder = application.getActionManager().getAction(CreateNodeAction.class);

            // this should make created node current, resulting in the new map being added
            // to the node automatically once it is loaded
            DataNodeDescriptor node = nodeBuilder.buildDataNode();

            // configure node...
            if ("JNDI".equalsIgnoreCase(adapter)) {
                node.setDataSourceFactoryType(JNDIDataSourceFactory.class.getName());
                node.setParameters((String) connection.get("serverUrl"));
            } else {

                // guess adapter from plugin or driver
                String cayenneAdapter = adaptersByEofPlugin.get(connection.get("plugin"));
                if (cayenneAdapter == null) {
                    cayenneAdapter = adaptersByDriver.get(connection.get("driver"));
                }

                if (cayenneAdapter != null) {
                    try {
                        Class<DbAdapter> adapterClass = application
                                .getClassLoader()
                                .loadClass(DbAdapter.class, cayenneAdapter);
                        node.setAdapterType(adapterClass.toString());
                    } catch (Throwable ex) {
                        // ignore...
                    }
                }

                node.setDataSourceFactoryType(XMLPoolingDataSourceFactory.class.getName());

                DataSourceDescriptor dsi = node.getDataSourceDescriptor();
                dsi.setDataSourceUrl(keyAsString(connection, "URL"));
                dsi.setJdbcDriver(keyAsString(connection, "driver"));
                dsi.setPassword(keyAsString(connection, "password"));
                dsi.setUserName(keyAsString(connection, "username"));
            }

            DataChannelDescriptor domain = (DataChannelDescriptor) getProjectController()
                    .getProject()
                    .getRootNode();
            domain.getNodeDescriptors().add(node);

            // send events after the node creation is complete
            getProjectController().fireDataNodeEvent(
                    DataNodeEvent.ofAdd(this, node));
            getProjectController().displayDataNode(
                    new DataNodeDisplayEvent(
                            this,
                            (DataChannelDescriptor) getProjectController()
                                    .getProject()
                                    .getRootNode(),
                            node));
        }
    }

    // CAY-246 - if user name or password is all numeric, it will
    // be returned as number, so we can't cast dictionary keys to String
    private String keyAsString(Map map, String key) {
        Object value = map.get(key);
        return (value != null) ? value.toString() : null;
    }

    /**
     * Adds DataMap into the project.
     */
    protected void addDataMap(DataMap map, DataMap currentMap) {

        ProjectController controller = getProjectController();

        if (currentMap != null) {
            // merge with existing map... have to memorize map state before and after
            // to do the right events

            Collection<ObjEntity> originalOE = new ArrayList<>(currentMap.getObjEntities());
            Collection<DbEntity> originalDE = new ArrayList<>(currentMap.getDbEntities());
            Collection<QueryDescriptor> originalQueries = new ArrayList<>(currentMap.getQueryDescriptors());

            currentMap.mergeWithDataMap(map);
            map = currentMap;

            // postprocess changes
            Collection<ObjEntity> newOE = new ArrayList<>(currentMap.getObjEntities());
            Collection<DbEntity> newDE = new ArrayList<>(currentMap.getDbEntities());
            Collection<QueryDescriptor> newQueries = new ArrayList<>(currentMap.getQueryDescriptors());

            Object src = application.getFrameController().getView();

            // 1. ObjEntities
            Collection<ObjEntity> addedOE = new ArrayList<>(newOE);
            addedOE.removeAll(originalOE);
            for (ObjEntity e : addedOE) {
                controller.fireObjEntityEvent(ObjEntityEvent.ofAdd(src, e));
            }

            Collection<ObjEntity> removedOE = new ArrayList<>(originalOE);
            removedOE.removeAll(newOE);
            for (ObjEntity e : removedOE) {
                controller.fireObjEntityEvent(ObjEntityEvent.ofRemove(src, e));
            }

            // 2. DbEntities
            Collection<DbEntity> addedDE = new ArrayList<>(newDE);
            addedDE.removeAll(originalDE);
            for (DbEntity e : addedDE) {
                controller.fireDbEntityEvent(DbEntityEvent.ofAdd(src, e));
            }

            Collection<DbEntity> removedDE = new ArrayList<>(originalDE);
            removedDE.removeAll(newDE);
            for (DbEntity e : removedDE) {
                controller.fireDbEntityEvent(DbEntityEvent.ofRemove(src, e));
            }

            // 3. queries
            Collection<QueryDescriptor> addedQueries = new ArrayList<>(newQueries);
            addedQueries.removeAll(originalQueries);
            for (QueryDescriptor q : addedQueries) {
                controller.fireQueryEvent(QueryEvent.ofAdd(src, q));
            }

            Collection<QueryDescriptor> removedQueries = new ArrayList<>(originalQueries);
            removedQueries.removeAll(newQueries);
            for (QueryDescriptor q : removedQueries) {
                controller.fireQueryEvent(QueryEvent.ofRemove(src, q));
            }

            controller.displayDataMap(new DataMapDisplayEvent(
                    application.getFrameController().getView(),
                    map,
                    (DataChannelDescriptor) controller
                            .getProject()
                            .getRootNode(),
                    controller.getSelectedDataNode()));
        } else {
            // fix DataMap name, as there maybe a map with the same name already
            ConfigurationNode root = controller.getProject().getRootNode();
            map.setName(NameBuilder
                    .builder(map, root)
                    .baseName(map.getName())
                    .name());

            // side effect of this operation is that if a node was created, this DataMap
            // will be linked with it...
            CreateDataMapAction.onMapCreated(application.getFrameController().getView(), getProjectController(), map);

        }
    }

    /**
     * Returns EOModel chooser.
     */
    public JFileChooser getEOModelChooser() {

        if (eoModelChooser == null) {
            eoModelChooser = new EOModelChooser("Select EOModel");
        }

        FSPath lastDir = application.getFrameController().getLastEOModelDirectory();
        lastDir.updateChooser(eoModelChooser);

        return eoModelChooser;
    }

    /**
     * Custom file chooser that will pop up again if a bad directory is selected.
     */
    static class EOModelChooser extends JFileChooser {

        protected FileFilter selectFilter;
        protected JDialog cachedDialog;

        public EOModelChooser(String title) {
            super.setFileFilter(FileFilters.getEOModelFilter());
            super.setDialogTitle(title);
            super.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            this.selectFilter = FileFilters.getEOModelSelectFilter();
        }

        public int showOpenDialog(Component parent) {
            int status = super.showOpenDialog(parent);
            if (status != JFileChooser.APPROVE_OPTION) {
                cachedDialog = null;
                return status;
            }

            // make sure invalid directory is not selected
            File file = this.getSelectedFile();
            if (selectFilter.accept(file)) {
                cachedDialog = null;
                return JFileChooser.APPROVE_OPTION;
            } else {
                if (file.isDirectory()) {
                    this.setCurrentDirectory(file);
                }

                return this.showOpenDialog(parent);
            }
        }

        protected JDialog createDialog(Component parent) throws HeadlessException {

            if (cachedDialog == null) {
                cachedDialog = super.createDialog(parent);
            }
            return cachedDialog;
        }
    }
}
