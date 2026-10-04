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
import org.apache.cayenne.project.upgrade.ProjectFileUpgrader;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
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
    protected HandlerFactory handlerFactory;

    @Inject
    protected ProjectFileUpgrader upgrader;

    @Override
    public Project load(Resource source) throws ConfigurationException {

        if (source == null) {
            throw new NullPointerException("Null configurationResource");
        }

        URL configurationURL = source.getURL();

        LOGGER.info("Loading XML configuration resource from {}", configurationURL);

        try (InputStream in = configurationURL.openStream()) {
            InputSource input = new InputSource(in);
            input.setSystemId(configurationURL.toString());
            return parse(source, input);
        } catch (UnsupportedVersionException e) {
            return upgradeAndLoad(source, e.getVersion());
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

        ProjectHandler rootHandler = new ProjectHandler(project, loaderContext, nameMapper, dataMapLoader);
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
        Document upgraded = upgrader.upgradeProjectDOM(configurationResource, version).getRuntimeDocument();
        URL configurationURL = configurationResource.getURL();
        try {
            return parse(configurationResource, DocumentInputSource.of(upgraded, configurationURL.toString()));
        } catch (Exception e) {
            throw new ConfigurationException("Error loading configuration from %s", e, configurationURL);
        }
    }
}
