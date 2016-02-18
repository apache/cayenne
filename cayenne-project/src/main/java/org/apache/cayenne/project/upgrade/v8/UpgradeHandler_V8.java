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
package org.apache.cayenne.project.upgrade.v8;


import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.XMLDataChannelDescriptorLoader;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.upgrade.BaseUpgradeHandler;
import org.apache.cayenne.project.upgrade.UpgradeHandler;
import org.apache.cayenne.project.upgrade.UpgradeMetaData;
import org.apache.cayenne.project.upgrade.UpgradeType;
import org.apache.cayenne.project.upgrade.v7.ProjectUpgrader_V7;
import org.apache.cayenne.query.QueryDescriptor;
import org.apache.cayenne.resource.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class UpgradeHandler_V8 extends BaseUpgradeHandler {

    static final String TO_VERSION = "8";
    static final String MIN_SUPPORTED_VERSION = "7";

    @Inject
    protected Injector injector;

    @Inject
    private ProjectSaver projectSaver;

    public UpgradeHandler_V8(Resource source) {
        super(source);
    }

    @Override
    protected Resource doPerformUpgrade(UpgradeMetaData metaData) throws ConfigurationException {
        if (compareVersions(metaData.getProjectVersion(), MIN_SUPPORTED_VERSION) == 0) {
            ProjectUpgrader_V7 upgraderV7 = new ProjectUpgrader_V7();
            injector.injectMembers(upgraderV7);
            UpgradeHandler handlerV7 = upgraderV7.getUpgradeHandler(projectSource);
            projectSource = handlerV7.performUpgrade();
        }

        upgradeFactories(projectSource);

        XMLDataChannelDescriptorLoader loader = new XMLDataChannelDescriptorLoader();
        injector.injectMembers(loader);
        ConfigurationTree<DataChannelDescriptor> tree = loader.load(projectSource);
        Project project = new Project(tree);

        // load and safe cycle updates project version
        projectSaver.save(project);
        return project.getConfigurationResource();
    }

    private void upgradeFactories(Resource projectSource) {
        Document projectDoc = readDOMDocument(projectSource);

        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate("/domain/map/@name", projectDoc, XPathConstants.NODESET);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            for (int i = 0; i < nodes.getLength(); i++) {
                Node mapNode = nodes.item(i);

                Resource mapResource = projectSource.getRelativeResource(mapNode.getNodeValue() + ".map.xml");

                Document datamapDoc = readDOMDocument(mapResource);

                NodeList queryNodes = (NodeList) xpath.evaluate("/data-map/query", datamapDoc, XPathConstants.NODESET);

                for (int j = 0; j < queryNodes.getLength(); j++) {
                    Element queryElement = (Element) queryNodes.item(j);
                    String factory = queryElement.getAttribute("factory");

                    String queryType;

                    switch (factory) {
                        case "org.apache.cayenne.map.SelectQueryBuilder":
                            queryType = QueryDescriptor.SELECT_QUERY;
                            break;
                        case "org.apache.cayenne.map.SQLTemplateBuilder":
                            queryType = QueryDescriptor.SQL_TEMPLATE;
                            break;
                        case "org.apache.cayenne.map.EjbqlBuilder":
                            queryType = QueryDescriptor.EJBQL_QUERY;
                            break;
                        case "org.apache.cayenne.map.ProcedureQueryBuilder":
                            queryType = QueryDescriptor.PROCEDURE_QUERY;
                            break;
                        default:
                            throw new ConfigurationException("Unknown query factory: " + factory);
                    }

                    queryElement.setAttribute("type", queryType);
                    queryElement.removeAttribute("factory");
                }

                DOMSource domSource = new DOMSource(datamapDoc);
                StreamResult result = new StreamResult(new FileOutputStream(new File(mapResource.getURL().toURI())));
                transformer.transform(domSource, result);
            }

        } catch (Exception e) {
            throw new ConfigurationException("Unable to parse Cayenne XML configuration files.", e);
        }
    }

    private Document readDOMDocument(Resource resource) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder domBuilder = documentBuilderFactory.newDocumentBuilder();

            try (InputStream inputStream = resource.getURL().openStream()) {
                return domBuilder.parse(inputStream);
            } catch (IOException | SAXException e) {
                throw new ConfigurationException("Error loading configuration from %s", e, resource);
            }
        } catch (ParserConfigurationException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    protected UpgradeMetaData loadMetaData() {
        String version = loadProjectVersion();

        UpgradeMetaData metadata = new UpgradeMetaData();
        metadata.setSupportedVersion(TO_VERSION);
        metadata.setProjectVersion(version);

        int c1 = compareVersions(version, MIN_SUPPORTED_VERSION);
        int c2 = compareVersions(TO_VERSION, version);

        if (c1 < 0) {
            metadata.setIntermediateUpgradeVersion(MIN_SUPPORTED_VERSION);
            metadata.setUpgradeType(UpgradeType.INTERMEDIATE_UPGRADE_NEEDED);
        } else if (c2 < 0) {
            metadata.setUpgradeType(UpgradeType.DOWNGRADE_NEEDED);
        } else if (c2 == 0) {
            metadata.setUpgradeType(UpgradeType.UPGRADE_NOT_NEEDED);
        } else {
            metadata.setUpgradeType(UpgradeType.UPGRADE_NEEDED);
        }

        return metadata;
    }
}
