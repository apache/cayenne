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
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.GetDbConnectionAction;
import org.apache.cayenne.modeler.dialog.pref.GeneralPreferences;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.modeler.event.DataSourceModificationEvent;
import org.apache.cayenne.modeler.event.DataSourceModificationListener;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;

import javax.sql.DataSource;
import javax.swing.DefaultComboBoxModel;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.prefs.Preferences;

import static org.apache.cayenne.modeler.pref.DBConnectionInfo.DB_ADAPTER_PROPERTY;
import static org.apache.cayenne.modeler.pref.DBConnectionInfo.JDBC_DRIVER_PROPERTY;
import static org.apache.cayenne.modeler.pref.DBConnectionInfo.PASSWORD_PROPERTY;
import static org.apache.cayenne.modeler.pref.DBConnectionInfo.URL_PROPERTY;
import static org.apache.cayenne.modeler.pref.DBConnectionInfo.USER_NAME_PROPERTY;

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
	private ProjectController projectController;

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

		this.view = createView(title);
		this.view.setTitle(title);
		this.connectionInfo = new DBConnectionInfo();

		initBindings();
		initDataSourceListener();
	}

	private String[] getLabelsForDialog(String viewTitle) {
		switch (viewTitle) {
			case GetDbConnectionAction.DIALOG_TITLE: {
				return new String[]{"Save", "Cancel"};
			}
			default:
				return new String[]{"Continue", "Cancel"};
		}
	}

	/**
	 * Creates swing dialog for this wizard
	 */
	private DataSourceWizardView createView(String viewTitle) {
		String[] labels = getLabelsForDialog(viewTitle);
		return new DataSourceWizardView(this, labels);
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

	private DBConnectionInfo getConnectionInfoFromPreferences() {
		DBConnectionInfo connectionInfo = new DBConnectionInfo();
		DataMapDefaults dataMapDefaults = projectController.
				getDataMapPreferences(projectController.getCurrentDataMap());
		connectionInfo.setDbAdapter(dataMapDefaults.getCurrentPreference().get(DB_ADAPTER_PROPERTY, null));
		connectionInfo.setUrl(dataMapDefaults.getCurrentPreference().get(URL_PROPERTY, null));
		connectionInfo.setUserName(dataMapDefaults.getCurrentPreference().get(USER_NAME_PROPERTY, null));
		connectionInfo.setPassword(dataMapDefaults.getCurrentPreference().get(PASSWORD_PROPERTY, null));
		connectionInfo.setJdbcDriver(dataMapDefaults.getCurrentPreference().get(JDBC_DRIVER_PROPERTY, null));
		return connectionInfo;
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

		DataMapDefaults dataMapDefaults = projectController.
				getDataMapPreferences(projectController.getCurrentDataMap());
		if (dataMapDefaults.getCurrentPreference().get(DB_ADAPTER_PROPERTY, null) != null) {
			getConnectionInfoFromPreferences().copyTo(connectionInfo);
		}
		view.pack();
		view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		view.setModal(true);
		view.connectionInfo.setConnectionInfo(connectionInfo);
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
				reportError("Connection Error", ignore);
				return;
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

	public void setProjectController(ProjectController projectController) {
		this.projectController = projectController;
	}
}
