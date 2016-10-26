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
package org.apache.cayenne.project.upgrade.v9;

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
import org.apache.cayenne.project.upgrade.v8.ProjectUpgrader_V8;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class UpgradeHandler_V9 extends BaseUpgradeHandler {

    static final String PREVIOUS_VERSION = "8";
    static final String TO_VERSION = "9";

    @Inject
    protected Injector injector;

    @Inject
    private ProjectSaver projectSaver;

    public UpgradeHandler_V9(Resource source) {
        super(source);
    }

    @Override
    protected Resource doPerformUpgrade(UpgradeMetaData metaData) throws ConfigurationException {
        if (compareVersions(metaData.getProjectVersion(), PREVIOUS_VERSION) == -1) {
            ProjectUpgrader_V8 upgraderV8 = new ProjectUpgrader_V8();
            injector.injectMembers(upgraderV8);
            UpgradeHandler handlerV8 = upgraderV8.getUpgradeHandler(projectSource);
            projectSource = handlerV8.performUpgrade();
        }

        deleteReverseEngineeringFiles(projectSource);

        XMLDataChannelDescriptorLoader loader = new XMLDataChannelDescriptorLoader();
        injector.injectMembers(loader);
        ConfigurationTree<DataChannelDescriptor> tree = loader.load(projectSource);
        Project project = new Project(tree);

        // load and safe cycle updates project version
        projectSaver.save(project);
        return project.getConfigurationResource();
    }

    private void deleteReverseEngineeringFiles(Resource projectSource) {
        Document projectDoc = readDOMDocument(projectSource);

        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate("/domain/map/@name", projectDoc, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); i++) {
                Node mapNode = nodes.item(i);

                Resource mapResource = projectSource.getRelativeResource(mapNode.getNodeValue() + ".map.xml");

                Document datamapDoc = readDOMDocument(mapResource);

                Node reNode = (Node) xpath.evaluate("/data-map/reverse-engineering-config",
                        datamapDoc, XPathConstants.NODE);

                if (reNode != null) {
                    String reFileName = ((Element) reNode).getAttribute("name") + ".xml";

                    try {
                        String directoryPath = Util.toFile(projectSource.getURL()).getParent();

                        File file = new File(directoryPath + "/" + reFileName);
                        if (file.exists()) {
                            file.delete();
                        }
                    } catch (Exception e) {
                        // ignore...
                    }
                }
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
    protected String getToVersion() {
        return TO_VERSION;
    }

}