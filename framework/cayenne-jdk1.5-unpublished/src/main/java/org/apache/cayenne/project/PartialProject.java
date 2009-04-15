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

package org.apache.cayenne.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.conf.ConfigLoader;
import org.apache.cayenne.conf.ConfigLoaderDelegate;
import org.apache.cayenne.conf.ConfigSaverDelegate;
import org.apache.cayenne.conf.ConfigStatus;
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.conf.JNDIDataSourceFactory;

/**
 * PartialProject is a "lightweight" project implementation. It can work with projects
 * even when some of the resources are missing. It never instantiates Cayenne stack
 * objects, using other, lightweight, data structures instead.
 * 
 * @deprecated since 3.0. {@link ProjectConfigurator} approach turned out to be not
 *             usable, and is in fact rarely used (if ever). It will be removed in
 *             subsequent releases.
 */
public class PartialProject extends Project {

    protected String projectVersion;
    protected Map<String, DomainMetaData> domains;
    protected ConfigLoaderDelegate loadDelegate;
    protected Map<String, String> dataViewLocations;

    /**
     * Constructor PartialProjectHandler.
     * 
     * @param projectFile
     */
    public PartialProject(File projectFile) {
        super(projectFile);
    }

    /**
     * @since 1.1
     */
    @Override
    public void upgrade() throws ProjectException {
        // upgrades not supported in this type of project
        throw new ProjectException("'PartialProject' does not support upgrades.");
    }

    /**
     * Loads internal project and rewrites its nodes according to the list of
     * DataNodeConfigInfo objects. Only main project file gets updated, the rest are
     * assumed to be in place.
     */
    public void updateNodes(List<? extends DataNodeConfigInfo> list)
            throws ProjectException {
        for (final DataNodeConfigInfo nodeConfig : list) {
            String domainName = nodeConfig.getDomain();
            if (domainName == null && domains.size() != 1) {
                throw new IllegalArgumentException(
                        "Node must have domain set explicitly if there is no default domain.");
            }

            if (domainName == null) {
                domainName = ((DomainMetaData) domains.values().toArray()[0]).name;
            }

            NodeMetaData node = findNode(domainName, nodeConfig.getName(), false);
            if (node == null) {
                continue;
            }

            if (nodeConfig.getAdapter() != null) {
                node.adapter = nodeConfig.getAdapter();
            }

            if (nodeConfig.getDataSource() != null) {
                node.dataSource = nodeConfig.getDataSource();
                node.factory = JNDIDataSourceFactory.class.getName();
            }
            else if (nodeConfig.getDriverFile() != null) {
                node.dataSource = node.name + DataNodeFile.LOCATION_SUFFIX;
                node.factory = DriverDataSourceFactory.class.getName();
            }
        }
    }

    @Override
    protected void prepareSave(List filesToSave, List wrappedObjects)
            throws ProjectException {
        filesToSave.addAll(files);
    }

    @Override
    protected void postInitialize(File projectFile) {
        loadDelegate = new LoadDelegate();
        domains = new HashMap<String, DomainMetaData>();

        try {
            FileInputStream in = new FileInputStream(projectFile);
            try {
                new ConfigLoader(loadDelegate).loadDomains(in);
            }
            catch (Exception ex) {
                throw new ProjectException("Error creating PartialProject.", ex);
            }
            finally {
                in.close();
            }
        }
        catch (IOException ioex) {
            throw new ProjectException("Error creating PartialProject.", ioex);
        }

        super.postInitialize(projectFile);
    }

    @Override
    public List getChildren() {
        return new ArrayList<DomainMetaData>(domains.values());
    }

    @Override
    public void checkForUpgrades() {
        // do nothing...
    }

    /**
     * @see org.apache.cayenne.project.Project#buildFileList()
     */
    @Override
    public List buildFileList() {
        List<ProjectFile> list = new ArrayList<ProjectFile>();
        list.add(projectFileForObject(this));
        return list;
    }

    /**
     * @see org.apache.cayenne.project.Project#getLoadStatus()
     */
    @Override
    public ConfigStatus getLoadStatus() {
        return loadDelegate.getStatus();
    }

    @Override
    public ProjectFile projectFileForObject(Object obj) {
        if (obj != this) {
            return null;
        }

        ApplicationProjectFile projectFile = new ApplicationProjectFile(this);
        projectFile.setSaveDelegate(new SaveDelegate());
        return projectFile;
    }

    private DomainMetaData findDomain(String domainName) {
        DomainMetaData domain = domains.get(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Can't find domain: " + domainName);
        }

        return domain;
    }

    private NodeMetaData findNode(
            String domainName,
            String nodeName,
            boolean failIfNotFound) {
        DomainMetaData domain = findDomain(domainName);
        NodeMetaData node = domain.nodes.get(nodeName);
        if (node == null && failIfNotFound) {
            throw new IllegalArgumentException("Can't find node: "
                    + domainName
                    + "."
                    + nodeName);
        }

        return node;
    }

    protected class DomainMetaData {

        protected String name;
        protected Map<String, NodeMetaData> nodes = new HashMap<String, NodeMetaData>();
        protected Map<String, MapMetaData> maps = new HashMap<String, MapMetaData>();
        protected Map mapDependencies = new HashMap();
        protected Map properties = new HashMap();

        public DomainMetaData(String name) {
            this.name = name;
        }
    }

    protected class NodeMetaData {

        protected String name;
        protected String dataSource;
        protected String adapter;
        protected String factory;
        protected String schemaUpdateStrategy;
        protected List<String> maps = new ArrayList<String>();

        public NodeMetaData(String name) {
            this.name = name;
        }
    }

    protected class MapMetaData {

        protected String name;
        protected String location;

        public MapMetaData(String name) {
            this.name = name;
        }
    }

    class LoadDelegate implements ConfigLoaderDelegate {

        protected ConfigStatus status = new ConfigStatus();

        public void shouldLoadProjectVersion(String version) {
            PartialProject.this.projectVersion = version;
        }

        protected DomainMetaData findDomain(String name) {
            DomainMetaData domain = domains.get(name);
            if (domain == null) {
                throw new ProjectException("Can't find domain: " + name);
            }

            return domain;
        }

        protected MapMetaData findMap(String domainName, String mapName) {
            DomainMetaData domain = findDomain(domainName);
            MapMetaData map = domain.maps.get(mapName);
            if (map == null) {
                throw new ProjectException("Can't find map: " + mapName);
            }

            return map;
        }

        protected NodeMetaData findNode(String domainName, String nodeName) {
            DomainMetaData domain = findDomain(domainName);
            NodeMetaData node = domain.nodes.get(nodeName);
            if (node == null) {
                throw new ProjectException("Can't find node: " + nodeName);
            }

            return node;
        }

        public void startedLoading() {
            domains.clear();
        }

        public void finishedLoading() {
        }

        public ConfigStatus getStatus() {
            return status;
        }

        public boolean loadError(Throwable th) {
            status.getOtherFailures().add(th.getMessage());
            return false;
        }

        public void shouldLinkDataMap(String domainName, String nodeName, String mapName) {
            findNode(domainName, nodeName).maps.add(mapName);
        }

        public void shouldLoadDataDomain(String name) {
            domains.put(name, new DomainMetaData(name));
        }

        public void shouldLoadDataDomainProperties(String domainName, Map properties) {
            if (properties == null || properties.isEmpty()) {
                return;
            }

            DomainMetaData domain = findDomain(domainName);
            domain.properties.putAll(properties);
        }

        public void shouldLoadDataMaps(String domainName, Map locations) {
            if (locations.size() == 0) {
                return;
            }

            DomainMetaData domain = findDomain(domainName);

            // load DataMaps tree
            Iterator it = locations.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String name = (String) entry.getKey();
                MapMetaData map = new MapMetaData(name);
                map.location = (String) entry.getValue();
                domain.maps.put(name, map);
            }
        }

        public void shouldLoadDataNode(
                String domainName,
                String nodeName,
                String dataSource,
                String adapter,
                String factory,
                String schemaUpdateStrategy) {

            NodeMetaData node = new NodeMetaData(nodeName);
            node.adapter = adapter;
            node.factory = factory;
            node.dataSource = dataSource;
            node.schemaUpdateStrategy = schemaUpdateStrategy;
            findDomain(domainName).nodes.put(nodeName, node);
        }

        public void shouldRegisterDataView(String dataViewName, String dataViewLocation) {

            if (dataViewName == null) {
                throw new NullPointerException("Null dataViewName");
            }

            if (dataViewLocation == null) {
                throw new NullPointerException("Null dataViewLocation");
            }

            if (dataViewLocations == null) {
                dataViewLocations = new HashMap<String, String>();
            }
            dataViewLocations.put(dataViewName, dataViewLocation);
        }
    }

    class SaveDelegate implements ConfigSaverDelegate {

        /**
         * @since 1.1
         */
        public String projectVersion() {
            return projectVersion;
        }

        public Iterator domainNames() {
            return domains.keySet().iterator();
        }

        public Iterator viewNames() {
            if (dataViewLocations == null) {
                dataViewLocations = new HashMap<String, String>();
            }
            return dataViewLocations.keySet().iterator();
        }

        public String viewLocation(String dataViewName) {
            if (dataViewLocations == null) {
                dataViewLocations = new HashMap<String, String>();
            }
            return dataViewLocations.get(dataViewName);
        }

        public Iterator propertyNames(String domainName) {
            return findDomain(domainName).properties.keySet().iterator();
        }

        public String propertyValue(String domainName, String propertyName) {
            return (String) findDomain(domainName).properties.get(propertyName);
        }

        public Iterator linkedMapNames(String domainName, String nodeName) {
            return (findDomain(domainName).nodes.get(nodeName)).maps.iterator();
        }

        public String mapLocation(String domainName, String mapName) {
            return (findDomain(domainName).maps.get(mapName)).location;
        }

        public Iterator mapNames(String domainName) {
            return findDomain(domainName).maps.keySet().iterator();
        }

        public String nodeAdapterName(String domainName, String nodeName) {
            return (findDomain(domainName).nodes.get(nodeName)).adapter;
        }

        public String nodeDataSourceName(String domainName, String nodeName) {
            return (findDomain(domainName).nodes.get(nodeName)).dataSource;
        }

        public String nodeFactoryName(String domainName, String nodeName) {
            return (findDomain(domainName).nodes.get(nodeName)).factory;
        }

        public String nodeSchemaUpdateStrategyName(String domainName, String nodeName) {
            return (findDomain(domainName).nodes.get(nodeName)).schemaUpdateStrategy;
        }

        public Iterator nodeNames(String domainName) {
            return findDomain(domainName).nodes.keySet().iterator();
        }
    }
}
