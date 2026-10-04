/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.project.xml;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.project.ConfigurationNameMapper;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectLoader;
import org.apache.cayenne.project.DataMapLoader;
import org.apache.cayenne.project.upgrade.UpgradeContext;
import org.apache.cayenne.project.upgrade.ProjectFileUpgrader;
import org.apache.cayenne.project.upgrade.UpgradeHandler;
import org.apache.cayenne.project.upgrade.UpgradeType;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.net.URL;

/**
 * @since 4.1
 */
public class XMLProjectLoader implements ProjectLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLProjectLoader.class);

    @Inject
    protected Provider<XMLReader> xmlReaderProvider;

    @Inject
    protected DataMapLoader dataMapLoader;

    @Inject
    protected ConfigurationNameMapper nameMapper;

    @Inject
    protected AdhocObjectFactory objectFactory;

    @Inject
    protected HandlerFactory handlerFactory;

    @Inject
    protected ProjectFileUpgrader upgrader;

    @Override
    public Project load(Resource configurationResource) throws ConfigurationException {

        if (configurationResource == null) {
            throw new NullPointerException("Null configurationResource");
        }

        URL configurationURL = configurationResource.getURL();

        LOGGER.info("Loading XML configuration resource from {}", configurationURL);

        try (InputStream in = configurationURL.openStream()) {
            InputSource input = new InputSource(in);
            input.setSystemId(configurationURL.toString());
            return parse(configurationResource, input);
        } catch (UnsupportedVersionException e) {
            return upgradeAndLoad(configurationResource, e.getVersion());
        } catch (Exception e) {
            throw new ConfigurationException("Error loading configuration from %s", e, configurationURL);
        }
    }

    protected Project parse(Resource configurationResource, InputSource input)
            throws Exception {

        Project project = new Project();
        project.setConfigurationSource(configurationResource);
        project.setName(nameMapper.projectNodeName(Project.class, configurationResource));

        XMLReader parser = xmlReaderProvider.get();
        LoaderContext loaderContext = new LoaderContext(parser, handlerFactory);
        loaderContext.addDataMapListener(dataMap -> project.getDataMaps().add(dataMap));

        ProjectHandler rootHandler = new ProjectHandler(this, project, loaderContext);
        parser.setContentHandler(rootHandler);
        parser.setErrorHandler(rootHandler);
        parser.parse(input);

        loaderContext.projectLoaded(project);

        return project;
    }

    /**
     * Loads a project created by an older version of Cayenne, upgrading its XML in memory. The project files are
     * not modified. The upgrade fails if it would drop a part of the project without a replacement.
     *
     * @since 5.0
     */
    protected Project upgradeAndLoad(Resource configurationResource, String version) {

        URL configurationURL = configurationResource.getURL();
        UpgradeType upgradeType = upgrader.checkUpgradeNeeded(version);

        if (upgradeType == UpgradeType.DOWNGRADE_NEEDED) {
            throw new ConfigurationException("""
                    Unable to load configuration from %s: project version %s is newer than the supported version %s. \
                    The project was created with a newer version of Cayenne""",
                    configurationURL, version, UpgradeHandler.CURRENT_VERSION);
        }

        if (upgradeType == UpgradeType.INTERMEDIATE_UPGRADE_NEEDED) {
            throw new ConfigurationException("""
                    Unable to load configuration from %s: project version %s is too old to be upgraded. \
                    Open the project in an older CayenneModeler to upgrade it to version %s first""",
                    configurationURL, version, UpgradeHandler.MIN_SUPPORTED_VERSION);
        }

        UpgradeContext context = upgrader.upgradeProjectDom(configurationResource, version);
        if (!context.getChangesAffectingRuntime().isEmpty()) {
            throw new ConfigurationException("""
                    Unable to upgrade configuration from %s (project version %s) in memory, as the upgrade requires \
                    manual changes. Open the project in CayenneModeler to upgrade it. %s""",
                    configurationURL, version, String.join(" ", context.getChangesAffectingRuntime()));
        }

        LOGGER.warn("""
                Configuration {} has project version {} and was upgraded to version {} in memory. \
                Open the project in CayenneModeler to upgrade its XML permanently""",
                configurationURL, version, UpgradeHandler.CURRENT_VERSION);

        Project project;
        try {
            project = parse(configurationResource,
                    DocumentInputSource.of(context.getDocument(), configurationURL.toString()));
        } catch (Exception e) {
            throw new ConfigurationException("Error loading configuration from %s", e, configurationURL);
        }

        upgrader.upgradeModel(version, project);
        return project;
    }
}
