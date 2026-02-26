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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.xml.ProjectVersion;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * Upgrade service sequence is following:
 * 1. This cycle should be done by Modeler and will result in a full project upgrade
 *
 *  - find all project and datamap resources
 *  - define set of upgrade handlers to process those resources
 *  - process DOM (project + N data maps)
 *  - save & load cycle to flush all DOM changes
 *  - process project model
 *  - save once again to cleanup and sort final XML
 *
 * 2. This cycle can be used by CayenneRuntime to optionally support old project versions
 *
 *  - find all project and datamap resources
 *  - define set of upgrade handlers to process those resources
 *  - process DOM (project + N data maps)
 *  - directly load model from DOM w/o saving
 *  - process project model
 *
 * @since 4.1
 */
public class DefaultUpgradeService implements UpgradeService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultUpgradeService.class);

    public static final ProjectVersion MIN_SUPPORTED_VERSION = ProjectVersion.V6;

    TreeMap<ProjectVersion, UpgradeHandler> handlers = new TreeMap<>();

    @Inject
    private ProjectSaver projectSaver;

    @Inject
    private DataChannelDescriptorLoader loader;

    public DefaultUpgradeService(@Inject List<UpgradeHandler> handlerList) {
        for (UpgradeHandler handler : handlerList) {
            handlers.put(handler.getVersion(), handler);
        }
    }

    @Override
    public UpgradeMetaData getUpgradeType(Resource resource) {
        UpgradeMetaData metaData = new UpgradeMetaData();

        ProjectVersion version = loadProjectVersion(resource);
        metaData.setProjectVersion(version.getAsString());
        metaData.setSupportedVersion(ProjectVersion.getCurrent().getAsString());

        if (version.compareTo(MIN_SUPPORTED_VERSION) < 0) {
            metaData.setIntermediateUpgradeVersion(MIN_SUPPORTED_VERSION.getAsString());
            metaData.setUpgradeType(UpgradeType.INTERMEDIATE_UPGRADE_NEEDED);
            return metaData;
        }

        int comparison = ProjectVersion.getCurrent().compareTo(version);
        if (comparison < 0) {
            metaData.setUpgradeType(UpgradeType.DOWNGRADE_NEEDED);
        } else if (comparison == 0) {
            metaData.setUpgradeType(UpgradeType.UPGRADE_NOT_NEEDED);
        } else {
            metaData.setUpgradeType(UpgradeType.UPGRADE_NEEDED);
        }
        return metaData;
    }

    protected List<UpgradeHandler> getHandlersForVersion(ProjectVersion version) {
        List<UpgradeHandler> handlerList = new ArrayList<>();
        for (Map.Entry<ProjectVersion, UpgradeHandler> entry : handlers.entrySet()) {
            if(entry.getKey().compareTo(version) > 0) {
                handlerList.add(entry.getValue());
            }
        }
        return handlerList;
    }

    @Override
    public Resource upgradeProject(Resource resource) {
        List<UpgradeHandler> handlerList = getHandlersForVersion(loadProjectVersion(resource));

        List<UpgradeUnit> upgradeUnits = upgradeDOM(resource, handlerList);
        saveDOM(upgradeUnits);

        resource = upgradeUnits.get(0).getResource();

        ConfigurationTree<DataChannelDescriptor> configurationTree = upgradeModel(resource, handlerList);
        saveModel(configurationTree);

        return resource;
    }

    protected List<UpgradeUnit> upgradeDOM(Resource resource, List<UpgradeHandler> handlerList) {
        List<UpgradeUnit> allUnits = new ArrayList<>();

        // Load DOM for all resources
        Document projectDocument = Util.readDocument(resource.getURL());
        UpgradeUnit projectUnit = new UpgradeUnit(resource, projectDocument);
        allUnits.add(projectUnit);

        List<Resource> dataMapResources = getAdditionalDatamapResources(projectUnit);
        List<UpgradeUnit> dataMapUnits = new ArrayList<>(dataMapResources.size());
        for (Resource dataMapResource : dataMapResources) {
            dataMapUnits.add(new UpgradeUnit(dataMapResource, Util.readDocument(dataMapResource.getURL())));
        }
        allUnits.addAll(dataMapUnits);

        // Update DOM
        for(UpgradeHandler handler : handlerList) {
            handler.processProjectDom(projectUnit);
            for(UpgradeUnit dataMapUnit : dataMapUnits) {
                handler.processDataMapDom(dataMapUnit);
            }
        }

        return allUnits;
    }

    protected void saveDOM(Collection<UpgradeUnit> upgradeUnits) {
        for(UpgradeUnit unit : upgradeUnits) {
            saveDocument(unit);
        }
    }

    protected ConfigurationTree<DataChannelDescriptor> upgradeModel(Resource resource, List<UpgradeHandler> handlerList) {
        // Load Model back from the update XML
        ConfigurationTree<DataChannelDescriptor> configurationTree = loadProject(resource);

        // Update model level if needed
        for(UpgradeHandler handler : handlerList) {
            handler.processModel(configurationTree.getRootNode());
        }

        return configurationTree;
    }

    protected ConfigurationTree<DataChannelDescriptor> loadProject(Resource resource) {
        // Load Model back from the update XML
        ConfigurationTree<DataChannelDescriptor> configurationTree = loader.load(resource);

        // link all datamaps, or else we will lose cross-datamaps relationships
        EntityResolver resolver = new EntityResolver();
        for(DataMap dataMap : configurationTree.getRootNode().getDataMaps()) {
            resolver.addDataMap(dataMap);
            dataMap.setNamespace(resolver);
        }
        return configurationTree;
    }

    protected void saveModel(ConfigurationTree<DataChannelDescriptor> configurationTree) {
        // Save project once again via project saver, this will normalize XML to minimize final diff
        Project project = new Project(configurationTree);
        projectSaver.save(project);
    }

    List<Resource> getAdditionalDatamapResources(UpgradeUnit upgradeUnit) {
        List<Resource> resources = new ArrayList<>();
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate("/domain/map/@name", upgradeUnit.getDocument(), XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node mapNode = nodes.item(i);
                // in version 3.0.0.1 and earlier map tag had attribute location,
                // but it was always equal to data map name + ".map.xml"
                Resource mapResource = upgradeUnit.getResource().getRelativeResource(mapNode.getNodeValue() + ".map.xml");
                resources.add(mapResource);
            }
        } catch (Exception ex) {
            logger.warn("Can't get additional dataMap resources: ", ex);
        }
        return resources;
    }

    protected void saveDocument(UpgradeUnit upgradeUnit) {
        try {
            Source input = new DOMSource(upgradeUnit.getDocument());
            Result output = new StreamResult(Util.toFile(upgradeUnit.getResource().getURL()));

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(input, output);
        } catch (Exception ex) {
            logger.warn("Can't save the document: ", ex);
        }
    }

    /**
     * A default method for quick extraction of the project version from an XML
     * file.
     */
    protected ProjectVersion loadProjectVersion(Resource resource) {

        RootTagHandler rootHandler = new RootTagHandler();
        URL url = resource.getURL();
        try (InputStream in = url.openStream()) {
            XMLReader parser = Util.createXmlReader();
            parser.setContentHandler(rootHandler);
            parser.setErrorHandler(rootHandler);
            parser.parse(new InputSource(in));
        } catch (SAXException e) {
            // expected... handler will terminate as soon as it finds a root tag.
        } catch (Exception e) {
            throw new ConfigurationException("Error reading configuration from %s", e, url);
        }

        return ProjectVersion.fromString(rootHandler.projectVersion);
    }

    class RootTagHandler extends DefaultHandler {

        private String projectVersion;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

            this.projectVersion = attributes.getValue("", "project-version");

            // bail right away - we are not interested in reading this to the end
            throw new SAXException("finished");
        }
    }
}
