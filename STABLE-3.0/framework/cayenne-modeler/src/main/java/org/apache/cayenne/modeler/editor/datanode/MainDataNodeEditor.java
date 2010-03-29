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

package org.apache.cayenne.modeler.editor.datanode;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.ThrowOnPartialOrCreateSchemaStrategy;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.ThrowOnPartialSchemaStrategy;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.DBCPDataSourceFactory;
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.conf.JNDIDataSourceFactory;
import org.apache.cayenne.map.event.DataNodeEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.DataNodeDisplayListener;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.project.ApplicationProject;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.BindingDelegate;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.validation.ValidationException;

/**
 * A controller for the main tab of the DataNode editor panel.
 * 
 */
public class MainDataNodeEditor extends CayenneController {

    protected static final String NO_LOCAL_DATA_SOURCE = "Select DataSource for Local Work...";

    final static String[] standardDataSourceFactories = new String[] {
            DriverDataSourceFactory.class.getName(),
            JNDIDataSourceFactory.class.getName(), DBCPDataSourceFactory.class.getName()
    };

    final static String[] standardSchemaUpdateStrategy = new String[] {
            SkipSchemaUpdateStrategy.class.getName(),
            CreateIfNoSchemaStrategy.class.getName(),
            ThrowOnPartialSchemaStrategy.class.getName(),
            ThrowOnPartialOrCreateSchemaStrategy.class.getName()
    };

    protected MainDataNodeView view;
    protected DataNodeEditor tabbedPaneController;
    protected DataNode node;
    protected Map datasourceEditors;
    protected Map localDataSources;

    protected DataSourceEditor defaultSubeditor;
    protected BindingDelegate nodeChangeProcessor;
    protected ObjectBinding[] bindings;
    protected ObjectBinding localDataSourceBinding;

    public MainDataNodeEditor(ProjectController parent, DataNodeEditor tabController) {

        super(parent);

        this.tabbedPaneController = tabController;
        this.view = new MainDataNodeView((ProjectController) getParent());
        this.datasourceEditors = new HashMap();
        this.localDataSources = new HashMap();

        this.nodeChangeProcessor = new BindingDelegate() {

            public void modelUpdated(
                    ObjectBinding binding,
                    Object oldValue,
                    Object newValue) {

                DataNodeEvent e = new DataNodeEvent(MainDataNodeEditor.this, node);
                if (binding != null && binding.getView() == view.getDataNodeName()) {
                    e.setOldName(oldValue != null ? oldValue.toString() : null);
                }

                ((ProjectController) getParent()).fireDataNodeEvent(e);
            }
        };

        this.defaultSubeditor = new CustomDataSourceEditor(parent, nodeChangeProcessor);

        initController();
    }

    // ======= properties

    public Component getView() {
        return view;
    }

    public String getFactoryName() {
        return (node != null) ? node.getDataSourceFactory() : null;
    }

    public void setFactoryName(String factoryName) {
        if (node != null) {
            node.setDataSourceFactory(factoryName);
            showDataSourceSubview(factoryName);
        }
    }

    public String getSchemaUpdateStrategy() {
        return (node != null) ? node.getSchemaUpdateStrategyName() : null;
    }

    public void setSchemaUpdateStrategy(String schemaUpdateStrategy) {
        if (node != null) {
            node.setSchemaUpdateStrategyName(schemaUpdateStrategy);
        }
    }

    public String getNodeName() {
        return (node != null) ? node.getName() : null;
    }

    public void setNodeName(String newName) {
        if (node == null) {
            return;
        }

        // validate...
        if (newName == null) {
            throw new ValidationException("Empty DataNode Name");
        }

        ProjectController parent = (ProjectController) getParent();
        Configuration config = ((ApplicationProject) parent.getProject())
                .getConfiguration();

        DataNode matchingNode = null;

        for (DataDomain domain : config.getDomains()) {
            DataNode nextNode = domain.getNode(newName);

            if (nextNode == node) {
                continue;
            }

            if (nextNode != null) {
                matchingNode = nextNode;
                break;
            }
        }

        if (matchingNode != null) {
            // there is an entity with the same name
            throw new ValidationException("There is another DataNode named '"
                    + newName
                    + "'. Use a different name.");
        }

        // passed validation, set value...

        // TODO: fixme....there is a slight chance that domain is different than the one
        // cached node belongs to
        ProjectUtil.setDataNodeName(parent.getCurrentDataDomain(), node, newName);
    }

    // ======== other stuff

    protected void initController() {
        view.getDataSourceDetail().add(defaultSubeditor.getView(), "default");
        view.getFactories().setEditable(true);
        // init combo box choices
        view.getFactories().setModel(
                new DefaultComboBoxModel(standardDataSourceFactories));

        view.getSchemaUpdateStrategy().setEditable(true);
        view.getSchemaUpdateStrategy().setModel(
                new DefaultComboBoxModel(standardSchemaUpdateStrategy));

        // init listeners
        ((ProjectController) getParent())
                .addDataNodeDisplayListener(new DataNodeDisplayListener() {

                    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
                        refreshView(e.getDataNode());
                    }
                });

        getView().addComponentListener(new ComponentAdapter() {

            public void componentShown(ComponentEvent e) {
                refreshView(node != null ? node : ((ProjectController) getParent())
                        .getCurrentDataNode());
            }
        });

        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        localDataSourceBinding = builder.bindToComboSelection(
                view.getLocalDataSources(),
                "parent.dataNodePreferences.localDataSource",
                NO_LOCAL_DATA_SOURCE);

        // use delegate for the rest of them

        builder.setDelegate(nodeChangeProcessor);

        bindings = new ObjectBinding[3];
        bindings[0] = builder.bindToTextField(view.getDataNodeName(), "nodeName");
        bindings[1] = builder.bindToComboSelection(view.getFactories(), "factoryName");
        bindings[2] = builder.bindToComboSelection(
                view.getSchemaUpdateStrategy(),
                "schemaUpdateStrategy");

        // one way bindings
        builder
                .bindToAction(
                        view.getConfigLocalDataSources(),
                        "dataSourceConfigAction()");
    }

    public void dataSourceConfigAction() {
        PreferenceDialog prefs = new PreferenceDialog(this);
        prefs.showDataSourceEditorAction(view.getLocalDataSources().getSelectedItem());
        refreshLocalDataSources();
    }

    protected void refreshLocalDataSources() {
        localDataSources.clear();

        Collection sources = getApplication().getPreferenceDomain().getDetails(
                DBConnectionInfo.class);

        int len = sources.size();
        Object[] keys = new Object[len + 1];

        // a slight chance that a real datasource is called NO_LOCAL_DATA_SOURCE...
        keys[0] = NO_LOCAL_DATA_SOURCE;
        Iterator it = sources.iterator();
        for (int i = 1; i <= len; i++) {
            DBConnectionInfo info = (DBConnectionInfo) it.next();
            keys[i] = info.getKey();
            localDataSources.put(keys[i], info);
        }

        view.getLocalDataSources().setModel(new DefaultComboBoxModel(keys));
        localDataSourceBinding.updateView();
    }

    /**
     * Reinitializes widgets to display selected DataNode.
     */
    protected void refreshView(DataNode node) {
        this.node = node;

        if (node == null) {
            getView().setVisible(false);
            return;
        }

        refreshLocalDataSources();

        for (ObjectBinding binding : bindings) {
            binding.updateView();
        }

        showDataSourceSubview(getFactoryName());
    }

    /**
     * Selects a subview for a currently selected DataSource factory.
     */
    protected void showDataSourceSubview(String factoryName) {
        DataSourceEditor c = (DataSourceEditor) datasourceEditors.get(factoryName);

        // create subview dynamically...
        if (c == null) {

            if (DriverDataSourceFactory.class.getName().equals(factoryName)) {
                c = new JDBCDataSourceEditor(
                        (ProjectController) getParent(),
                        nodeChangeProcessor);
            }
            else if (JNDIDataSourceFactory.class.getName().equals(factoryName)) {
                c = new JNDIDataSourceEditor(
                        (ProjectController) getParent(),
                        nodeChangeProcessor);
            }
            else if (DBCPDataSourceFactory.class.getName().equals(factoryName)) {
                c = new DBCPDataSourceEditor(
                        (ProjectController) getParent(),
                        nodeChangeProcessor);
            }
            else {
                // special case - no detail view, just show it and bail..
                defaultSubeditor.setNode(node);
                disabledTab("default");
                view.getDataSourceDetailLayout().show(
                        view.getDataSourceDetail(),
                        "default");
                return;
            }

            datasourceEditors.put(factoryName, c);
            view.getDataSourceDetail().add(c.getView(), factoryName);

            // this is needed to display freshly added panel...
            view.getDataSourceDetail().getParent().validate();
        }

        // this will refresh subview...
        c.setNode(node);
        disabledTab(factoryName);
        // display the right subview...
        view.getDataSourceDetailLayout().show(view.getDataSourceDetail(), factoryName);

    }

    protected void disabledTab(String name) {

        if (name.equals(standardDataSourceFactories[0])) {
            tabbedPaneController.getTabComponent().setEnabledAt(2, true);
        }
        else {
            tabbedPaneController.getTabComponent().setEnabledAt(2, false);
        }
    }

}
