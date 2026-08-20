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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.apache.cayenne.util.Util.isBlank;

/**
 *
 * Upgrade service sequence is following:
 * 1. This cycle should be done by Modeler and will result in a full project upgrade
 * <p>
 * - find all project and datamap resources
 * - define set of upgrade handlers to process those resources
 * - process DOM (project + N data maps)
 * - save & load cycle to flush all DOM changes
 * - process project model
 * - save once again to cleanup and sort final XML
 * <p>
 * 2. This cycle can be used by CayenneRuntime to optionally support old project versions
 * <p>
 * - find all project and datamap resources
 * - define set of upgrade handlers to process those resources
 * - process DOM (project + N data maps)
 * - directly load model from DOM w/o saving
 * - process project model
 *
 * @since 4.1
 */
public class DefaultProjectUpgrader implements ProjectUpgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProjectUpgrader.class);

    public static final String UNKNOWN_VERSION = "0";
    public static final String MIN_SUPPORTED_VERSION = "6";

    TreeMap<String, UpgradeHandler> handlers = new TreeMap<>(VersionComparator.INSTANCE);

    @Inject
    private ProjectSaver projectSaver;

    @Inject
    private DataChannelDescriptorLoader loader;

    public DefaultProjectUpgrader(@Inject List<UpgradeHandler> handlerList) {
        for (UpgradeHandler handler : handlerList) {
            handlers.put(handler.getVersion(), handler);
        }
    }

    @Override
    public PreUpgradeState checkUpgradeNeeded(Resource resource) {
        String version = loadProjectVersion(resource);
        String supportedVersion = String.valueOf(Project.VERSION);

        int c1 = VersionComparator.INSTANCE.compare(version, MIN_SUPPORTED_VERSION);
        if (c1 < 0) {
            return new PreUpgradeState(
                    UpgradeType.INTERMEDIATE_UPGRADE_NEEDED,
                    version,
                    supportedVersion,
                    MIN_SUPPORTED_VERSION);
        }

        int c2 = VersionComparator.INSTANCE.compare(supportedVersion, version);
        UpgradeType upgradeType;
        if (c2 < 0) {
            upgradeType = UpgradeType.DOWNGRADE_NEEDED;
        } else if (c2 == 0) {
            upgradeType = UpgradeType.UPGRADE_NOT_NEEDED;
        } else {
            upgradeType = UpgradeType.UPGRADE_NEEDED;
        }
        return new PreUpgradeState(upgradeType, version, supportedVersion, null);
    }

    protected List<UpgradeHandler> getHandlersForVersion(String version) {
        boolean found = MIN_SUPPORTED_VERSION.equals(version);
        List<UpgradeHandler> handlerList = new ArrayList<>();

        for (Map.Entry<String, UpgradeHandler> entry : handlers.entrySet()) {
            if (entry.getKey().equals(version)) {
                found = true;
                continue;
            }
            if (!found) {
                continue;
            }

            handlerList.add(entry.getValue());
        }

        return handlerList;
    }

    @Override
    public PostUpgradeState upgrade(Resource resource) {
        List<UpgradeHandler> handlerList = getHandlersForVersion(loadProjectVersion(resource));

        List<UpgradeContext> upgradeUnits = upgradeDOM(resource, handlerList);
        saveDOM(upgradeUnits);

        resource = upgradeUnits.getFirst().getResource();

        ConfigurationTree<DataChannelDescriptor> configurationTree = upgradeModel(resource, handlerList);
        saveModel(configurationTree);

        return new PostUpgradeState(resource, collectPostUpgradeMessages(upgradeUnits));
    }

    /**
     * Collects user-facing messages recorded by upgrade handlers while processing the units, deduplicating repeats
     * of the same message across units.
     */
    protected static List<String> collectPostUpgradeMessages(Collection<UpgradeContext> upgradeUnits) {
        Set<String> messages = new LinkedHashSet<>();
        for (UpgradeContext unit : upgradeUnits) {
            messages.addAll(unit.getPostUpgradeMessages());
        }
        return new ArrayList<>(messages);
    }

    protected List<UpgradeContext> upgradeDOM(Resource resource, List<UpgradeHandler> handlerList) {
        List<UpgradeContext> allUnits = new ArrayList<>();

        // Load DOM for all resources
        Document projectDocument = readDocument(resource.getURL());
        UpgradeContext projectUnit = new UpgradeContext(resource, projectDocument);
        allUnits.add(projectUnit);

        List<Resource> dataMapResources = getAdditionalDatamapResources(projectUnit);
        List<UpgradeContext> dataMapUnits = new ArrayList<>(dataMapResources.size());
        for (Resource dataMapResource : dataMapResources) {
            dataMapUnits.add(new UpgradeContext(dataMapResource, readDocument(dataMapResource.getURL())));
        }
        allUnits.addAll(dataMapUnits);

        // Update DOM
        for (UpgradeHandler handler : handlerList) {
            handler.processProjectDom(projectUnit);
            for (UpgradeContext dataMapUnit : dataMapUnits) {
                handler.processDataMapDom(dataMapUnit);
            }
        }

        return allUnits;
    }

    protected void saveDOM(Collection<UpgradeContext> upgradeUnits) {
        for (UpgradeContext unit : upgradeUnits) {
            saveDocument(unit);
        }
    }

    protected ConfigurationTree<DataChannelDescriptor> upgradeModel(Resource resource, List<UpgradeHandler> handlerList) {
        // Load Model back from the update XML
        ConfigurationTree<DataChannelDescriptor> configurationTree = loadProject(resource);

        // Update model level if needed
        for (UpgradeHandler handler : handlerList) {
            handler.processModel(configurationTree.getRootNode());
        }

        return configurationTree;
    }

    protected ConfigurationTree<DataChannelDescriptor> loadProject(Resource resource) {
        // Load Model back from the update XML
        ConfigurationTree<DataChannelDescriptor> configurationTree = loader.load(resource);

        // link all datamaps, or else we will lose cross-datamaps relationships
        EntityResolver resolver = new EntityResolver();
        for (DataMap dataMap : configurationTree.getRootNode().getDataMaps()) {
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

    List<Resource> getAdditionalDatamapResources(UpgradeContext upgradeUnit) {
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
            LOGGER.warn("Can't get additional dataMap resources: ", ex);
        }
        return resources;
    }

    protected void saveDocument(UpgradeContext upgradeUnit) {
        try {
            Source input = new DOMSource(upgradeUnit.getDocument());
            Result output = new StreamResult(Util.toFile(upgradeUnit.getResource().getURL()));

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(input, output);
        } catch (Exception ex) {
            LOGGER.warn("Can't save the document: ", ex);
        }
    }

    /**
     * A default method for quick extraction of the project version from an XML
     * file.
     */
    protected String loadProjectVersion(Resource resource) {

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

        return rootHandler.projectVersion != null ? rootHandler.projectVersion : UNKNOWN_VERSION;
    }

    protected static double decodeVersion(String version) {
        if (version == null || isBlank(version)) {
            return 0;
        }

        // leave the first dot, and treat remaining as a fraction
        // remove all non digit chars
        StringBuilder buffer = new StringBuilder(version.length());
        boolean dotProcessed = false;
        for (int i = 0; i < version.length(); i++) {
            char nextChar = version.charAt(i);
            if (nextChar == '.' && !dotProcessed) {
                dotProcessed = true;
                buffer.append('.');
            } else if (Character.isDigit(nextChar)) {
                buffer.append(nextChar);
            }
        }

        return Double.parseDouble(buffer.toString());
    }

    protected static Document readDocument(URL url) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(false);
        documentBuilderFactory.setXIncludeAware(false);
        documentBuilderFactory.setExpandEntityReferences(false);

        try {
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException ex) {
            throw new ConfigurationException("Unable to configure DocumentBuilderFactory", ex);
        }

        try {
            DocumentBuilder domBuilder = documentBuilderFactory.newDocumentBuilder();
            try (InputStream inputStream = url.openStream()) {
                return domBuilder.parse(inputStream);
            } catch (IOException | SAXException e) {
                throw new ConfigurationException("Error loading configuration from %s", e, url);
            }
        } catch (ParserConfigurationException e) {
            throw new ConfigurationException(e);
        }
    }

    private static class VersionComparator implements Comparator<String> {

        private static final VersionComparator INSTANCE = new VersionComparator();

        @Override
        public int compare(String o1, String o2) {
            if (o1.equals(o2)) {
                return 0;
            }
            double v1Double = decodeVersion(o1);
            double v2Double = decodeVersion(o2);
            return v1Double < v2Double ? -1 : 1;
        }
    }

    static class RootTagHandler extends DefaultHandler {

        private String projectVersion;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

            this.projectVersion = attributes.getValue("", "project-version");

            // bail right away - we are not interested in reading this to the end
            throw new SAXException("finished");
        }
    }
}
