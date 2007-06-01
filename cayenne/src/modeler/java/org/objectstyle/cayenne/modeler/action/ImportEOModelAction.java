/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.modeler.action;

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.conf.JNDIDataSourceFactory;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.event.DataNodeEvent;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.AdapterMapping;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.dialog.ErrorDebugDialog;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.modeler.pref.FSPath;
import org.objectstyle.cayenne.modeler.util.CayenneAction;
import org.objectstyle.cayenne.modeler.util.FileFilters;
import org.objectstyle.cayenne.project.NamedObjectFactory;
import org.objectstyle.cayenne.project.ProjectDataSource;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.wocompat.EOModelProcessor;

/**
 * Action handler for WebObjects EOModel import function.
 * 
 * @author Andrei Adamchik
 */
public class ImportEOModelAction extends CayenneAction {

    private static final Logger logObj = Logger.getLogger(ImportEOModelAction.class);

    public static String getActionName() {
        return "Import EOModel";
    }

    protected JFileChooser eoModelChooser;

    public ImportEOModelAction(Application application) {
        super(getActionName(), application);
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
                logObj.log(Level.INFO, "EOModel Loading Exception", ex);
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
                    new DataNodeEvent(this, node, DataNodeEvent.ADD));
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

            currentMap.mergeWithDataMap(map);
            map = currentMap;

            // postprocess changes
            Collection newOE = new ArrayList(currentMap.getObjEntities());
            Collection newDE = new ArrayList(currentMap.getDbEntities());

            EntityEvent entityEvent = new EntityEvent(Application.getFrame(), null);

            Collection addedOE = CollectionUtils.subtract(newOE, originalOE);
            Iterator it = addedOE.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                entityEvent.setEntity(e);
                entityEvent.setId(EntityEvent.ADD);
                mediator.fireObjEntityEvent(entityEvent);
            }

            Collection removedOE = CollectionUtils.subtract(originalOE, newOE);
            it = removedOE.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                entityEvent.setEntity(e);
                entityEvent.setId(EntityEvent.REMOVE);
                mediator.fireObjEntityEvent(entityEvent);
            }

            Collection addedDE = CollectionUtils.subtract(newDE, originalDE);
            it = addedDE.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                entityEvent.setEntity(e);
                entityEvent.setId(EntityEvent.ADD);
                mediator.fireDbEntityEvent(entityEvent);
            }

            Collection removedDE = CollectionUtils.subtract(originalDE, newDE);
            it = removedDE.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                entityEvent.setEntity(e);
                entityEvent.setId(EntityEvent.REMOVE);
                mediator.fireDbEntityEvent(entityEvent);
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