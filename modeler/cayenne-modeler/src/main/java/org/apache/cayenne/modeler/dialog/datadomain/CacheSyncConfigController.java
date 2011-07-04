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

package org.apache.cayenne.modeler.dialog.datadomain;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.WindowConstants;

import org.apache.cayenne.access.DataRowStore;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.DomainEvent;
import org.apache.cayenne.event.JMSBridgeFactory;
import org.apache.cayenne.event.JavaGroupsBridgeFactory;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A controller for CacheSyncConfigDialog and its subviews. Builds a model out of a
 * DataDomain properties map, and on save updates DataDomain properties with configuration
 * changes made by the user. This controller manages one main dialog view, and its
 * subviews organized using CardLayout. Each subview as well as the main dialog have their
 * own independent models.
 * 
 */
public class CacheSyncConfigController extends CayenneController {

    private static Log logObj = LogFactory.getLog(CacheSyncConfigController.class);

    // using strings instead of the actioal factory classes, since we
    // JMS and JavaGroups libraries may not be around, and Modeler
    // may throw CNFE
    private static final String JGROUPS_FACTORY_CLASS = "org.apache.cayenne.event.JavaGroupsBridgeFactory";
    private static final String JMS_FACTORY_CLASS = "org.apache.cayenne.event.JMSBridgeFactory";

    public static final String SAVE_CONFIG_CONTROL = "Done";
    public static final String CANCEL_CONFIG_CONTROL = "Cancel";

    public static final String JGROUPS_DEFAULT_CONTROL = "Standard Configuration";

    public static final String JGROUPS_URL_CONTROL = "Use Configuration File";
    
    public static final String JGROUPS_FACTORY_LABEL = "JavaGroups Multicast (Default)";
    public static final String JMS_FACTORY_LABEL = "JMS Transport";
    public static final String CUSTOM_FACTORY_LABEL = "Custom Transport";

    protected Map existingCards;
    protected Map properties;
    protected boolean modified;
    protected ProjectController eventController;
    
    protected CacheSyncConfigView view;

    public CacheSyncConfigController(ProjectController eventController) {
        super(eventController);
        this.eventController = eventController;
    }

    /**
     * Creates and shows a new modal dialog window. Registers as a listener for its own
     * model to update subviews on model changes.
     */
    public void startup() { 
        DataChannelDescriptor domain = (DataChannelDescriptor)eventController.getProject().getRootNode();
        
        String factory = (String) domain.getProperties().get(
                DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY);

        view = new CacheSyncConfigView();
        initView();
        
        properties = new HashMap(((DataChannelDescriptor)eventController.getProject()
                .getRootNode()).getProperties());
        System.out.println(properties);
        
        // build cards, showing the one corresponding to DataDomain state
        prepareChildren(factory);
        
        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }
    
    public Component getView() {
        return this.view;
    }
    
    private void initView() {
        view.getCancelButton().addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                view.dispose();
            }
        });
        view.getSaveButton().addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                commitChanges();
            }
        });
        view.getTypeSelector().addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                selectCard();
            }
        });
    }
    
    protected void selectCard() {
        view.showCard((String)view.getTypeSelector().getSelectedItem());
    }

    protected void jgroupsDefaultConfig() {
        JGroupsConfigPanel view = (JGroupsConfigPanel) existingCards
                .get(JGROUPS_FACTORY_LABEL);
        if (view != null) {
            view.showDefaultConfig();
        }
    }

    protected void jgroupsURLConfig() {
        JGroupsConfigPanel view = (JGroupsConfigPanel) existingCards
                .get(JGROUPS_FACTORY_LABEL);
        if (view != null) {
            view.showCustomConfig();
        }
    }

    /**
     * Stores configuration changes in the data domain properties.
     */
    protected void commitChanges() {
        DataChannelDescriptor domain = (DataChannelDescriptor)eventController.getProject().getRootNode();
        logObj.warn("domain properties BEFORE: " + domain.getProperties());
        
        Map<String, String> props = domain.getProperties();
        
        String type = (String)view.getTypeSelector().getSelectedItem();
        if (JGROUPS_FACTORY_LABEL.equals(type)) {
            JGroupsConfigPanel jgroupsPanel = (JGroupsConfigPanel) existingCards
                    .get(JGROUPS_FACTORY_LABEL);
            props.put(DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY, JGROUPS_FACTORY_CLASS);
            if (jgroupsPanel.useConfigFile.isSelected()) {
                props.remove(JavaGroupsBridgeFactory.MCAST_ADDRESS_PROPERTY);
                props.remove(JavaGroupsBridgeFactory.MCAST_PORT_PROPERTY);
                if (!"".equals(jgroupsPanel.configURL.getText())) {
                    props.put(JavaGroupsBridgeFactory.JGROUPS_CONFIG_URL_PROPERTY, 
                            jgroupsPanel.configURL.getText());
                }
                else {
                    props.put(JavaGroupsBridgeFactory.JGROUPS_CONFIG_URL_PROPERTY, null);
                }
            }
            else {
                props.remove(JavaGroupsBridgeFactory.JGROUPS_CONFIG_URL_PROPERTY);
                if (!"".equals(jgroupsPanel.multicastAddress.getText())) {
                    props.put(JavaGroupsBridgeFactory.MCAST_ADDRESS_PROPERTY, 
                            jgroupsPanel.multicastAddress.getText());
                }
                else {
                    props.put(JavaGroupsBridgeFactory.MCAST_ADDRESS_PROPERTY, null);
                }
                if (!"".equals(jgroupsPanel.multicastPort.getText())) { 
                    props.put(JavaGroupsBridgeFactory.MCAST_PORT_PROPERTY, 
                            jgroupsPanel.multicastPort.getText());
                }
                else {
                    props.put(JavaGroupsBridgeFactory.MCAST_PORT_PROPERTY, null);
                }
            }
        }
        else if (JMS_FACTORY_LABEL.equals(type)) {
            JMSConfigPanel jmsPanel = (JMSConfigPanel) existingCards
                    .get(JMS_FACTORY_LABEL);
            props.put(DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY, JMS_FACTORY_CLASS);
            if (!"".equals(jmsPanel.topicFactory.getText())) {
                props.put(JMSBridgeFactory.TOPIC_CONNECTION_FACTORY_PROPERTY, 
                        jmsPanel.topicFactory.getText());
            }
            else {
                props.put(JMSBridgeFactory.TOPIC_CONNECTION_FACTORY_PROPERTY, null);
            }
        }
        else if (CUSTOM_FACTORY_LABEL.equals(type)) {
            CustomRemoteEventsConfigPanel customPanel = (CustomRemoteEventsConfigPanel) existingCards
                    .get(CUSTOM_FACTORY_LABEL);
            if (!"".equals(customPanel.factoryClass.getText())) {
                props.put(DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY, customPanel.factoryClass.getText());
            }
            else {
                props.put(DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY, null);
            }
        }
        
        logObj.warn("domain properties: " + domain.getProperties());
        
        eventController.fireDomainEvent(new DomainEvent(this, domain));

        view.dispose();
    }
    
    protected void loadProperties(String factory) {
        String configUrl = (String)properties.get(JavaGroupsBridgeFactory.JGROUPS_CONFIG_URL_PROPERTY);
        String multicastAddress = (String)properties.get(JavaGroupsBridgeFactory.MCAST_ADDRESS_PROPERTY);
        String multicastPort = (String)properties.get(JavaGroupsBridgeFactory.MCAST_PORT_PROPERTY);
        String topicFactory = (String)properties.get(JMSBridgeFactory.TOPIC_CONNECTION_FACTORY_PROPERTY);
        
        JGroupsConfigPanel jgroupsPanel = (JGroupsConfigPanel) existingCards
                .get(JGROUPS_FACTORY_LABEL);
        
        if (configUrl != null) {
            jgroupsPanel.useConfigFile.setSelected(true);
            jgroupsURLConfig();
            jgroupsPanel.configURL.setText(configUrl);
        }
        else {
            jgroupsPanel.useDefaultConfig.setSelected(true);
            jgroupsDefaultConfig();
        }
        
        if (multicastAddress != null) {
            jgroupsPanel.multicastAddress.setText(multicastAddress);
        }
        else {
            jgroupsPanel.multicastAddress.setText(JavaGroupsBridgeFactory.MCAST_ADDRESS_DEFAULT);
        }
        
        if (multicastPort != null) {
            jgroupsPanel.multicastPort.setText(multicastPort);
        }
        else {
            jgroupsPanel.multicastPort.setText(JavaGroupsBridgeFactory.MCAST_PORT_DEFAULT);
        }
        
        JMSConfigPanel jmsPanel = (JMSConfigPanel) existingCards
                .get(JMS_FACTORY_LABEL);
     
        if (topicFactory != null) {
            jmsPanel.topicFactory.setText(topicFactory);
        }
        else {
            jmsPanel.topicFactory.setText(JMSBridgeFactory.TOPIC_CONNECTION_FACTORY_DEFAULT);
        }
        
        CustomRemoteEventsConfigPanel customPanel = (CustomRemoteEventsConfigPanel) existingCards
                .get(CUSTOM_FACTORY_LABEL);
        String factoryClass = (String)properties.get(DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY);
        if (factoryClass != null) {
            customPanel.factoryClass.setText(factoryClass);
        }
        else {
            customPanel.factoryClass.setText(DataRowStore.EVENT_BRIDGE_FACTORY_DEFAULT);
        }
        
        if (JGROUPS_FACTORY_CLASS.equals(factory)) {
            view.getTypeSelector().setSelectedItem(JGROUPS_FACTORY_LABEL);
        }
        else if (JMS_FACTORY_CLASS.equals(factory)) {
            view.getTypeSelector().setSelectedItem(JMS_FACTORY_LABEL);
        }
        else {
            view.getTypeSelector().setSelectedItem(CUSTOM_FACTORY_LABEL);
        }
    }

    protected void prepareChildren(String factory) {
        existingCards = new HashMap();
        CacheSyncConfigView topView = (CacheSyncConfigView) getView();

        // note that none of the panels need a controller
        // if they issue controls, they will use this object taken from parent

        JGroupsConfigPanel jgroupsPanel = new JGroupsConfigPanel();
        existingCards.put(JGROUPS_FACTORY_LABEL, jgroupsPanel);
        topView.addCard(jgroupsPanel, JGROUPS_FACTORY_LABEL);
        
        jgroupsPanel.getUseDefaultConfig().addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                jgroupsDefaultConfig();
            }
        });
        jgroupsPanel.getUseConfigFile().addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                jgroupsURLConfig();
            }
        });

        JMSConfigPanel jmsPanel = new JMSConfigPanel();
        existingCards.put(JMS_FACTORY_LABEL, jmsPanel);
        topView.addCard(jmsPanel, JMS_FACTORY_LABEL);

        CustomRemoteEventsConfigPanel customFactoryPanel = new CustomRemoteEventsConfigPanel();
        existingCards.put(CUSTOM_FACTORY_LABEL, customFactoryPanel);
        topView.addCard(customFactoryPanel, CUSTOM_FACTORY_LABEL);

        if (factory == null) {
            factory = DataRowStore.EVENT_BRIDGE_FACTORY_DEFAULT;
        }

        if (JGROUPS_FACTORY_CLASS.equals(factory)) {
            ((CacheSyncConfigView) getView())
                    .showCard(JGROUPS_FACTORY_LABEL);
        }
        else if (JMS_FACTORY_CLASS.equals(factory)) {
            ((CacheSyncConfigView) getView())
                    .showCard(JMS_FACTORY_LABEL);
        }
        else {
            ((CacheSyncConfigView) getView())
                    .showCard(CUSTOM_FACTORY_LABEL);
        }
        
        loadProperties(factory);
    }
}
