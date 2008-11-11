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


package org.apache.cayenne.modeler.action;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.conf.JNDIDataSourceFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.event.DataNodeEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.AdapterMapping;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.FileFilters;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.project.ProjectDataSource;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.wocompat.EOModelProcessor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Action handler for WebObjects EOModel import function.
 * 
 */
public class ImportEOModelAction extends CayenneAction {

    private static Log logObj = LogFactory.getLog(ImportEOModelAction.class);

    public static String getActionName() {
        return "Import EOModel";
    }

    protected JFileChooser eoModelChooser;

    public ImportEOModelAction(Application application) {
        super(getActionName(), application);
    }
    
    public String getIconName() {
        return "icon-eomodel.gif";
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
                String path = file.getCanonicalPath();

                EOModelProcessor processor = new EOModelProcessor();

                // load DataNode if we are not merging with an existing map
                if (currentMap == null) {
                    loadDataNode(processor.loadModeIndex(path));
                }

                // load DataMap
                DataMap map = processor.loadEOModel(path);
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
            CreateNodeAction nodeBuilder = (CreateNodeAction) getApplication().getAction(
                    CreateNodeAction.getActionName());

            // this should make created node current, resulting in the new map being added
            // to the node automatically once it is loaded
            DataNode node = nodeBuilder.buildDataNode();

            // configure node...
            if ("JNDI".equalsIgnoreCase(adapter)) {
                node.setDataSourceFactory(JNDIDataSourceFactory.class.getName());
                node.setDataSourceLocation((String) connection.get("serverUrl"));
            }
            else {
                // guess adapter from plugin or driver
                AdapterMapping adapterDefaults = getApplication().getAdapterMapping();
                String cayenneAdapter = adapterDefaults.adapterForEOFPluginOrDriver(
                        (String) connection.get("plugin"),
                        (String) connection.get("driver"));
                if (cayenneAdapter != null) {
                    try {
                        Class adapterClass = getApplication()
                                .getClassLoadingService()
                                .loadClass(cayenneAdapter);
                        node.setAdapter((DbAdapter) adapterClass.newInstance());
                    }
                    catch (Throwable ex) {
                        // ignore...
                    }
                }

                node.setDataSourceFactory(DriverDataSourceFactory.class.getName());

                DataSourceInfo dsi = ((ProjectDataSource) node.getDataSource())
                        .getDataSourceInfo();
                
                
                
                dsi.setDataSourceUrl(keyAsString(connection, "URL"));
                dsi.setJdbcDriver(keyAsString(connection, "driver"));
                dsi.setPassword(keyAsString(connection, "password"));
                dsi.setUserName(keyAsString(connection, "username"));
            }

            // send events after the node creation is complete
            getProjectController().fireDataNodeEvent(
                    new DataNodeEvent(this, node, MapEvent.ADD));
            getProjectController().fireDataNodeDisplayEvent(
                    new DataNodeDisplayEvent(this, getProjectController()
                            .getCurrentDataDomain(), node));
        }
    }
    
    // CAY-246 - if user name or password is all numeric, it will
    // be returned as number, so we can't cast dictionary keys to String
    private String keyAsString(Map map, String key) {
        Object value = map.get(key);
        return (value != null) ? value.toString() : null;
    }

    /**
     * Returns <code>true</code> if path contains a DataDomain object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(DataDomain.class) != null;
    }

    /**
     * Adds DataMap into the project.
     */
    protected void addDataMap(DataMap map, DataMap currentMap) {

        ProjectController mediator = getProjectController();

        if (currentMap != null) {
            // merge with existing map... have to memorize map state before and after
            // to do the right events

            Collection originalOE = new ArrayList(currentMap.getObjEntities());
            Collection originalDE = new ArrayList(currentMap.getDbEntities());
            Collection originalQueries = new ArrayList(currentMap.getQueries());

            currentMap.mergeWithDataMap(map);
            map = currentMap;

            // postprocess changes
            Collection newOE = new ArrayList(currentMap.getObjEntities());
            Collection newDE = new ArrayList(currentMap.getDbEntities());
            Collection newQueries = new ArrayList(currentMap.getQueries());

            EntityEvent entityEvent = new EntityEvent(Application.getFrame(), null);
            QueryEvent queryEvent = new QueryEvent(Application.getFrame(), null);

            Collection addedOE = CollectionUtils.subtract(newOE, originalOE);
            Iterator it = addedOE.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                entityEvent.setEntity(e);
                entityEvent.setId(MapEvent.ADD);
                mediator.fireObjEntityEvent(entityEvent);
            }

            Collection removedOE = CollectionUtils.subtract(originalOE, newOE);
            it = removedOE.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                entityEvent.setEntity(e);
                entityEvent.setId(MapEvent.REMOVE);
                mediator.fireObjEntityEvent(entityEvent);
            }

            Collection addedDE = CollectionUtils.subtract(newDE, originalDE);
            it = addedDE.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                entityEvent.setEntity(e);
                entityEvent.setId(MapEvent.ADD);
                mediator.fireDbEntityEvent(entityEvent);
            }

            Collection removedDE = CollectionUtils.subtract(originalDE, newDE);
            it = removedDE.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                entityEvent.setEntity(e);
                entityEvent.setId(MapEvent.REMOVE);
                mediator.fireDbEntityEvent(entityEvent);
            }
            
            // queries
            Collection addedQueries = CollectionUtils.subtract(newQueries, originalQueries);
            it = addedQueries.iterator();
            while (it.hasNext()) {
                Query q = (Query) it.next();
                queryEvent.setQuery(q);
                queryEvent.setId(MapEvent.ADD);
                mediator.fireQueryEvent(queryEvent);
            }

            Collection removedQueries = CollectionUtils.subtract(originalQueries, newQueries);
            it = removedQueries.iterator();
            while (it.hasNext()) {
            	Query q = (Query) it.next();
                queryEvent.setQuery(q);
                queryEvent.setId(MapEvent.REMOVE);
                mediator.fireQueryEvent(queryEvent);
            }

            mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(Application
                    .getFrame(), map, mediator.getCurrentDataDomain(), mediator
                    .getCurrentDataNode()));
        }
        else {
            // fix DataMap name, as there maybe a map with the same name already
            DataDomain domain = mediator.getCurrentDataDomain();
            map.setName(NamedObjectFactory.createName(DataMap.class, domain, map
                    .getName()));

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
