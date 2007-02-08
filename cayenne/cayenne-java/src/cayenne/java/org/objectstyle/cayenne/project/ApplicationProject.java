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
package org.objectstyle.cayenne.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.ConfigurationException;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.ConfigStatus;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.conf.RuntimeLoadDelegate;
import org.objectstyle.cayenne.map.DataMap;

/**
 * Represents Cayenne application project.
 * 
 * @author Andrei Adamchik
 */
public class ApplicationProject extends Project {

    private static Logger logObj = Logger.getLogger(ApplicationProject.class);

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
    public void upgrade() throws ProjectException {
        ApplicationUpgradeHandler.sharedHandler().performUpgrade(this);
    }

    /**
     * Initializes internal <code>Configuration</code> object and then calls super.
     */
    protected void postInitialize(File projectFile) {
        logObj.debug("postInitialize: " + projectFile);

        loadProject();
        super.postInitialize(projectFile);
    }

    /**
     * @since 1.1
     * @deprecated since 1.2
     */
    protected void loadProject(File projectFile) throws Exception {
        loadProject();
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

    public void checkForUpgrades() {
        ApplicationUpgradeHandler.sharedHandler().checkForUpgrades(
                configuration,
                upgradeMessages);
    }

    /**
     * @see org.objectstyle.cayenne.project.Project#getChildren()
     */
    public List getChildren() {
        return new ArrayList(this.getConfiguration().getDomains());
    }

    /**
     * Returns appropriate ProjectFile or null if object does not require a file of its
     * own. In case of ApplicationProject, the nodes that require separate filed are: the
     * project itself, each DataMap, each driver DataNode.
     */
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

    public ConfigStatus getLoadStatus() {
        return (configuration != null)
                ? configuration.getLoadStatus()
                : new ConfigStatus();
    }

    final class ProjectLoader extends RuntimeLoadDelegate {

        public ProjectLoader(Configuration config) {
            super(config, config.getLoadStatus());
        }

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

        public void shouldLoadDataDomainProperties(String domainName, Map properties) {

            // remove factory property to avoid instatiation attempts for unknown/invalid
            // classes

            Map propertiesClone = new HashMap(properties);
            Object dataContextFactory = propertiesClone
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
