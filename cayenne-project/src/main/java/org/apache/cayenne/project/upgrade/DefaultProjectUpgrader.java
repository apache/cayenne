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

package org.apache.cayenne.project.upgrade;

import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.upgrade.UpgradeContext;
import org.apache.cayenne.configuration.upgrade.ConfigurationUpgrader;
import org.apache.cayenne.configuration.upgrade.UpgradeHandler;
import org.apache.cayenne.configuration.upgrade.UpgradeType;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Upgrades project files permanently, rewriting the XML on disk. The actual upgrade is done by the
 * {@link ConfigurationUpgrader} shared with the runtime.
 *
 * @since 4.1
 */
public class DefaultProjectUpgrader implements ProjectUpgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProjectUpgrader.class);

    @Inject
    private ProjectSaver projectSaver;

    @Inject
    private DataChannelDescriptorLoader loader;

    @Inject
    private ConfigurationUpgrader upgrader;

    @Override
    public PreUpgradeState checkUpgradeNeeded(Resource resource) {
        String version = upgrader.readVersion(resource);
        UpgradeType upgradeType = upgrader.checkUpgradeNeeded(version);
        String intermediateVersion = upgradeType == UpgradeType.INTERMEDIATE_UPGRADE_NEEDED
                ? UpgradeHandler.MIN_SUPPORTED_VERSION
                : null;
        return new PreUpgradeState(upgradeType, version, UpgradeHandler.CURRENT_VERSION, intermediateVersion);
    }

    @Override
    public PostUpgradeState upgrade(Resource resource) {
        String version = upgrader.readVersion(resource);

        // upgrade and save DOM of the project and all its DataMaps
        UpgradeContext projectContext = upgrader.upgradeProjectDom(resource, version);
        List<UpgradeContext> contexts = new ArrayList<>();
        contexts.add(projectContext);
        for (Resource dataMapResource : dataMapResources(projectContext)) {
            contexts.add(upgrader.upgradeDataMapDom(dataMapResource, version));
        }
        for (UpgradeContext context : contexts) {
            saveDocument(context);
            deleteObsoleteFiles(context);
        }

        // load the model back from the upgraded XML, upgrade it and save once again via the project saver,
        // which normalizes the XML to minimize the final diff
        ConfigurationTree<DataChannelDescriptor> configurationTree = loadProject(resource);
        upgrader.upgradeModel(version, configurationTree.getRootNode());
        projectSaver.save(new Project(configurationTree));

        return new PostUpgradeState(resource, collectPostUpgradeMessages(contexts));
    }

    protected static List<String> collectPostUpgradeMessages(List<UpgradeContext> contexts) {
        Set<String> messages = new LinkedHashSet<>();
        for (UpgradeContext context : contexts) {
            messages.addAll(context.getChangeNotifications());
        }
        return new ArrayList<>(messages);
    }

    protected ConfigurationTree<DataChannelDescriptor> loadProject(Resource resource) {
        ConfigurationTree<DataChannelDescriptor> configurationTree = loader.load(resource);

        // link all datamaps, or else we will lose cross-datamaps relationships
        EntityResolver resolver = new EntityResolver();
        for (DataMap dataMap : configurationTree.getRootNode().getDataMaps()) {
            resolver.addDataMap(dataMap);
            dataMap.setNamespace(resolver);
        }

        return configurationTree;
    }

    /**
     * Returns the resources of the DataMaps referenced by the project XML, resolved relative to the project resource.
     */
    protected List<Resource> dataMapResources(UpgradeContext projectContext) {
        List<Resource> resources = new ArrayList<>();
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate("/*/map/@name", projectContext.getDocument(),
                    XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node mapNode = nodes.item(i);
                resources.add(projectContext.getResource().getRelativeResource(mapNode.getNodeValue() + ".map.xml"));
            }
        } catch (Exception ex) {
            LOGGER.warn("Can't get additional dataMap resources: ", ex);
        }
        return resources;
    }

    protected void deleteObsoleteFiles(UpgradeContext context) {
        File directory = Util.toFile(context.getResource().getURL()).getParentFile();
        for (String relativePath : context.getObsoleteFiles()) {
            File file = new File(directory, relativePath);
            try {
                if (!Files.deleteIfExists(file.toPath())) {
                    LOGGER.warn("Obsolete file not found, skipping deletion: {}", file);
                }
            } catch (IOException e) {
                LOGGER.warn("Can't delete obsolete file {}", file, e);
            }
        }
    }

    protected void saveDocument(UpgradeContext context) {
        try {
            Source input = new DOMSource(context.getDocument());
            Result output = new StreamResult(Util.toFile(context.getResource().getURL()));
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(input, output);
        } catch (Exception ex) {
            LOGGER.warn("Can't save the document: ", ex);
        }
    }
}
