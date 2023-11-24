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

package org.apache.cayenne.modeler.editor.datanode;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;

import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.ThrowOnPartialOrCreateSchemaStrategy;
import org.apache.cayenne.access.dbsync.ThrowOnPartialSchemaStrategy;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.configuration.runtime.XMLPoolingDataSourceFactory;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.DataNodeDefaults;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.ProjectUtil;
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
	private final static String XML_POOLING_DATA_SOURCE_FACTORY = XMLPoolingDataSourceFactory.class.getName();

	private final static String[] STANDARD_DATA_SOURCE_FACTORIES = new String[] {
			DataSourceFactoryType.CAYENNE.getLabel(),
			DataSourceFactoryType.CUSTOM.getLabel()
	};

	private final static String[] STANDARD_SCHEMA_UPDATE_STRATEGY = new String[] {
	        SkipSchemaUpdateStrategy.class.getName(),
			CreateIfNoSchemaStrategy.class.getName(),
            ThrowOnPartialSchemaStrategy.class.getName(),
			ThrowOnPartialOrCreateSchemaStrategy.class.getName()
	};

	protected MainDataNodeView view;
	protected DataNodeEditor tabbedPaneController;
	protected DataNodeDescriptor node;
	protected Map<String, DataSourceEditor> datasourceEditors;

	protected CustomDataSourceEditor defaultSubeditor;
	protected BindingDelegate nodeChangeProcessor;
	protected ObjectBinding[] bindings;
	protected ObjectBinding localDataSourceBinding;

	public MainDataNodeEditor(ProjectController parent, DataNodeEditor tabController) {

		super(parent);

		this.tabbedPaneController = tabController;
		this.view = new MainDataNodeView();
		this.datasourceEditors = new HashMap<>();

		this.nodeChangeProcessor = (binding, oldValue, newValue) -> {

            DataNodeEvent e = new DataNodeEvent(MainDataNodeEditor.this, node);
            if (binding != null && binding.getView() == view.getDataNodeName()) {
                e.setOldName(oldValue != null ? oldValue.toString() : null);
            }

            ((ProjectController) getParent()).fireDataNodeEvent(e);
        };

		this.defaultSubeditor = new CustomDataSourceEditor(parent, nodeChangeProcessor);

		initController();
	}

	// ======= properties

	public Component getView() {
		return view;
	}

	public String getFactoryName() {
		return XML_POOLING_DATA_SOURCE_FACTORY.equals(node.getDataSourceFactoryType())
				? DataSourceFactoryType.CAYENNE.getLabel()
				: DataSourceFactoryType.CUSTOM.getLabel();
	}

	public void setFactoryName(String factoryName) {
		if (node != null) {
			if(DataSourceFactoryType.CAYENNE.getLabel().equals(factoryName)) {
				node.setDataSourceFactoryType(XML_POOLING_DATA_SOURCE_FACTORY);
			} else {
				node.setDataSourceFactoryType(defaultSubeditor.getFactoryName());
			}
			showDataSourceSubview(factoryName);
		}
	}

	public String getSchemaUpdateStrategy() {
		return (node != null) ? node.getSchemaUpdateStrategyType() : null;
	}

	public void setSchemaUpdateStrategy(String schemaUpdateStrategy) {
		if (node != null) {
			node.setSchemaUpdateStrategyType(schemaUpdateStrategy);
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
		DataNodeDefaults oldPref = parent.getDataNodePreferences();
		DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) getApplication().getProject()
				.getRootNode();

		Collection<DataNodeDescriptor> matchingNode = dataChannelDescriptor.getNodeDescriptors();
        for (DataNodeDescriptor node : matchingNode) {
            if (node.getName().equals(newName)) {
                // there is an entity with the same name
                throw new ValidationException("There is another DataNode named '" + newName
                        + "'. Use a different name.");
            }
        }

		// passed validation, set value...
		ProjectUtil.setDataNodeName(node, newName);

		oldPref.copyPreferences(newName);
	}

	protected void initController() {
		view.getDataSourceDetail().add(defaultSubeditor.getView(), "default");
		view.getFactories().setEditable(false);
		// init combo box choices
		view.getFactories().setModel(new DefaultComboBoxModel<>(STANDARD_DATA_SOURCE_FACTORIES));

		view.getSchemaUpdateStrategy().setEditable(true);
		view.getSchemaUpdateStrategy().setModel(new DefaultComboBoxModel<>(STANDARD_SCHEMA_UPDATE_STRATEGY));

		// init listeners
		((ProjectController) getParent()).addDataNodeDisplayListener(e -> refreshView(e.getDataNode()));

		getView().addComponentListener(new ComponentAdapter() {

			public void componentShown(ComponentEvent e) {
				refreshView(node != null ? node : ((ProjectController) getParent()).getCurrentDataNode());
			}
		});

		BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);

		localDataSourceBinding = builder.bindToComboSelection(view.getLocalDataSources(),
				"parent.dataNodePreferences.localDataSource", NO_LOCAL_DATA_SOURCE);

		// use delegate for the rest of them

		builder.setDelegate(nodeChangeProcessor);

		bindings = new ObjectBinding[4];
		bindings[0] = builder.bindToTextField(view.getDataNodeName(), "nodeName");
		bindings[1] = builder.bindToComboSelection(view.getFactories(), "factoryName");
		bindings[2] = builder.bindToComboSelection(view.getSchemaUpdateStrategy(), "schemaUpdateStrategy");
		bindings[3] = builder.bindToTextField(view.getCustomAdapter(), "adapterName");

		// one way bindings
		builder.bindToAction(view.getConfigLocalDataSources(), "dataSourceConfigAction()");
	}

	public void dataSourceConfigAction() {
		PreferenceDialog prefs = new PreferenceDialog(this);
		prefs.showDataSourceEditorAction(view.getLocalDataSources().getSelectedItem());
		refreshLocalDataSources();
	}

	protected void refreshLocalDataSources() {
		@SuppressWarnings("unchecked")
        Map<String, Object> sources = (Map<String, Object>)getApplication().getCayenneProjectPreferences()
                .getDetailObject(DBConnectionInfo.class).getChildrenPreferences();

		int len = sources.size();
        String[] keys = new String[len + 1];

		// a slight chance that a real datasource is called
		// NO_LOCAL_DATA_SOURCE...
		keys[0] = NO_LOCAL_DATA_SOURCE;

		String[] dataSources = sources.keySet().toArray(new String[0]);
        System.arraycopy(dataSources, 0, keys, 1, dataSources.length);

		view.getLocalDataSources().setModel(new DefaultComboBoxModel<>(keys));
		localDataSourceBinding.updateView();
	}

	/**
	 * Reinitializes widgets to display selected DataNode.
	 */
	protected void refreshView(DataNodeDescriptor node) {
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

		DataSourceEditor c = datasourceEditors.get(factoryName);
		// create subview dynamically...
		if (c == null) {
			if (DataSourceFactoryType.CAYENNE.getLabel().equals(factoryName)) {
				c = new JDBCDataSourceEditor((ProjectController) getParent(), nodeChangeProcessor);
			} else {
				// special case - no detail view, just show it and bail..
				defaultSubeditor.setNode(node);
				view.getDataSourceDetailLayout().show(view.getDataSourceDetail(), "default");
				return;
			}

			datasourceEditors.put(factoryName, c);
			view.getDataSourceDetail().add(c.getView(), factoryName);

			// this is needed to display freshly added panel...
			view.getDataSourceDetail().getParent().validate();
		}

		// this will refresh subview...
		c.setNode(node);
		// display the right subview...
		view.getDataSourceDetailLayout().show(view.getDataSourceDetail(), factoryName);
	}

	public String getAdapterName() {
		return node.getAdapterType();
	}

	public void setAdapterName(String name) {
		node.setAdapterType(name);
	}

	enum DataSourceFactoryType {
		CAYENNE("Cayenne Data Source Factory"),
		CUSTOM("Custom Data Source Factory");
		private final String label;

		DataSourceFactoryType(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}
}
