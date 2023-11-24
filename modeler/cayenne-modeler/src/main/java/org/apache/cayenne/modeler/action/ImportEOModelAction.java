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

package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.configuration.runtime.JNDIDataSourceFactory;
import org.apache.cayenne.configuration.runtime.XMLPoolingDataSourceFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.AdapterMapping;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.FileFilters;
import org.apache.cayenne.wocompat.EOModelProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Action handler for WebObjects EOModel import function.
 * 
 */
public class ImportEOModelAction extends CayenneAction {

    private static Logger logObj = LoggerFactory.getLogger(ImportEOModelAction.class);

    public static String getActionName() {
        return "Import EOModel";
    }

    protected JFileChooser eoModelChooser;

    public ImportEOModelAction(Application application) {
        super(getActionName(), application);
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
        int status = fileChooser.showOpenDialog(Application.getFrame());

        if (status == JFileChooser.APPROVE_OPTION) {

            // save preferences
            FSPath lastDir = getApplication()
                    .getFrameController()
                    .getLastEOModelDirectory();
            lastDir.updateFromChooser(fileChooser);

            File file = fileChooser.getSelectedFile();
            if (file.isFile()) {
                file = file.getParentFile();
            }

            DataMap currentMap = getProjectController().getCurrentDataMap();

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

            }
            catch (Exception ex) {
                logObj.info("EOModel Loading Exception", ex);
                ErrorDebugDialog.guiException(ex);
            }

        }
    }

    protected void loadDataNode(Map eomodelIndex) {
        // if this is JDBC or JNDI node and connection dictionary is specified, load a
        // DataNode, otherwise ignore it (meaning that pre 5.* EOModels will not have a
        // node).

        String adapter = (String) eomodelIndex.get("adaptorName");
        Map connection = (Map) eomodelIndex.get("connectionDictionary");

        if (adapter != null && connection != null) {
            CreateNodeAction nodeBuilder = getApplication().getActionManager().getAction(CreateNodeAction.class);

            // this should make created node current, resulting in the new map being added
            // to the node automatically once it is loaded
            DataNodeDescriptor node = nodeBuilder.buildDataNode();

            // configure node...
            if ("JNDI".equalsIgnoreCase(adapter)) {
                node.setDataSourceFactoryType(JNDIDataSourceFactory.class.getName());
                node.setParameters((String) connection.get("serverUrl"));
            } else {
                // guess adapter from plugin or driver
                AdapterMapping adapterDefaults = getApplication().getAdapterMapping();
                String cayenneAdapter = adapterDefaults.adapterForEOFPluginOrDriver(
                        (String) connection.get("plugin"),
                        (String) connection.get("driver"));
                if (cayenneAdapter != null) {
                    try {
                        Class<DbAdapter> adapterClass = getApplication()
                                .getClassLoadingService()
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
                    new DataNodeEvent(this, node, MapEvent.ADD));
            getProjectController().fireDataNodeDisplayEvent(
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

        ProjectController mediator = getProjectController();

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

            EntityEvent entityEvent = new EntityEvent(Application.getFrame(), null);
            QueryEvent queryEvent = new QueryEvent(Application.getFrame(), null);

            // 1. ObjEntities
            Collection<ObjEntity> addedOE = new ArrayList<>(newOE);
            addedOE.removeAll(originalOE);
            for (ObjEntity e : addedOE) {
                entityEvent.setEntity(e);
                entityEvent.setId(MapEvent.ADD);
                mediator.fireObjEntityEvent(entityEvent);
            }

            Collection<ObjEntity> removedOE = new ArrayList<>(originalOE);
            removedOE.removeAll(newOE);
            for (ObjEntity e : removedOE) {
                entityEvent.setEntity(e);
                entityEvent.setId(MapEvent.REMOVE);
                mediator.fireObjEntityEvent(entityEvent);
            }

            // 2. DbEntities
            Collection<DbEntity> addedDE = new ArrayList<>(newDE);
            addedDE.removeAll(originalDE);
            for(DbEntity e: addedDE) {
                entityEvent.setEntity(e);
                entityEvent.setId(MapEvent.ADD);
                mediator.fireDbEntityEvent(entityEvent);
            }

            Collection<DbEntity> removedDE = new ArrayList<>(originalDE);
            removedDE.removeAll(newDE);
            for(DbEntity e: removedDE) {
                entityEvent.setEntity(e);
                entityEvent.setId(MapEvent.REMOVE);
                mediator.fireDbEntityEvent(entityEvent);
            }

            // 3. queries
            Collection<QueryDescriptor> addedQueries = new ArrayList<>(newQueries);
            addedQueries.removeAll(originalQueries);
            for(QueryDescriptor q: addedQueries) {
                queryEvent.setQuery(q);
                queryEvent.setId(MapEvent.ADD);
                mediator.fireQueryEvent(queryEvent);
            }

            Collection<QueryDescriptor> removedQueries = new ArrayList<>(originalQueries);
            removedQueries.removeAll(newQueries);
            for(QueryDescriptor q: removedQueries) {
                queryEvent.setQuery(q);
                queryEvent.setId(MapEvent.REMOVE);
                mediator.fireQueryEvent(queryEvent);
            }

            mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(Application
                    .getFrame(), map, (DataChannelDescriptor) mediator
                    .getProject()
                    .getRootNode(), mediator.getCurrentDataNode()));
        }
        else {
            // fix DataMap name, as there maybe a map with the same name already
            ConfigurationNode root = mediator.getProject().getRootNode();
            map.setName(NameBuilder
                    .builder(map, root)
                    .baseName(map.getName())
                    .name());

            // side effect of this operation is that if a node was created, this DataMap
            // will be linked with it...
            mediator.addDataMap(Application.getFrame(), map);
        }
    }

    /**
     * Returns EOModel chooser.
     */
    public JFileChooser getEOModelChooser() {

        if (eoModelChooser == null) {
            eoModelChooser = new EOModelChooser("Select EOModel");
        }

        FSPath lastDir = getApplication().getFrameController().getLastEOModelDirectory();
        lastDir.updateChooser(eoModelChooser);

        return eoModelChooser;
    }

    /**
     * Custom file chooser that will pop up again if a bad directory is selected.
     */
    class EOModelChooser extends JFileChooser {

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
            }
            else {
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
