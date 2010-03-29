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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.ConfigStatus;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.conf.RuntimeLoadDelegate;
import org.apache.cayenne.map.DataMap;

/**
 * Represents Cayenne application project.
 */
public class ApplicationProject extends Project {

    protected Configuration configuration;

    /**
     * Constructor for ApplicationProject.
     * 
     * @param projectFile
     */
    public ApplicationProject(File projectFile) {
        this(projectFile, null);
    }

    /**
     * @since 1.2
     */
    public ApplicationProject(File projectFile, Configuration configuration) {

        if (configuration == null) {

            // normalize project file...
            if (projectFile != null) {

                if (projectFile.isDirectory()) {
                    projectFile = new File(projectFile.getPath()
                            + File.separator
                            + Configuration.DEFAULT_DOMAIN_FILE);
                }

                try {
                    projectFile = projectFile.getCanonicalFile();
                }
                catch (IOException e) {
                    throw new ProjectException("Bad project file: " + projectFile);
                }
            }

            configuration = new ProjectConfiguration(projectFile);
            configuration.setLoaderDelegate(new ProjectLoader(configuration));
        }

        this.configuration = configuration;

        initialize(projectFile);
        postInitialize(projectFile);
    }

    /**
     * @since 1.1
     */
    @Override
    public void upgrade() throws ProjectException {
        ApplicationUpgradeHandler.sharedHandler().performUpgrade(this);
    }

    /**
     * Initializes internal <code>Configuration</code> object and then calls super.
     */
    @Override
    protected void postInitialize(File projectFile) {
        loadProject();
        super.postInitialize(projectFile);
    }

    /**
     * @since 1.2
     */
    protected void loadProject() {

        // try to initialize configuration
        if (configuration.canInitialize()) {

            try {
                configuration.initialize();
            }
            catch (Exception e) {
                throw new ProjectException(
                        "Error initializaing project configuration.",
                        e);
            }
            configuration.didInitialize();
        }

        // set default version
        if (configuration.getProjectVersion() == null) {
            configuration.setProjectVersion(ApplicationUpgradeHandler
                    .sharedHandler()
                    .supportedVersion());
        }
    }

    /**
     * Returns Cayenne configuration object associated with this project.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Sets Cayenne configuration object associated with this project.
     */
    public void setConfiguration(ProjectConfiguration config) {
        this.configuration = config;
    }

    @Override
    public void checkForUpgrades() {
        this.upgradeStatus = ApplicationUpgradeHandler.sharedHandler().checkForUpgrades(
                configuration,
                upgradeMessages);
    }

    /**
     * @see org.apache.cayenne.project.Project#getChildren()
     */
    @Override
    public List getChildren() {
        return new ArrayList<DataDomain>(this.getConfiguration().getDomains());
    }

    /**
     * Returns appropriate ProjectFile or null if object does not require a file of its
     * own. In case of ApplicationProject, the nodes that require separate filed are: the
     * project itself, each DataMap, each driver DataNode.
     */
    @Override
    public ProjectFile projectFileForObject(Object obj) {
        if (requiresProjectFile(obj)) {
            String domainFileName = this.getConfiguration().getDomainConfigurationName();
            ApplicationProjectFile file = new ApplicationProjectFile(this, domainFileName);

            // inject save delegate...
            file.setSaveDelegate(configuration.getSaverDelegate());
            return file;
        }
        else if (requiresMapFile(obj)) {
            return new DataMapFile(this, (DataMap) obj);
        }
        else if (requiresNodeFile(obj)) {
            return new DataNodeFile(this, (DataNode) obj);
        }

        return null;
    }

    protected boolean requiresProjectFile(Object obj) {
        return obj == this;
    }

    protected boolean requiresMapFile(Object obj) {
        return obj instanceof DataMap;
    }

    protected boolean requiresNodeFile(Object obj) {
        if (obj instanceof DataNode) {
            DataNode node = (DataNode) obj;

            // only driver datasource factory requires a file
            if (DriverDataSourceFactory.class.getName().equals(
                    node.getDataSourceFactory())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ConfigStatus getLoadStatus() {
        return (configuration != null)
                ? configuration.getLoadStatus()
                : new ConfigStatus();
    }

    final class ProjectLoader extends RuntimeLoadDelegate {

        public ProjectLoader(Configuration config) {
            super(config, config.getLoadStatus());
        }

        protected void updateDefaults(DataDomain domain) {
            // do nothing...
        }

        @Override
        public void shouldLoadDataDomain(String domainName) {
            super.shouldLoadDataDomain(domainName);

            try {
                // disable class indexing
                findDomain(domainName).getEntityResolver().setIndexedByClass(false);
            }
            catch (Exception ex) {
                throw new ConfigurationException("Domain is not loaded: " + domainName);
            }
        }

        @Override
        public void shouldLoadDataDomainProperties(
                String domainName,
                Map<String, String> properties) {

            // remove factory property to avoid instantiation attempts for unknown/invalid
            // classes

            Map<String, String> propertiesClone = new HashMap<String, String>(properties);
            String dataContextFactory = propertiesClone
                    .remove(DataDomain.DATA_CONTEXT_FACTORY_PROPERTY);

            super.shouldLoadDataDomainProperties(domainName, propertiesClone);

            // stick property back in...
            if (dataContextFactory != null) {
                try {
                    findDomain(domainName).getProperties().put(
                            DataDomain.DATA_CONTEXT_FACTORY_PROPERTY,
                            dataContextFactory);
                }
                catch (Exception ex) {
                    throw new ConfigurationException("Domain is not loaded: "
                            + domainName);
                }
            }
        }
    }
}
