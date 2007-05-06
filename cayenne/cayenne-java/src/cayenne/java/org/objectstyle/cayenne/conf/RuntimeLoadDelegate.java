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
package org.objectstyle.cayenne.conf;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.ConfigurationException;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.dba.AutoAdapter;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.MapLoader;
import org.xml.sax.InputSource;

/**
 * Implementation of ConfigLoaderDelegate that creates Cayenne access objects stack.
 * 
 * @author Andrei Adamchik
 */
public class RuntimeLoadDelegate implements ConfigLoaderDelegate {

    private static Logger logObj = Logger.getLogger(RuntimeLoadDelegate.class);

    protected Map domains = new HashMap();
    protected Map views = new HashMap();
    protected ConfigStatus status;
    protected Configuration config;
    protected long startTime;

    /**
     * @deprecated since 1.2
     */
    public RuntimeLoadDelegate(Configuration config, ConfigStatus status, Level logLevel) {
        this(config, status);
    }

    public RuntimeLoadDelegate(Configuration config, ConfigStatus status) {

        this.config = config;

        if (status == null) {
            status = new ConfigStatus();
        }

        this.status = status;
    }

    protected DataDomain findDomain(String name) throws FindException {
        DataDomain domain = (DataDomain) domains.get(name);
        if (domain == null) {
            throw new FindException("Can't find DataDomain: " + name);
        }

        return domain;
    }

    protected DataMap findMap(String domainName, String mapName) throws FindException {
        DataDomain domain = findDomain(domainName);
        DataMap map = domain.getMap(mapName);
        if (map == null) {
            throw new FindException("Can't find DataMap: " + mapName);
        }

        return map;
    }

    protected DataNode findNode(String domainName, String nodeName) throws FindException {
        DataDomain domain = findDomain(domainName);
        DataNode node = domain.getNode(nodeName);
        if (node == null) {
            throw new FindException("Can't find DataNode: " + nodeName);
        }

        return node;
    }

    public boolean loadError(Throwable th) {
        logObj.info("Parser Exception.", th);
        status.getOtherFailures().add(th.getMessage());
        return false;
    }

    /**
     * @since 1.1
     */
    public void shouldLoadProjectVersion(String version) {
        config.setProjectVersion(version);
    }

    /**
     * @since 1.1
     */
    public void shouldRegisterDataView(String name, String location) {
        views.put(name, location);
    }

    public void shouldLoadDataDomainProperties(String domainName, Map properties) {
        if (properties == null || properties.isEmpty()) {
            return;
        }

        DataDomain domain = null;
        try {
            domain = findDomain(domainName);
        }
        catch (FindException ex) {
            logObj.info("Error: Domain is not loaded: " + domainName);
            throw new ConfigurationException("Domain is not loaded: " + domainName);
        }

        domain.initWithProperties(properties);
    }

    public void shouldLoadDataDomain(String domainName) {
        if (domainName == null) {
            logObj.info("Error: unnamed <domain>.");
            throw new ConfigurationException("Domain 'name' attribute must be not null.");
        }

        logObj.info("loaded domain: " + domainName);
        domains.put(domainName, new DataDomain(domainName));
    }

    public void shouldLoadDataMaps(String domainName, Map locations) {
        if (locations.size() == 0) {
            return;
        }

        DataDomain domain = null;
        try {
            domain = findDomain(domainName);
        }
        catch (FindException ex) {
            logObj.info("Error: Domain is not loaded: " + domainName);
            throw new ConfigurationException("Domain is not loaded: " + domainName);
        }

        // load DataMaps tree
        Iterator it = locations.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            DataMap map = domain.getMap(name);
            if (map != null) {
                continue;
            }

            loadDataMap(domain, name, locations);
        }
    }

    /**
     * Returns DataMap for the name and location information. If a DataMap is already
     * loaded within a given domain, such loaded map is returned, otherwise the map is
     * loaded and linked with the DataDomain.
     */
    protected DataMap loadDataMap(DataDomain domain, String mapName, Map locations) {

        if (mapName == null) {
            throw new ConfigurationException("Error: <map> without 'name'.");
        }

        String location = (String) locations.get(mapName);

        if (location == null) {
            throw new ConfigurationException("Error: map '"
                    + mapName
                    + "' without 'location'.");
        }

        // load DataMap
        InputStream mapIn = config.getMapConfiguration(location);
        if (mapIn == null) {
            logObj.info("Warning: map location not found.");
            getStatus().addFailedMap(mapName, location, "map location not found");
            return null;
        }

        try {
            DataMap map = new MapLoader().loadDataMap(new InputSource(mapIn));

            logObj.info("loaded <map name='"
                    + mapName
                    + "' location='"
                    + location
                    + "'>.");

            map.setName(mapName);
            map.setLocation(location);

            domain.addMap(map);
            return map;
        }
        catch (Exception dmex) {
            logObj.info("Warning: map loading failed.", dmex);
            getStatus().addFailedMap(
                    mapName,
                    location,
                    "map loading failed - " + dmex.getMessage());
            return null;
        }
    }
    
    /**
     * Creates a new DataNode. Subclasses may override this method to provide a custom node class.
     * 
     * @since 1.
     */
    protected DataNode createDataNode(String nodeName) {
        return new DataNode(nodeName);
    }

    public void shouldLoadDataNode(
            String domainName,
            String nodeName,
            String dataSource,
            String adapter,
            String factory) {

        logObj.info("loading <node name='"
                + nodeName
                + "' datasource='"
                + dataSource
                + "' factory='"
                + factory
                + "'>.");

        if (nodeName == null) {
            throw new ConfigurationException("Error: <node> without 'name'.");
        }

        if (dataSource == null) {
            logObj.info("Warning: <node> '" + nodeName + "' has no 'datasource'.");
        }

        if (factory == null) {
            if (config.getDataSourceFactory() != null) {
                logObj.info("Warning: <node> '" + nodeName + "' without 'factory'.");
            }
            else {
                throw new ConfigurationException("Error: <node> '"
                        + nodeName
                        + "' without 'factory'.");
            }
        }

        DataNode node = createDataNode(nodeName);

        node.setDataSourceFactory(factory);
        node.setDataSourceLocation(dataSource);

        // load DataSource
        try {
            // use DomainHelper factory if it exists, if not - use factory specified
            // in configuration data
            DataSourceFactory confFactory = config.getDataSourceFactory();
            DataSourceFactory localFactory = (confFactory != null)
                    ? confFactory
                    : (DataSourceFactory) Class.forName(factory).newInstance();

            logObj.info("using factory: " + localFactory.getClass().getName());

            localFactory.initializeWithParentConfiguration(config);
            DataSource ds = localFactory.getDataSource(dataSource);
            if (ds != null) {
                logObj.info("loaded datasource.");
                node.setDataSource(ds);
            }
            else {
                logObj.info("Warning: null datasource.");
                getStatus().getFailedDataSources().put(nodeName, dataSource);
            }
        }
        catch (Exception ex) {
            logObj.info("Error: DataSource load failed", ex);
            getStatus().addFailedDataSource(
                    nodeName,
                    dataSource,
                    "DataSource load failed - " + ex.getMessage());
        }

        initAdapter(node, adapter);

        try {
            findDomain(domainName).addNode(node);
        }
        catch (FindException ex) {
            logObj.info("Error: can't load node, unknown domain: " + domainName);
            getStatus().addFailedDataSource(
                    nodeName,
                    nodeName,
                    "can't load node, unknown domain: " + domainName);
        }
    }

    /**
     * Intializes DataNode adapter.
     * 
     * @since 1.2
     */
    protected void initAdapter(DataNode node, String adapterName) {

        if (adapterName != null) {
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Class dbAdapterClass = Class.forName(adapterName, true, cl);
                node.setAdapter((DbAdapter) dbAdapterClass.newInstance());
                return;
            }
            catch (Exception ex) {
                logObj.info("instantiating adapter failed", ex);
                getStatus().addFailedAdapter(
                        node.getName(),
                        adapterName,
                        "instantiating adapter failed - " + ex.getMessage());
            }
        }

        logObj.info("no adapter set, using automatic adapter.");
        node.setAdapter(new AutoAdapter(new NodeDataSource(node)));
    }

    public void shouldLinkDataMap(String domainName, String nodeName, String mapName) {

        if (mapName == null) {
            logObj.info("<map-ref> has no 'name'.");
            throw new ConfigurationException("<map-ref> has no 'name'.");
        }

        logObj.info("loaded map-ref: " + mapName + ".");
        DataMap map = null;
        DataNode node = null;

        try {
            map = findMap(domainName, mapName);
        }
        catch (FindException ex) {
            logObj.info("Error: unknown map: " + mapName);
            getStatus().addFailedMapRefs(mapName, "unknown map: " + mapName);
            return;
        }

        try {
            node = findNode(domainName, nodeName);
        }
        catch (FindException ex) {
            logObj.info("Error: unknown node: " + nodeName);
            getStatus().addFailedMapRefs(mapName, "unknown node: " + nodeName);
            return;
        }

        node.addDataMap(map);
    }

    /**
     * Returns the domains.
     * 
     * @return List
     */
    public Map getDomains() {
        return domains;
    }

    /**
     * Returns the status.
     * 
     * @return ConfigStatus
     */
    public ConfigStatus getStatus() {
        return status;
    }

    /**
     * Returns the config.
     * 
     * @return Configuration
     */
    public Configuration getConfig() {
        return config;
    }

    /**
     * Sets the config.
     * 
     * @param config The config to set
     */
    public void setConfig(Configuration config) {
        this.config = config;
    }

    /**
     * Returns the logLevel.
     * 
     * @deprecated since 1.2
     */
    public Level getLogLevel() {
        return Level.INFO;
    }

    /**
     * Sets the logLevel.
     * 
     * @deprecated since 1.2
     */
    public void setLogLevel(Level logLevel) {
        // noop
    }

    /**
     * @see org.objectstyle.cayenne.conf.ConfigLoaderDelegate#finishedLoading()
     */
    public void finishedLoading() {
        // check for failures
        if (status.hasFailures()) {
            if (!config.isIgnoringLoadFailures()) {
                StringBuffer msg = new StringBuffer(128);
                msg.append("Load failures. Main configuration class: ");
                msg.append(config.getClass().getName());
                msg.append(", details: ");
                msg.append(status.describeFailures());
                throw new ConfigurationException(msg.toString());
            }
        }

        // update configuration object
        Iterator it = getDomains().values().iterator();
        while (it.hasNext()) {
            config.addDomain((DataDomain) it.next());
        }

        config.setDataViewLocations(views);

        logObj.info("finished configuration loading in "
                + (System.currentTimeMillis() - startTime)
                + " ms.");
    }

    /**
     * @see org.objectstyle.cayenne.conf.ConfigLoaderDelegate#startedLoading()
     */
    public void startedLoading() {
        startTime = System.currentTimeMillis();
        logObj.info("started configuration loading.");
    }

    /**
     * Thrown when loaded data does not contain certain expected objects.
     */
    class FindException extends Exception {

        /**
         * Constructor for FindException.
         * 
         * @param msg
         */
        public FindException(String msg) {
            super(msg);
        }
    }
}
