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

package org.apache.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import org.apache.cayenne.datasource.DriverDataSource;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.FileClassLoadingService;
import org.apache.cayenne.modeler.event.DataSourceModificationEvent;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.CayennePreferenceEditor;
import org.apache.cayenne.pref.ChildrenMapPreference;
import org.apache.cayenne.pref.PreferenceEditor;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.util.Util;

/**
 * Editor for the local DataSources configured in preferences.
 * 
 */
public class DataSourcePreferences extends CayenneController {

	protected DataSourcePreferencesView view;
	protected String dataSourceKey;
	protected Map dataSources;
	protected ChildrenMapPreference dataSourcePreferences;
	protected CayennePreferenceEditor editor;

	public DataSourcePreferences(PreferenceDialog parentController) {
		super(parentController);

		this.view = new DataSourcePreferencesView(this);

		PreferenceEditor editor = parentController.getEditor();
		if (editor instanceof CayennePreferenceEditor) {
			this.editor = (CayennePreferenceEditor) editor;
		}

		// init view data
		this.dataSourcePreferences = getApplication().getCayenneProjectPreferences().getDetailObject(
				DBConnectionInfo.class);
		this.dataSources = dataSourcePreferences.getChildrenPreferences();

		Object[] keys = dataSources.keySet().toArray();
		Arrays.sort(keys);
		DefaultComboBoxModel dataSourceModel = new DefaultComboBoxModel(keys);
		view.getDataSources().setModel(dataSourceModel);

		initBindings();

		// show first item
		if (keys.length > 0) {
			view.getDataSources().setSelectedIndex(0);
			editDataSourceAction();
		}
	}

	public Component getView() {
		return view;
	}

	protected void initBindings() {
		BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);
		builder.bindToAction(view.getAddDataSource(), "newDataSourceAction()");
		builder.bindToAction(view.getDuplicateDataSource(), "duplicateDataSourceAction()");
		builder.bindToAction(view.getRemoveDataSource(), "removeDataSourceAction()");
		builder.bindToAction(view.getTestDataSource(), "testDataSourceAction()");

		builder.bindToComboSelection(view.getDataSources(), "dataSourceKey");
	}

	public Map getDataSources() {
		return dataSources;
	}

	public String getDataSourceKey() {
		return dataSourceKey;
	}

	public void setDataSourceKey(String dataSourceKey) {
		this.dataSourceKey = dataSourceKey;
		editDataSourceAction();
	}

	public DBConnectionInfo getConnectionInfo() {
		return (DBConnectionInfo) dataSourcePreferences.getObject(dataSourceKey);
	}

	/**
	 * Shows a dialog to create new local DataSource configuration.
	 */
	public void newDataSourceAction() {

		DataSourceCreator creatorWizard = new DataSourceCreator(this);
		DBConnectionInfo dataSource = creatorWizard.startupAction();

		if (dataSource != null) {
			dataSourcePreferences.create(creatorWizard.getName(), dataSource);
			dataSources = dataSourcePreferences.getChildrenPreferences();

			Object[] keys = dataSources.keySet().toArray();
			Arrays.sort(keys);
			view.getDataSources().setModel(new DefaultComboBoxModel(keys));
			view.getDataSources().setSelectedItem(creatorWizard.getName());
			editDataSourceAction();
			fireEvent(creatorWizard.getName(), MapEvent.ADD);
		}
	}

	/**
	 * Shows a dialog to duplicate an existing local DataSource configuration.
	 */
	public void duplicateDataSourceAction() {
		Object selected = view.getDataSources().getSelectedItem();
		if (selected != null) {
			DataSourceDuplicator wizard = new DataSourceDuplicator(this, selected.toString());
			DBConnectionInfo dataSource = wizard.startupAction();

			if (dataSource != null) {
				dataSourcePreferences.create(wizard.getName(), dataSource);
				dataSources = dataSourcePreferences.getChildrenPreferences();

				Object[] keys = dataSources.keySet().toArray();
				Arrays.sort(keys);
				view.getDataSources().setModel(new DefaultComboBoxModel(keys));
				view.getDataSources().setSelectedItem(wizard.getName());
				editDataSourceAction();
				fireEvent(wizard.getName(), MapEvent.ADD);
			}
		}
	}

	/**
	 * Removes current DataSource.
	 */
	public void removeDataSourceAction() {
		String key = getDataSourceKey();
		if (key != null) {
			dataSourcePreferences.remove(key);

			dataSources = dataSourcePreferences.getChildrenPreferences();
			Object[] keys = dataSources.keySet().toArray();
			Arrays.sort(keys);
			view.getDataSources().setModel(new DefaultComboBoxModel(keys));
			editDataSourceAction(keys.length > 0 ? keys[0] : null);
			fireEvent(key, MapEvent.REMOVE);
		}
	}

	private void fireEvent(String dataSourceKey, int eventId) {
		DataSourceModificationEvent event = new DataSourceModificationEvent(this, dataSourceKey, eventId);
		getApplication().getFrameController().getProjectController().fireDataSourceModificationEvent(event);
	}

	/**
	 * Opens specified DataSource in the editor.
	 */
	public void editDataSourceAction(Object dataSourceKey) {
		view.getDataSources().setSelectedItem(dataSourceKey);
		editDataSourceAction();
	}

	/**
	 * Opens current DataSource in the editor.
	 */
	public void editDataSourceAction() {
		this.view.getDataSourceEditor().setConnectionInfo(getConnectionInfo());
	}

	/**
	 * Tries to establish a DB connection, reporting the status of this
	 * operation.
	 */
	public void testDataSourceAction() {
		DBConnectionInfo currentDataSource = getConnectionInfo();
		if (currentDataSource == null) {
			return;
		}

		if (currentDataSource.getJdbcDriver() == null) {
			JOptionPane.showMessageDialog(null, "No JDBC Driver specified", "Warning", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (currentDataSource.getUrl() == null) {
			JOptionPane.showMessageDialog(null, "No Database URL specified", "Warning", JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {

			FileClassLoadingService classLoader = new FileClassLoadingService();

			List<File> oldPathFiles = ((FileClassLoadingService) getApplication().getClassLoadingService())
					.getPathFiles();

			Collection<String> details = new ArrayList<>();
			for (File oldPathFile : oldPathFiles) {
				details.add(oldPathFile.getAbsolutePath());
			}

			Preferences classPathPreferences = getApplication().getPreferencesNode(ClasspathPreferences.class, "");
			if (editor.getChangedPreferences().containsKey(classPathPreferences)) {
				Map<String, String> map = editor.getChangedPreferences().get(classPathPreferences);

				for (Map.Entry<String, String> en : map.entrySet()) {
					String key = en.getKey();
					if (!details.contains(key)) {
						details.add(key);
					}
				}
			}

			if (editor.getRemovedPreferences().containsKey(classPathPreferences)) {
				Map<String, String> map = editor.getRemovedPreferences().get(classPathPreferences);

				for (Map.Entry<String, String> en : map.entrySet()) {
					String key = en.getKey();
					if (details.contains(key)) {
						details.remove(key);
					}
				}
			}

			if (details.size() > 0) {
				classLoader.setPathFiles(details.stream().map(File::new).collect(Collectors.toList()));
			}

			Class<Driver> driverClass = classLoader.loadClass(Driver.class, currentDataSource.getJdbcDriver());
			Driver driver = driverClass.newInstance();

			// connect via Cayenne DriverDataSource - it addresses some driver
			// issues...
			// can't use try with resource here as we can loose meaningful exception
			Connection c = new DriverDataSource(driver, currentDataSource.getUrl(),
					currentDataSource.getUserName(), currentDataSource.getPassword()).getConnection();
			try {
				c.close();
			} catch (SQLException ignored) {
				// i guess we can ignore this...
			}

			JOptionPane.showMessageDialog(null, "Connected Successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
		} catch (Throwable th) {
			th = Util.unwindException(th);
			String message = "Error connecting to DB: " + th.getLocalizedMessage();

			StringTokenizer st = new StringTokenizer(message);
			StringBuilder sbMessage = new StringBuilder();
			int len = 0;

			String tempString;
			while (st.hasMoreTokens()) {
				tempString = st.nextElement().toString();
				if (len < 110) {
					len = len + tempString.length() + 1;
				} else {
					sbMessage.append("\n");
					len = 0;
				}
				sbMessage.append(tempString).append(" ");
			}

			JOptionPane.showMessageDialog(null, sbMessage.toString(), "Warning", JOptionPane.WARNING_MESSAGE);
		}
	}
}
