
/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.dialog.datadomain;

import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.access.DataRowStore;
import org.objectstyle.cayenne.event.JavaGroupsBridgeFactory;
import org.scopemvc.core.Selector;

/**
 * @author Andrei Adamchik
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
