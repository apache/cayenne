
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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.access.DataRowStore;
import org.apache.cayenne.event.JavaGroupsBridgeFactory;
import org.scopemvc.core.Selector;

/**
 */
public class JGroupsConfigModel extends CacheSyncConfigModel {
    private static final String[] storedProperties =
        new String[] {
            DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY,
            JavaGroupsBridgeFactory.MCAST_ADDRESS_PROPERTY,
            JavaGroupsBridgeFactory.MCAST_PORT_PROPERTY,
            JavaGroupsBridgeFactory.JGROUPS_CONFIG_URL_PROPERTY };

    private static Map selectors;
    private static Map defaults;

    public static final Selector USING_CONFIG_FILE_SELECTOR =
        Selector.fromString("usingConfigFile");
    public static final Selector USING_DEFAULT_CONFIG_SELECTOR =
        Selector.fromString("usingDefaultConfig");

    public static final Selector MCAST_ADDRESS_SELECTOR =
        Selector.fromString("mcastAddress");
    public static final Selector MCAST_PORT_SELECTOR = Selector.fromString("mcastPort");
    public static final Selector JGROUPS_CONFIG_URL_SELECTOR =
        Selector.fromString("jgroupsConfigURL");

    static {
        selectors = new HashMap(5);
        selectors.put(
            JavaGroupsBridgeFactory.JGROUPS_CONFIG_URL_PROPERTY,
            JGROUPS_CONFIG_URL_SELECTOR);
        selectors.put(
            JavaGroupsBridgeFactory.MCAST_ADDRESS_PROPERTY,
            MCAST_ADDRESS_SELECTOR);
        selectors.put(JavaGroupsBridgeFactory.MCAST_PORT_PROPERTY, MCAST_PORT_SELECTOR);

        defaults = new HashMap(4);
        defaults.put(
            JavaGroupsBridgeFactory.MCAST_ADDRESS_PROPERTY,
            JavaGroupsBridgeFactory.MCAST_ADDRESS_DEFAULT);
        defaults.put(
            JavaGroupsBridgeFactory.MCAST_PORT_PROPERTY,
            JavaGroupsBridgeFactory.MCAST_PORT_DEFAULT);
    }

    protected boolean usingConfigFile;

    public void setMap(Map map) {
        super.setMap(map);
        usingConfigFile = (map != null && getJgroupsConfigURL() != null);
    }

    public Selector selectorForKey(String key) {
        return (Selector) selectors.get(key);
    }

    public String defaultForKey(String key) {
        return (String) defaults.get(key);
    }

    public boolean isUsingConfigFile() {
        return usingConfigFile;
    }

    public String[] supportedProperties() {
        return storedProperties;
    }

    public void setUsingConfigFile(boolean b) {
        this.usingConfigFile = b;

        if (b) {
            setMcastAddress(null);
            setMcastPort(null);
        }
        else {
            setJgroupsConfigURL(null);
        }
    }

    public boolean isUsingDefaultConfig() {
        return !isUsingConfigFile();
    }

    public void setUsingDefaultConfig(boolean flag) {
        setUsingConfigFile(!flag);
    }

    public String getJgroupsConfigURL() {
        return getProperty(JavaGroupsBridgeFactory.JGROUPS_CONFIG_URL_PROPERTY);
    }

    public void setJgroupsConfigURL(String jgroupsConfigURL) {
        setProperty(
            JavaGroupsBridgeFactory.JGROUPS_CONFIG_URL_PROPERTY,
            jgroupsConfigURL);
    }

    public String getMcastAddress() {
        return getProperty(JavaGroupsBridgeFactory.MCAST_ADDRESS_PROPERTY);
    }

    public void setMcastAddress(String multicastAddress) {
        setProperty(JavaGroupsBridgeFactory.MCAST_ADDRESS_PROPERTY, multicastAddress);
    }

    public String getMcastPort() {
        return getProperty(JavaGroupsBridgeFactory.MCAST_PORT_PROPERTY);
    }

    public void setMcastPort(String multicastPort) {
        setProperty(JavaGroupsBridgeFactory.MCAST_PORT_PROPERTY, multicastPort);
    }
}
