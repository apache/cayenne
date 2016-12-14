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
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.modeler.dialog.pref.GeneralPreferences;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.modeler.event.DataSourceModificationEvent;
import org.apache.cayenne.modeler.event.DataSourceModificationListener;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;

import javax.sql.DataSource;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * A subclass of ConnectionWizard that tests configured DataSource, but does not
 * keep an open connection.
 * 
 */
public class DataSourceWizard extends CayenneController {

	private DataSourceWizardView view;
	private ObjectBinding dataSourceBinding;
	private Map<String, DBConnectionInfo> dataSources;
	private String dataSourceKey;

	// this object is a clone of an object selected from the dropdown, as we
	// need to allow
	// local temporary modifications
	private DBConnectionInfo connectionInfo;

	private boolean canceled;

	private DataSourceModificationListener dataSourceListener;

	private DbAdapter adapter;
	private DataSource dataSource;

	public DataSourceWizard(CayenneController parent, String title) {
		super(parent);

		this.view = createView();
		this.view.setTitle(title);
		this.connectionInfo = new DBConnectionInfo();

		initBindings();
		initDataSourceListener();
	}

	/**
	 * Creates swing dialog for this wizard
	 */
	private DataSourceWizardView createView() {
		return new DataSourceWizardView(this);
	}

	protected void initBindings() {
		BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);

		dataSourceBinding = builder.bindToComboSelection(view.getDataSources(), "dataSourceKey");

		builder.bindToAction(view.getCancelButton(), "cancelAction()");
		builder.bindToAction(view.getOkButton(), "okAction()");
		builder.bindToAction(view.getConfigButton(), "dataSourceConfigAction()");
	}

	private void initDataSourceListener() {
		dataSourceListener = new DataSourceModificationListener() {
			@Override
			public void callbackDataSourceRemoved(DataSourceModificationEvent e) {}

			@Override
			public void callbackDataSourceAdded(DataSourceModificationEvent e) {
				setDataSourceKey(e.getDataSourceName());
				refreshDataSources();
			}
		};
		getApplication().getFrameController().getProjectController()
				.addDataSourceModificationListener(dataSourceListener);
	}

	private void initFavouriteDataSource() {
		Preferences pref = getApplication().getPreferencesNode(GeneralPreferences.class, "");
		String favouriteDataSource = pref.get(GeneralPreferences.FAVOURITE_DATA_SOURCE, null);
		if(favouriteDataSource != null && dataSources.containsKey(favouriteDataSource)) {
			setDataSourceKey(favouriteDataSource);
			dataSourceBinding.updateView();
		}
	}

	private void removeDataSourceListener() {
		getApplication().getFrameController().getProjectController()
				.removeDataSourceModificationListener(dataSourceListener);
	}

	public String getDataSourceKey() {
		return dataSourceKey;
	}

	public void setDataSourceKey(String dataSourceKey) {
		this.dataSourceKey = dataSourceKey;

		// update a clone object that will be used to obtain connection...
		DBConnectionInfo currentInfo = dataSources.get(dataSourceKey);
		if (currentInfo != null) {
			currentInfo.copyTo(connectionInfo);
		} else {
			connectionInfo = new DBConnectionInfo();
		}

		view.getConnectionInfo().setConnectionInfo(connectionInfo);
	}

	/**
	 * Main action method that pops up a dialog asking for user selection.
	 * Returns true if the selection was confirmed, false - if canceled.
	 */
	public boolean startupAction() {
		this.canceled = true;

		refreshDataSources();
		initFavouriteDataSource();

		view.pack();
		view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		view.setModal(true);
		makeCloseableOnEscape();
		centerView();
		view.setVisible(true);

		return !canceled;
	}

	public DBConnectionInfo getConnectionInfo() {
		return connectionInfo;
	}

	/**
	 * Tests that the entered information is valid and can be used to open a
	 * conneciton. Does not store the open connection.
	 */
	public void okAction() {
		DBConnectionInfo info = getConnectionInfo();
		ClassLoadingService classLoader = getApplication().getClassLoadingService();

		// doing connection testing...
		try {
			this.adapter = info.makeAdapter(classLoader);
			this.dataSource = info.makeDataSource(classLoader);
			try (Connection connection = dataSource.getConnection()) {
			} catch (SQLException ignore) {
			}
		} catch (Throwable th) {
			reportError("Connection Error", th);
			return;
		}
		onClose(false);
	}

	public void cancelAction() {
		onClose(true);
	}

	/**
	 * On close handler. Introduced to remove data source listener.
	 */
	protected void onClose(boolean canceled) {
		// set success flag, and unblock the caller...
		this.canceled = canceled;
		view.dispose();
		removeDataSourceListener();
		if(!canceled) {
			Preferences pref = getApplication().getPreferencesNode(GeneralPreferences.class, "");
			pref.put(GeneralPreferences.FAVOURITE_DATA_SOURCE, getDataSourceKey());
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

	@SuppressWarnings("unchecked")
	private void refreshDataSources() {
		this.dataSources = (Map<String, DBConnectionInfo>)getApplication().getCayenneProjectPreferences().getDetailObject(DBConnectionInfo.class)
				.getChildrenPreferences();

		// 1.2 migration fix - update data source adapter names
		final String _12package = "org.objectstyle.cayenne.";
		for(DBConnectionInfo info : dataSources.values()) {
			if (info.getDbAdapter() != null && info.getDbAdapter().startsWith(_12package)) {
				info.setDbAdapter("org.apache.cayenne." + info.getDbAdapter().substring(_12package.length()));
			}
		}

		String[] keys = dataSources.keySet().toArray(new String[0]);
		Arrays.sort(keys);
		view.getDataSources().setModel(new DefaultComboBoxModel<>(keys));

		String key = null;
		if (getDataSourceKey() == null || !dataSources.containsKey(getDataSourceKey())) {
			if (keys.length > 0) {
				key = keys[0];
			}
		}

		setDataSourceKey(key != null ? key : getDataSourceKey());
		dataSourceBinding.updateView();
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * Returns configured DbAdapter.
	 */
	public DbAdapter getAdapter() {
		return adapter;
	}
}
