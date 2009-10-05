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

package org.apache.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataRowStore;
import org.apache.cayenne.cache.MapQueryCacheFactory;
import org.apache.cayenne.cache.OSQueryCacheFactory;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.map.event.DomainEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.datadomain.CacheSyncConfigController;
import org.apache.cayenne.modeler.event.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.DomainDisplayListener;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.project.ApplicationProject;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Panel for editing DataDomain.
 */
public class DataDomainView extends JPanel implements DomainDisplayListener {

    final static String[] QUERY_CACHE_FACTORIES = new String[] {
            MapQueryCacheFactory.class.getName(), OSQueryCacheFactory.class.getName()
    };

    protected ProjectController projectController;

    protected TextAdapter name;
    protected TextAdapter cacheSize;
    protected JCheckBox objectValidation;
    protected JCheckBox externalTransactions;
    protected TextAdapter dataContextFactory;
    protected JComboBox queryCacheFactory;
    protected JCheckBox sharedCache;
    protected JCheckBox remoteUpdates;
    protected JButton configRemoteUpdates;

    public DataDomainView(ProjectController projectController) {
        this.projectController = projectController;

        // Create and layout components
        initView();

        // hook up listeners to widgets
        initController();
    }

    protected void initView() {

        // create widgets
        this.name = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDomainName(text);
            }
        };

        this.cacheSize = new TextAdapter(new JTextField(10)) {

            protected void updateModel(String text) {
                setCacheSize(text);
            }
        };

        this.dataContextFactory = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDomainProperty(DataDomain.DATA_CONTEXT_FACTORY_PROPERTY, text, null);
            }
        };

        this.objectValidation = new JCheckBox();
        this.externalTransactions = new JCheckBox();

        this.queryCacheFactory = CayenneWidgetFactory.createUndoableComboBox();

        this.sharedCache = new JCheckBox();
        this.remoteUpdates = new JCheckBox();
        this.configRemoteUpdates = new JButton("Configure...");
        configRemoteUpdates.setEnabled(false);

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:pref, 3dlu, fill:50dlu, 3dlu, fill:47dlu, 3dlu, fill:100",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("DataDomain Configuration", cc.xywh(1, 1, 7, 1));
        builder.addLabel("DataDomain Name:", cc.xy(1, 3));
        builder.add(name.getComponent(), cc.xywh(3, 3, 5, 1));

        builder.addLabel("DataContext Factory:", cc.xy(1, 5));
        builder.add(dataContextFactory.getComponent(), cc.xywh(3, 5, 5, 1));

        builder.addLabel("Object Validation:", cc.xy(1, 7));
        builder.add(objectValidation, cc.xy(3, 7));

        builder.addLabel("Container-Managed Transactions:", cc.xy(1, 9));
        builder.add(externalTransactions, cc.xy(3, 9));

        builder.addSeparator("Cache Configuration", cc.xywh(1, 11, 7, 1));
        builder.addLabel("Query Cache Factory:", cc.xy(1, 13));
        builder.add(queryCacheFactory, cc.xywh(3, 13, 5, 1));

        builder.addLabel("Size of Object Cache:", cc.xy(1, 15));
        builder.add(cacheSize.getComponent(), cc.xy(3, 15));

        builder.addLabel("Use Shared Cache:", cc.xy(1, 17));
        builder.add(sharedCache, cc.xy(3, 17));

        builder.addLabel("Remote Change Notifications:", cc.xy(1, 19));
        builder.add(remoteUpdates, cc.xy(3, 19));
        builder.add(configRemoteUpdates, cc.xy(7, 19));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    protected void initController() {
        projectController.addDomainDisplayListener(this);

        queryCacheFactory.setEditable(true);
        queryCacheFactory.setModel(new DefaultComboBoxModel(QUERY_CACHE_FACTORIES));

        queryCacheFactory.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setDomainProperty(
                        DataDomain.QUERY_CACHE_FACTORY_PROPERTY,
                        (String) queryCacheFactory.getModel().getSelectedItem(),
                        MapQueryCacheFactory.class.getName());
            }
        });

        // add action listener to checkboxes
        objectValidation.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String value = objectValidation.isSelected() ? "true" : "false";
                setDomainProperty(
                        DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
                        value,
                        Boolean.toString(DataDomain.VALIDATING_OBJECTS_ON_COMMIT_DEFAULT));
            }
        });

        externalTransactions.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String value = externalTransactions.isSelected() ? "true" : "false";
                setDomainProperty(
                        DataDomain.USING_EXTERNAL_TRANSACTIONS_PROPERTY,
                        value,
                        Boolean.toString(DataDomain.USING_EXTERNAL_TRANSACTIONS_DEFAULT));
            }
        });

        sharedCache.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String value = sharedCache.isSelected() ? "true" : "false";
                setDomainProperty(
                        DataDomain.SHARED_CACHE_ENABLED_PROPERTY,
                        value,
                        Boolean.toString(DataDomain.SHARED_CACHE_ENABLED_DEFAULT));

                // turning off shared cache should result in disabling remote events

                remoteUpdates.setEnabled(sharedCache.isSelected());

                if (!sharedCache.isSelected()) {
                    // uncheck remote updates...
                    remoteUpdates.setSelected(false);

                    setDomainProperty(
                            DataRowStore.REMOTE_NOTIFICATION_PROPERTY,
                            "false",
                            Boolean.toString(DataRowStore.REMOTE_NOTIFICATION_DEFAULT));
                }

                // depending on final remote updates status change button status
                configRemoteUpdates.setEnabled(remoteUpdates.isSelected());
            }
        });

        remoteUpdates.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String value = remoteUpdates.isSelected() ? "true" : "false";

                // update config button state
                configRemoteUpdates.setEnabled(remoteUpdates.isSelected());

                setDomainProperty(
                        DataRowStore.REMOTE_NOTIFICATION_PROPERTY,
                        value,
                        Boolean.toString(DataRowStore.REMOTE_NOTIFICATION_DEFAULT));
            }
        });

        configRemoteUpdates.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new CacheSyncConfigController(projectController).startup();
            }
        });
    }

    /**
     * Helper method that updates domain properties. If a value equals to default, null
     * value is used instead.
     */
    protected void setDomainProperty(String property, String value, String defaultValue) {

        DataDomain domain = projectController.getCurrentDataDomain();
        if (domain == null) {
            return;
        }

        // no empty strings
        if ("".equals(value)) {
            value = null;
        }

        // use NULL for defaults
        if (value != null && value.equals(defaultValue)) {
            value = null;
        }

        Map properties = domain.getProperties();
        Object oldValue = properties.get(property);
        if (!Util.nullSafeEquals(value, oldValue)) {
            properties.put(property, value);

            DomainEvent e = new DomainEvent(this, domain);
            projectController.fireDomainEvent(e);
        }
    }

    public String getDomainProperty(String property, String defaultValue) {
        DataDomain domain = projectController.getCurrentDataDomain();
        if (domain == null) {
            return null;
        }

        String value = (String) domain.getProperties().get(property);
        return value != null ? value : defaultValue;
    }

    public boolean getDomainBooleanProperty(String property, String defaultValue) {
        return "true".equalsIgnoreCase(getDomainProperty(property, defaultValue));
    }

    /**
     * Invoked on domain selection event. Updates view with the values from the currently
     * selected domain.
     */
    public void currentDomainChanged(DomainDisplayEvent e) {
        DataDomain domain = e.getDomain();
        if (null == domain) {
            return;
        }

        // extract values from the new domain object
        name.setText(domain.getName());

        cacheSize.setText(getDomainProperty(
                DataRowStore.SNAPSHOT_CACHE_SIZE_PROPERTY,
                Integer.toString(DataRowStore.SNAPSHOT_CACHE_SIZE_DEFAULT)));

        objectValidation.setSelected(getDomainBooleanProperty(
                DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
                Boolean.toString(DataDomain.VALIDATING_OBJECTS_ON_COMMIT_DEFAULT)));

        externalTransactions.setSelected(getDomainBooleanProperty(
                DataDomain.USING_EXTERNAL_TRANSACTIONS_PROPERTY,
                Boolean.toString(DataDomain.USING_EXTERNAL_TRANSACTIONS_DEFAULT)));
        dataContextFactory.setText(getDomainProperty(
                DataDomain.DATA_CONTEXT_FACTORY_PROPERTY,
                null));

        sharedCache.setSelected(getDomainBooleanProperty(
                DataDomain.SHARED_CACHE_ENABLED_PROPERTY,
                Boolean.toString(DataDomain.SHARED_CACHE_ENABLED_DEFAULT)));

        remoteUpdates.setSelected(getDomainBooleanProperty(
                DataRowStore.REMOTE_NOTIFICATION_PROPERTY,
                Boolean.toString(DataRowStore.REMOTE_NOTIFICATION_DEFAULT)));
        remoteUpdates.setEnabled(sharedCache.isSelected());
        configRemoteUpdates.setEnabled(remoteUpdates.isEnabled()
                && remoteUpdates.isSelected());

        queryCacheFactory.setSelectedItem(getDomainProperty(
                DataDomain.QUERY_CACHE_FACTORY_PROPERTY,
                MapQueryCacheFactory.class.getName()));
    }

    void setDomainName(String newName) {
        if (newName == null || newName.trim().length() == 0) {
            throw new ValidationException("Enter name for DataDomain");
        }

        Configuration configuration = ((ApplicationProject) Application.getProject())
                .getConfiguration();
        
        DataDomain domain = projectController.getCurrentDataDomain();

        DataDomain matchingDomain = configuration.getDomain(newName);

        if (matchingDomain == null) {
            Domain prefs = projectController.getPreferenceDomainForDataDomain();

            DomainEvent e = new DomainEvent(this, domain, domain.getName());
            ProjectUtil.setDataDomainName(configuration, domain, newName);
            prefs.rename(newName);
            projectController.fireDomainEvent(e);
        }
        else if (matchingDomain != domain) {
            throw new ValidationException("There is another DataDomain named '"
                    + newName
                    + "'. Use a different name.");
        }
    }

    void setCacheSize(String text) {
        if (text.length() > 0) {
            try {
                Integer.parseInt(text);
            }
            catch (NumberFormatException ex) {
                throw new ValidationException("Cache size must be an integer: " + text);
            }
        }

        setDomainProperty(DataRowStore.SNAPSHOT_CACHE_SIZE_PROPERTY, text, Integer
                .toString(DataRowStore.SNAPSHOT_CACHE_SIZE_DEFAULT));
    }
}
