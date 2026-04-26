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

package org.apache.cayenne.modeler.ui.preferences.datasource;

import org.apache.cayenne.datasource.DriverDataSource;
import org.apache.cayenne.modeler.event.model.ModelEvent;
import org.apache.cayenne.modeler.event.model.DataSourceEvent;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.pref.ChildrenMapPreference;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.service.classloader.ModelerClassLoader;
import org.apache.cayenne.modeler.ui.preferences.PreferenceDialogController;
import org.apache.cayenne.modeler.ui.preferences.classpath.ClasspathPreferencesController;
import org.apache.cayenne.modeler.ui.preferences.datasource.creator.DataSourceCreatorController;
import org.apache.cayenne.modeler.ui.preferences.datasource.duplicator.DataSourceDuplicatorController;
import org.apache.cayenne.util.Util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Editor for the local DataSources configured in preferences.
 */
public class DataSourcePreferencesController extends ChildController<PreferenceDialogController> {

	private final DataSourcePreferencesView view;
	private String dataSourceKey;
	private Map dataSources;
	private final ChildrenMapPreference dataSourcePreferences;

	public DataSourcePreferencesController(PreferenceDialogController parent) {
		super(parent);

		this.view = new DataSourcePreferencesView(this);

		// init view data
		this.dataSourcePreferences = getApplication().getCayenneProjectPreferences().getDetailObject(
				DBConnectionInfo.class);
		this.dataSources = dataSourcePreferences.getChildrenPreferences();

		Object[] keys = dataSources.keySet().toArray();
		Arrays.sort(keys);
		DefaultComboBoxModel<Object> dataSourceModel = new DefaultComboBoxModel<>(keys);
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
		view.getAddDataSource().addActionListener(e -> newDataSourceAction());
		view.getDuplicateDataSource().addActionListener(e -> duplicateDataSourceAction());
		view.getRemoveDataSource().addActionListener(e -> removeDataSourceAction());
		view.getTestDataSource().addActionListener(e -> testDataSourceAction());

		view.getDataSources().addActionListener(e -> {
			Object sel = view.getDataSources().getSelectedItem();
			setDataSourceKey(sel != null ? sel.toString() : null);
		});
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
		DataSourceCreatorController creatorWizard = new DataSourceCreatorController(this);
		DBConnectionInfo dataSource = creatorWizard.startupAction();

		if (dataSource != null) {
			dataSourcePreferences.create(creatorWizard.getName(), dataSource);
			dataSources = dataSourcePreferences.getChildrenPreferences();

			Object[] keys = dataSources.keySet().toArray();
			Arrays.sort(keys);
			view.getDataSources().setModel(new DefaultComboBoxModel<>(keys));
			view.getDataSources().setSelectedItem(creatorWizard.getName());
			editDataSourceAction();
			fireEvent(DataSourceEvent.ofAdd(this, creatorWizard.getName()));
		}
	}

	/**
	 * Shows a dialog to duplicate an existing local DataSource configuration.
	 */
	public void duplicateDataSourceAction() {
		Object selected = view.getDataSources().getSelectedItem();
		if (selected != null) {
			DataSourceDuplicatorController wizard = new DataSourceDuplicatorController(this, selected.toString());
			DBConnectionInfo dataSource = wizard.startupAction();

			if (dataSource != null) {
				dataSourcePreferences.create(wizard.getName(), dataSource);
				dataSources = dataSourcePreferences.getChildrenPreferences();

				Object[] keys = dataSources.keySet().toArray();
				Arrays.sort(keys);
				view.getDataSources().setModel(new DefaultComboBoxModel<>(keys));
				view.getDataSources().setSelectedItem(wizard.getName());
				editDataSourceAction();
				fireEvent(DataSourceEvent.ofAdd(this, wizard.getName()));
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
			view.getDataSources().setModel(new DefaultComboBoxModel<>(keys));
			editDataSourceAction(keys.length > 0 ? keys[0] : null);
			fireEvent(DataSourceEvent.ofRemove(this, key));
		}
	}

	private void fireEvent(DataSourceEvent event) {
		getApplication().getFrameController().getProjectController().fireDataSourceEvent(event);
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

			ModelerClassLoader classLoader = new ModelerClassLoader();

			Set<File> oldPathFiles = getApplication().getClassLoader().getFiles();

			Collection<String> details = new ArrayList<>();
			for (File oldPathFile : oldPathFiles) {
				details.add(oldPathFile.getAbsolutePath());
			}

			Preferences classPathPreferences = getApplication().getPreferencesNode(ClasspathPreferencesController.class, "");
			if (parent.getContext().getChangedPreferences().containsKey(classPathPreferences)) {
				Map<String, String> map = parent.getContext().getChangedPreferences().get(classPathPreferences);

				for (Map.Entry<String, String> en : map.entrySet()) {
					String key = en.getKey();
					if (!details.contains(key)) {
						details.add(key);
					}
				}
			}

			if (parent.getContext().getRemovedPreferences().containsKey(classPathPreferences)) {
				Map<String, String> map = parent.getContext().getRemovedPreferences().get(classPathPreferences);

				for (Map.Entry<String, String> en : map.entrySet()) {
					String key = en.getKey();
                    details.remove(key);
				}
			}

			if (!details.isEmpty()) {
				classLoader.setFiles(details.stream().map(File::new).collect(Collectors.toList()));
			}

			Class<Driver> driverClass = classLoader.loadClass(Driver.class, currentDataSource.getJdbcDriver());
			Driver driver = driverClass.getDeclaredConstructor().newInstance();

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
