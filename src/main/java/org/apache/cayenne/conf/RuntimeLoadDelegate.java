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

package org.apache.cayenne.conf;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

/**
 * Implementation of ConfigLoaderDelegate that creates Cayenne access objects stack.
 * 
 * @author Andrus Adamchik
 */
public class RuntimeLoadDelegate implements ConfigLoaderDelegate {

    private static final Log logger = LogFactory.getLog(RuntimeLoadDelegate.class);

    // TODO: andrus, 7/17/2006 - these variables, and project upgrade logic should be
    // refactored out of the MapLoader. In fact we should either modify raw XML during the
    // upgrade, or implement some consistent upgrade API across variou loaders
    final static String _1_2_PACKAGE_PREFIX = "org.objectstyle.cayenne.";
    final static String _2_0_PACKAGE_PREFIX = "org.apache.cayenne.";

    protected Map domains = new HashMap();
    protected Map views = new HashMap();
    protected ConfigStatus status;
    protected Configuration config;
    protected long startTime;
    protected MapLoader mapLoader;

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
        logger.info("Parser Exception.", th);
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
            logger.info("Error: Domain is not loaded: " + domainName);
            throw new ConfigurationException("Domain is not loaded: " + domainName);
        }

        domain.initWithProperties(properties);
    }

    public void shouldLoadDataDomain(String domainName) {
        if (domainName == null) {
            logger.info("Error: unnamed <domain>.");
            throw new ConfigurationException("Domain 'name' attribute must be not null.");
        }

        logger.info("loaded domain: " + domainName);
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
            logger.info("Error: Domain is not loaded: " + domainName);
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

    protected MapLoader getMapLoader() {
        // it is worth caching the map loader, as it precompiles some XML operations
        // starting from release 3.0
        if (mapLoader == null) {
            mapLoader = new MapLoader();
        }

        return mapLoader;
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
            logger.info("Warning: map location not found.");
            getStatus().addFailedMap(mapName, location, "map location not found");
            return null;
        }

        try {
            DataMap map = getMapLoader().loadDataMap(new InputSource(mapIn));

            logger.info("loaded <map name='"
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
            logger.info("Warning: map loading failed.", dmex);
            getStatus().addFailedMap(
                    mapName,
                    location,
                    "map loading failed - " + dmex.getMessage());
            return null;
        }
    }

    /**
     * Creates a new DataNode. Subclasses may override this method to provide a custom
     * node class.
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

        logger.info("loading <node name='"
                + nodeName
                + "' datasource='"
                + dataSource
                + "' factory='"
                + factory
                + "'>.");

        if (nodeName == null) {
            throw new ConfigurationException("Error: <node> without 'name'.");
        }

        factory = convertClassNameFromV1_2(factory);
        adapter = convertClassNameFromV1_2(adapter);

        if (dataSource == null) {
            logger.info("Warning: <node> '" + nodeName + "' has no 'datasource'.");
        }

        if (factory == null) {
            if (config.getDataSourceFactory() != null) {
                logger.info("Warning: <node> '" + nodeName + "' without 'factory'.");
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

            logger.info("using factory: " + localFactory.getClass().getName());

            localFactory.initializeWithParentConfiguration(config);
            DataSource ds = localFactory.getDataSource(dataSource);
            if (ds != null) {
                logger.info("loaded datasource.");
                node.setDataSource(ds);
            }
            else {
                logger.info("Warning: null datasource.");
                getStatus().getFailedDataSources().put(nodeName, dataSource);
            }
        }
        catch (Exception ex) {
            logger.info("Error: DataSource load failed", ex);
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
            logger.info("Error: can't load node, unknown domain: " + domainName);
            getStatus().addFailedDataSource(
                    nodeName,
                    nodeName,
                    "can't load node, unknown domain: " + domainName);
        }
    }

    /**
     * @since 2.0
     */
    String convertClassNameFromV1_2(String name) {
        if (name == null) {
            return null;
        }

        // upgrade from v. <= 1.2
        if (name.startsWith(_1_2_PACKAGE_PREFIX)) {
            return _2_0_PACKAGE_PREFIX + name.substring(_1_2_PACKAGE_PREFIX.length());
        }

        return name;
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
                logger.info("instantiating adapter failed", ex);
                getStatus().addFailedAdapter(
                        node.getName(),
                        adapterName,
                        "instantiating adapter failed - " + ex.getMessage());
            }
        }

        logger.info("no adapter set, using automatic adapter.");
        node.setAdapter(new AutoAdapter(new NodeDataSource(node)));
    }

    public void shouldLinkDataMap(String domainName, String nodeName, String mapName) {

        if (mapName == null) {
            logger.info("<map-ref> has no 'name'.");
            throw new ConfigurationException("<map-ref> has no 'name'.");
        }

        logger.info("loaded map-ref: " + mapName + ".");
        DataMap map = null;
        DataNode node = null;

        try {
            map = findMap(domainName, mapName);
        }
        catch (FindException ex) {
            logger.info("Error: unknown map: " + mapName);
            getStatus().addFailedMapRefs(mapName, "unknown map: " + mapName);
            return;
        }

        try {
            node = findNode(domainName, nodeName);
        }
        catch (FindException ex) {
            logger.info("Error: unknown node: " + nodeName);
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
     * @see org.apache.cayenne.conf.ConfigLoaderDelegate#finishedLoading()
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

        // load missing relationships and update configuration object
        Iterator it = getDomains().values().iterator();
        while (it.hasNext()) {
            DataDomain domain = (DataDomain) it.next();
            updateDefaults(domain);
            config.addDomain(domain);
        }

        config.setDataViewLocations(views);

        logger.info("finished configuration loading in "
                + (System.currentTimeMillis() - startTime)
                + " ms.");
    }

    /**
     * Updates missing mapping artefacts that can be guessed from other mapping
     * information. This implementation creates missing reverse relationships, marking
     * newly created relationships as "runtime".
     * 
     * @since 3.0
     */
    protected void updateDefaults(DataDomain domain) {

        // connect DB layer
        Iterator maps = domain.getDataMaps().iterator();
        while (maps.hasNext()) {
            DataMap map = (DataMap) maps.next();

            Iterator entities = map.getDbEntities().iterator();
            while (entities.hasNext()) {
                DbEntity entity = (DbEntity) entities.next();

                // iterate by copy to avoid concurrency modification errors on reflexive
                // relationships
                Object[] relationships = entity.getRelationships().toArray();
                for (int i = 0; i < relationships.length; i++) {
                    DbRelationship relationship = (DbRelationship) relationships[i];
                    if (relationship.getReverseRelationship() == null) {
                        DbRelationship reverse = relationship.createReverseRelationship();

                        Entity targetEntity = reverse.getSourceEntity();
                        reverse.setName(makeUniqueRelationshipName(targetEntity));
                        reverse.setRuntime(true);
                        targetEntity.addRelationship(reverse);

                        logger.info("added runtime complimentary DbRelationship from "
                                + targetEntity.getName()
                                + " to "
                                + reverse.getTargetEntityName());
                    }
                }
            }
        }

        // connect object layer
        maps = domain.getDataMaps().iterator();
        while (maps.hasNext()) {
            DataMap map = (DataMap) maps.next();

            Iterator entities = map.getObjEntities().iterator();
            while (entities.hasNext()) {
                ObjEntity entity = (ObjEntity) entities.next();

                // iterate by copy to avoid concurrency modification errors on reflexive
                // relationships
                Object[] relationships = entity.getRelationships().toArray();
                for (int i = 0; i < relationships.length; i++) {
                    ObjRelationship relationship = (ObjRelationship) relationships[i];
                    if (relationship.getReverseRelationship() == null) {
                        ObjRelationship reverse = relationship
                                .createReverseRelationship();

                        Entity targetEntity = reverse.getSourceEntity();
                        reverse.setName(makeUniqueRelationshipName(targetEntity));
                        reverse.setRuntime(true);
                        targetEntity.addRelationship(reverse);
                        
                        logger.info("added runtime complimentary ObjRelationship from "
                                + targetEntity.getName()
                                + " to "
                                + reverse.getTargetEntityName());
                    }
                }
            }
        }
    }

    private String makeUniqueRelationshipName(Entity entity) {
        for (int i = 0; i < 1000; i++) {
            String name = "runtimeRelationship" + i;
            if(entity.getRelationship(name) == null) {
                return name;
            }
        }

        throw new CayenneRuntimeException(
                "Could not come up with a unique relationship name");
    }

    /**
     * @see org.apache.cayenne.conf.ConfigLoaderDelegate#startedLoading()
     */
    public void startedLoading() {
        startTime = System.currentTimeMillis();
        logger.info("started configuration loading.");
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
