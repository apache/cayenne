/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.Validate;
import org.objectstyle.cayenne.conf.ConfigLoader;
import org.objectstyle.cayenne.conf.ConfigLoaderDelegate;
import org.objectstyle.cayenne.conf.ConfigSaverDelegate;
import org.objectstyle.cayenne.conf.ConfigStatus;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.conf.JNDIDataSourceFactory;

/**
 * PartialProject is a "lightweight" project implementation. It can work with
 * projects even when some of the resources are missing. It never instantiates
 * Cayenne stack objects, using other, lightweight, data structures instead.
 * 
 * @author Andrei Adamchik
 */
public class PartialProject extends Project {
    protected String projectVersion;
    protected Map domains;
    protected ConfigLoaderDelegate loadDelegate;
    protected Map dataViewLocations;

    /**
     * Constructor PartialProjectHandler.
     * @param projectFile
     */
    public PartialProject(File projectFile) {
        super(projectFile);
    }

    /**
     * @since 1.1
     */
    public void upgrade() throws ProjectException {
        // upgrades not supported in this type of project
        throw new ProjectException("'PartialProject' does not support upgrades.");
    }

    /**
     * Loads internal project and rewrites its nodes according to the list of
     * DataNodeConfigInfo objects. Only main project file gets updated, the rest
     * are assumed to be in place.
     */
    public void updateNodes(List list) throws ProjectException {
        Iterator it = list.iterator();
        while (it.hasNext()) {
            DataNodeConfigInfo nodeConfig = (DataNodeConfigInfo) it.next();
            String domainName = nodeConfig.getDomain();
            if (domainName == null && domains.size() != 1) {
                throw new IllegalArgumentException("Node must have domain set explicitly if there is no default domain.");
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

    protected void prepareSave(List filesToSave, List wrappedObjects)
        throws ProjectException {
        filesToSave.addAll(files);
    }

    protected void postInitialize(File projectFile) {
        loadDelegate = new LoadDelegate();
        domains = new HashMap();

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

    public List getChildren() {
        return new ArrayList(domains.values());
    }

    public void checkForUpgrades() {
        // do nothing...
    }

    /**
     * @see org.objectstyle.cayenne.project.Project#buildFileList()
     */
    public List buildFileList() {
        List list = new ArrayList();
        list.add(projectFileForObject(this));
        return list;
    }

    /**
     * @see org.objectstyle.cayenne.project.Project#getLoadStatus()
     */
    public ConfigStatus getLoadStatus() {
        return loadDelegate.getStatus();
    }

    public ProjectFile projectFileForObject(Object obj) {
        if (obj != this) {
            return null;
        }

        ApplicationProjectFile projectFile = new ApplicationProjectFile(this);
        projectFile.setSaveDelegate(new SaveDelegate());
        return projectFile;
    }

    private DomainMetaData findDomain(String domainName) {
        DomainMetaData domain = (DomainMetaData) domains.get(domainName);
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
        NodeMetaData node = (NodeMetaData) domain.nodes.get(nodeName);
        if (node == null && failIfNotFound) {
            throw new IllegalArgumentException(
                "Can't find node: " + domainName + "." + nodeName);
        }

        return node;
    }

    protected class DomainMetaData {
        protected String name;
        protected Map nodes = new HashMap();
        protected Map maps = new HashMap();
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
        protected List maps = new ArrayList();

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
            DomainMetaData domain = (DomainMetaData) domains.get(name);
            if (domain == null) {
                throw new ProjectException("Can't find domain: " + name);
            }

            return domain;
        }

        protected MapMetaData findMap(String domainName, String mapName) {
            DomainMetaData domain = findDomain(domainName);
            MapMetaData map = (MapMetaData) domain.maps.get(mapName);
            if (map == null) {
                throw new ProjectException("Can't find map: " + mapName);
            }

            return map;
        }

        protected NodeMetaData findNode(String domainName, String nodeName) {
            DomainMetaData domain = findDomain(domainName);
            NodeMetaData node = (NodeMetaData) domain.nodes.get(nodeName);
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

        public void shouldLinkDataMap(
            String domainName,
            String nodeName,
            String mapName) {
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
            Iterator it = locations.keySet().iterator();
            while (it.hasNext()) {
                String name = (String) it.next();
                MapMetaData map = new MapMetaData(name);
                map.location = (String) locations.get(name);
                domain.maps.put(name, map);
            }
        }

        /**
         * @deprecated since 1.1
         */
        public void shouldLoadDataMaps(
            String domainName,
            Map locations,
            Map dependencies) {

            if (locations.size() == 0) {
                return;
            }

            DomainMetaData domain = findDomain(domainName);

            // load DataMaps tree
            Iterator it = locations.keySet().iterator();
            while (it.hasNext()) {
                String name = (String) it.next();
                MapMetaData map = new MapMetaData(name);
                map.location = (String) locations.get(name);
                domain.maps.put(name, map);
            }
        }

        /**
         * @deprecated Since 1.0.4 this method is no longer called during project loading.
         * shouldLoadDataMaps(String,Map,Map) is used instead.
         */
        public void shouldLoadDataMap(
            String domainName,
            String mapName,
            String location,
            List depMapNames) {

            MapMetaData map = new MapMetaData(mapName);
            map.location = location;
            findDomain(domainName).maps.put(mapName, map);
        }

        public void shouldLoadDataNode(
            String domainName,
            String nodeName,
            String dataSource,
            String adapter,
            String factory) {

            NodeMetaData node = new NodeMetaData(nodeName);
            node.adapter = adapter;
            node.factory = factory;
            node.dataSource = dataSource;
            findDomain(domainName).nodes.put(nodeName, node);
        }

        public void shouldRegisterDataView(
            String dataViewName,
            String dataViewLocation) {
            Validate.notNull(dataViewName);
            Validate.notNull(dataViewLocation);
            if (dataViewLocations == null) {
                dataViewLocations = new HashMap();
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

        /**
         * @deprecated since 1.1
         */
        public Iterator dependentMapNames(String domainName, String mapName) {
            return IteratorUtils.EMPTY_ITERATOR;
        }

        public Iterator domainNames() {
            return domains.keySet().iterator();
        }

        public Iterator viewNames() {
            if (dataViewLocations == null) {
                dataViewLocations = new HashMap();
            }
            return dataViewLocations.keySet().iterator();
        }

        public String viewLocation(String dataViewName) {
            if (dataViewLocations == null) {
                dataViewLocations = new HashMap();
            }
            return (String) dataViewLocations.get(dataViewName);
        }

        public Iterator propertyNames(String domainName) {
            return findDomain(domainName).properties.keySet().iterator();
        }

        public String propertyValue(String domainName, String propertyName) {
            return (String) findDomain(domainName).properties.get(propertyName);
        }

        public Iterator linkedMapNames(String domainName, String nodeName) {
            return ((NodeMetaData) findDomain(domainName).nodes.get(nodeName))
                .maps
                .iterator();
        }

        public String mapLocation(String domainName, String mapName) {
            return ((MapMetaData) findDomain(domainName).maps.get(mapName)).location;
        }

        public Iterator mapNames(String domainName) {
            return findDomain(domainName).maps.keySet().iterator();
        }

        public String nodeAdapterName(String domainName, String nodeName) {
            return ((NodeMetaData) findDomain(domainName).nodes.get(nodeName)).adapter;
        }

        public String nodeDataSourceName(String domainName, String nodeName) {
            return ((NodeMetaData) findDomain(domainName).nodes.get(nodeName)).dataSource;
        }

        public String nodeFactoryName(String domainName, String nodeName) {
            return ((NodeMetaData) findDomain(domainName).nodes.get(nodeName)).factory;
        }

        public Iterator nodeNames(String domainName) {
            return findDomain(domainName).nodes.keySet().iterator();
        }
    }
}
