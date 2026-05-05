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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.datasource.DriverDataSource;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dbconnector.DBConnector;
import org.apache.cayenne.modeler.dbconnector.DBConnectors;
import org.apache.cayenne.modeler.service.classloader.ModelerClassLoader;
import org.apache.cayenne.modeler.toolkit.AppPanel;
import org.apache.cayenne.modeler.ui.preferences.classpath.ClasspathPrefsPanel;
import org.apache.cayenne.modeler.ui.preferences.dbconnector.creator.DBConnectorCreatorDialog;
import org.apache.cayenne.modeler.ui.preferences.dbconnector.duplicator.DBConnectorDuplicatorDialog;
import org.apache.cayenne.util.Util;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
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
 * Editor panel for the local DB Connectors configured in preferences.
 */
public class DBConnectorPrefsPanel extends AppPanel {

    private final ClasspathPrefsPanel classpathPrefs;
    private final DBConnectorEditor editor;
    private final JComboBox<Object> connectorChoices;
    private final JButton addConnectorButton;
    private final JButton duplicateConnectorButton;
    private final JButton removeConnectorButton;
    private final JButton testConnectorButton;

    private final DBConnectors registry;
    private final Map<String, DBConnector> connectors;
    private final Set<String> toRemove;
    private String connectorName;

    public DBConnectorPrefsPanel(Application app, ClasspathPrefsPanel classpathPrefs) {
        super(app);
        this.classpathPrefs = classpathPrefs;
        this.editor = new DBConnectorEditor();
        this.connectorChoices = new JComboBox<>();
        this.addConnectorButton = new JButton("New...");
        this.duplicateConnectorButton = new JButton("Duplicate...");
        this.removeConnectorButton = new JButton("Delete");
        this.testConnectorButton = new JButton("Test...");

        // working snapshot of the live registry; commit/discard on Save/Cancel
        this.registry = app.getDbConnectors();
        this.connectors = new LinkedHashMap<>();
        registry.getAll().forEach((name, connector) -> connectors.put(name, copyOf(connector)));
        this.toRemove = new HashSet<>();

        initLayout();
        initBindings();

        Object[] keys = connectors.keySet().toArray();
        Arrays.sort(keys);
        setConnectors(keys);

        if (keys.length > 0) {
            connectorChoices.setSelectedItem(keys[0]);
            editConnectorAction();
        }
    }

    public Map<String, DBConnector> getConnectors() {
        return connectors;
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
        // working snapshot lives in this panel and is GC-eligible after dialog dispose
    }

    /**
     * Opens specified DB Connector in the editor.
     */
    public void editConnectorAction(Object connectorKey) {
        connectorChoices.setSelectedItem(connectorKey);
        editConnectorAction();
    }

    private void editConnectorAction() {
        editor.setConnector(getConnectionInfo());
    }

    private void initLayout() {
        CellConstraints cc = new CellConstraints();
        PanelBuilder sidebar = new PanelBuilder(new FormLayout(
                "fill:min(150dlu;pref)",
                "p, 10dlu, p, 3dlu, p, 3dlu, p, 10dlu, p"));
        sidebar.setDefaultDialogBorder();

        sidebar.add(connectorChoices, cc.xy(1, 1));
        sidebar.add(addConnectorButton, cc.xy(1, 3));
        sidebar.add(duplicateConnectorButton, cc.xy(1, 5));
        sidebar.add(removeConnectorButton, cc.xy(1, 7));
        sidebar.add(testConnectorButton, cc.xy(1, 9));

        PanelBuilder editorBuilder = new PanelBuilder(new FormLayout(
                "fill:default:grow",
                "p, 3dlu, fill:default:grow"));
        editorBuilder.setDefaultDialogBorder();
        editorBuilder.addSeparator("Edit DB Connector", cc.xy(1, 1));
        editorBuilder.add(editor, cc.xy(1, 3));

        setLayout(new BorderLayout());
        add(editorBuilder.getPanel(), BorderLayout.CENTER);
        add(sidebar.getPanel(), BorderLayout.EAST);
    }

    private void initBindings() {
        addConnectorButton.addActionListener(e -> addConnectorClicked());
        duplicateConnectorButton.addActionListener(e -> {
            Object selected = connectorChoices.getSelectedItem();
            duplicateConnectorClicked(selected != null ? selected.toString() : null);
        });
        removeConnectorButton.addActionListener(e -> removeConnectorClicked());
        testConnectorButton.addActionListener(e -> testConnectorClicked());
        connectorChoices.addActionListener(e -> {
            Object sel = connectorChoices.getSelectedItem();
            this.connectorName = sel != null ? sel.toString() : null;
            editConnectorAction();
        });
    }

    private void setConnectors(Object[] keys) {
        connectorChoices.setModel(new DefaultComboBoxModel<>(keys));
    }

    private void addConnectorClicked() {
        DBConnectorCreatorDialog creator = new DBConnectorCreatorDialog(app(), SwingUtilities.getWindowAncestor(this), this);
        DBConnector connector = creator.startupAction();

        if (connector != null) {
            Object[] keys = connectors.keySet().toArray();
            Arrays.sort(keys);
            setConnectors(keys);
            connectorChoices.setSelectedItem(creator.getName());
            editConnectorAction();
        }
    }

    private void duplicateConnectorClicked(String prototypeKey) {
        if (prototypeKey == null) {
            return;
        }
        DBConnectorDuplicatorDialog duplicator = new DBConnectorDuplicatorDialog(
                app(), SwingUtilities.getWindowAncestor(this), this, prototypeKey);
        DBConnector connector = duplicator.startupAction();

        if (connector != null) {
            Object[] keys = connectors.keySet().toArray();
            Arrays.sort(keys);
            setConnectors(keys);
            connectorChoices.setSelectedItem(duplicator.getName());
            editConnectorAction();
        }
    }

    private void removeConnectorClicked() {
        String key = connectorName;
        if (key != null) {
            connectors.remove(key);
            toRemove.add(key);

            Object[] keys = connectors.keySet().toArray();
            Arrays.sort(keys);
            setConnectors(keys);
            editConnectorAction(keys.length > 0 ? keys[0] : null);
        }
    }

    private void testConnectorClicked() {
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

            List<String> details = classpathPrefs.getEntries();
            if (!details.isEmpty()) {
                classLoader.setFiles(details.stream().map(File::new).collect(Collectors.toList()));
            }

            Class<Driver> driverClass = classLoader.loadClass(Driver.class, currentConnector.getJdbcDriver());
            Driver driver = driverClass.getDeclaredConstructor().newInstance();

            // connect via Cayenne DriverDataSource - it addresses some driver
            // issues...
            // can't use try with resource here as we can lose meaningful exception
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

    private static DBConnector copyOf(DBConnector src) {
        DBConnector copy = new DBConnector();
        src.copyTo(copy);
        return copy;
    }
}
