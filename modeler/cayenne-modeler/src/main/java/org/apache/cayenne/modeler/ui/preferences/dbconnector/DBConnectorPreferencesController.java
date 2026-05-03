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

package org.apache.cayenne.modeler.ui.preferences.dbconnector;

import org.apache.cayenne.datasource.DriverDataSource;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.dbconnector.DBConnector;
import org.apache.cayenne.modeler.dbconnector.DBConnectors;
import org.apache.cayenne.modeler.service.classloader.ModelerClassLoader;
import org.apache.cayenne.modeler.ui.preferences.PreferenceDialogController;
import org.apache.cayenne.modeler.ui.preferences.dbconnector.creator.DBConnectorCreatorController;
import org.apache.cayenne.modeler.ui.preferences.dbconnector.duplicator.DBConnectorDuplicatorController;
import org.apache.cayenne.util.Util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

/**
 * Editor for the local DB Connectors configured in preferences.
 */
public class DBConnectorPreferencesController extends ChildController<PreferenceDialogController> {

	private final DBConnectorPreferencesView view;
	private final DBConnectors registry;
	private final Map<String, DBConnector> connectors;
	private final Set<String> toRemove;
	private String connectorName;

	public DBConnectorPreferencesController(PreferenceDialogController parent) {
		super(parent);

		this.view = new DBConnectorPreferencesView(this);

		// init view data — work on a snapshot of the live registry; commit/discard on Save/Revert
		this.registry = getApplication().getDbConnectors();
		this.connectors = new LinkedHashMap<>();
		registry.getAll().forEach((name, connector) -> connectors.put(name, copyOf(connector)));
		this.toRemove = new HashSet<>();

		Object[] keys = connectors.keySet().toArray();
		Arrays.sort(keys);
		DefaultComboBoxModel<Object> connectorModel = new DefaultComboBoxModel<>(keys);
		view.getConnectors().setModel(connectorModel);

		initBindings();

		// show first item
		if (keys.length > 0) {
			view.getConnectors().setSelectedIndex(0);
			editConnectorAction();
		}
	}

	public Component getView() {
		return view;
	}

	protected void initBindings() {
		view.getAddConnector().addActionListener(e -> newConnectorAction());
		view.getDuplicateConnector().addActionListener(e -> duplicateConnectorAction());
		view.getRemoveConnector().addActionListener(e -> removeConnectorAction());
		view.getTestConnector().addActionListener(e -> testConnectorAction());

		view.getConnectors().addActionListener(e -> {
			Object sel = view.getConnectors().getSelectedItem();
			setConnectorName(sel != null ? sel.toString() : null);
		});
	}

	public Map<String, DBConnector> getConnectors() {
		return connectors;
	}

	public String getConnectorName() {
		return connectorName;
	}

	public void setConnectorName(String connectorName) {
		this.connectorName = connectorName;
		editConnectorAction();
	}

	public DBConnector getConnectionInfo() {
		return connectors.get(connectorName);
	}

	/**
	 * Adds a new entry to the working snapshot. Sub-dialogs (creator/duplicator) call this
	 * after validating uniqueness against {@link #getConnectors()}.
	 */
	public DBConnector create(String name) {
		DBConnector info = new DBConnector();
		connectors.put(name, info);
		toRemove.remove(name);
		return info;
	}

	/**
	 * Apply the working snapshot to the live registry. Called on dialog Save.
	 */
	public void commit() {
		for (String name : toRemove) {
			registry.remove(name);
		}
		toRemove.clear();

		connectors.forEach(registry::put);
	}

	/**
	 * Drop the working snapshot. Called on dialog Cancel — registry is unchanged.
	 */
	public void discard() {
		// working snapshot lives in this controller and is GC-eligible after dialog dispose
	}

	private static DBConnector copyOf(DBConnector src) {
		DBConnector copy = new DBConnector();
		src.copyTo(copy);
		return copy;
	}

	/**
	 * Shows a dialog to create a new local DB Connector configuration.
	 */
	public void newConnectorAction() {
		DBConnectorCreatorController creatorWizard = new DBConnectorCreatorController(this);
		DBConnector connector = creatorWizard.startupAction();

		if (connector != null) {
			Object[] keys = connectors.keySet().toArray();
			Arrays.sort(keys);
			view.getConnectors().setModel(new DefaultComboBoxModel<>(keys));
			view.getConnectors().setSelectedItem(creatorWizard.getName());
			editConnectorAction();
		}
	}

	/**
	 * Shows a dialog to duplicate an existing local DB Connector configuration.
	 */
	public void duplicateConnectorAction() {
		Object selected = view.getConnectors().getSelectedItem();
		if (selected != null) {
			DBConnectorDuplicatorController wizard = new DBConnectorDuplicatorController(this, selected.toString());
			DBConnector connector = wizard.startupAction();

			if (connector != null) {
				Object[] keys = connectors.keySet().toArray();
				Arrays.sort(keys);
				view.getConnectors().setModel(new DefaultComboBoxModel<>(keys));
				view.getConnectors().setSelectedItem(wizard.getName());
				editConnectorAction();
			}
		}
	}

	/**
	 * Removes current DB Connector.
	 */
	public void removeConnectorAction() {
		String key = getConnectorName();
		if (key != null) {
			connectors.remove(key);
			toRemove.add(key);

			Object[] keys = connectors.keySet().toArray();
			Arrays.sort(keys);
			view.getConnectors().setModel(new DefaultComboBoxModel<>(keys));
			editConnectorAction(keys.length > 0 ? keys[0] : null);
		}
	}

	/**
	 * Opens specified DB Connector in the editor.
	 */
	public void editConnectorAction(Object connectorKey) {
		view.getConnectors().setSelectedItem(connectorKey);
		editConnectorAction();
	}

	/**
	 * Opens current DB Connector in the editor.
	 */
	public void editConnectorAction() {
		this.view.getConnectorEditor().setConnectionInfo(getConnectionInfo());
	}

	/**
	 * Tries to establish a DB connection, reporting the status of this operation.
	 */
	public void testConnectorAction() {
		DBConnector currentConnector = getConnectionInfo();
		if (currentConnector == null) {
			return;
		}

		if (currentConnector.getJdbcDriver() == null) {
			JOptionPane.showMessageDialog(null, "No JDBC Driver specified", "Warning", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (currentConnector.getUrl() == null) {
			JOptionPane.showMessageDialog(null, "No Database URL specified", "Warning", JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {

			ModelerClassLoader classLoader = new ModelerClassLoader();

			List<String> details = parent.getClasspathPrefsController().getEntries();
			if (!details.isEmpty()) {
				classLoader.setFiles(details.stream().map(File::new).collect(Collectors.toList()));
			}

			Class<Driver> driverClass = classLoader.loadClass(Driver.class, currentConnector.getJdbcDriver());
			Driver driver = driverClass.getDeclaredConstructor().newInstance();

			// connect via Cayenne DriverDataSource - it addresses some driver
			// issues...
			// can't use try with resource here as we can loose meaningful exception
			Connection c = new DriverDataSource(driver, currentConnector.getUrl(),
					currentConnector.getUserName(), currentConnector.getPassword()).getConnection();
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
