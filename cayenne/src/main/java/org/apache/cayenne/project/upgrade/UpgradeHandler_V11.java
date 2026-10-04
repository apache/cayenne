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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Upgrade handler for the project version "11" introduced by 5.0.M1 release.
 * Changes highlight:
 * - schemas version update
 * - ROP removal
 * - cgen schema changes
 *
 * @since 5.0
 */
public final class UpgradeHandler_V11 implements UpgradeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeHandler_V11.class);

    private static final List<String> defaultTemplatePaths = List.of(
            "templates/v4_1/singleclass.vm",
            "templates/v4_1/superclass.vm",
            "templates/v4_1/subclass.vm",
            "templates/v4_1/embeddable-singleclass.vm",
            "templates/v4_1/embeddable-superclass.vm",
            "templates/v4_1/embeddable-subclass.vm",
            "templates/v4_1/datamap-singleclass.vm",
            "templates/v4_1/datamap-superclass.vm",
            "templates/v4_1/datamap-subclass.vm");

    @Override
    public String getVersion() {
        return "11";
    }

    @Override
    public void upgradeProjectDOM(UpgradeContext upgradeUnit) {
        upgradeProjectSchemaAndVersion(upgradeUnit);
        updateDataNodeConnectionPool(upgradeUnit);
    }

    @Override
    public void upgradeDataMapDOM(UpgradeContext upgradeUnit) {
        upgradeDataMapSchemaAndVersion(upgradeUnit);
        upgradeExtensionSchema(upgradeUnit, "cgen");
        upgradeExtensionSchema(upgradeUnit, "dbImport");
        upgradeExtensionSchema(upgradeUnit, "graph");
        upgradeInfoSchema(upgradeUnit);

        dropROPProperties(upgradeUnit);
        dropObjEntityClientInfo(upgradeUnit);
        upgradeGenericObjEntity(upgradeUnit);
        updateCgenConfig(upgradeUnit);
        updateDbImportConfig(upgradeUnit);
    }

    private void upgradeProjectSchemaAndVersion(UpgradeContext upgradeUnit) {
        Element project = upgradeUnit.getDocument().getDocumentElement();
        project.setAttribute("xmlns", "http://cayenne.apache.org/schema/11/domain");
        project.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        project.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/11/domain "
                + "https://cayenne.apache.org/schema/11/domain.xsd");
        project.setAttribute("project-version", "11");
    }

    private void upgradeDataMapSchemaAndVersion(UpgradeContext upgradeUnit) {
        Element dataMap = upgradeUnit.getDocument().getDocumentElement();
        dataMap.setAttribute("xmlns", "http://cayenne.apache.org/schema/11/modelMap");
        dataMap.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/11/modelMap "
                + "https://cayenne.apache.org/schema/11/modelMap.xsd");
        dataMap.setAttribute("project-version", "11");
    }

    private void upgradeExtensionSchema(UpgradeContext upgradeUnit, String extension) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("/data-map/*[local-name()='" + extension + "']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception e) {
            return;
        }
        for (int j = 0; j < nodes.getLength(); j++) {
            Element element = (Element) nodes.item(j);
            element.setAttribute("xmlns", "http://cayenne.apache.org/schema/11/" + extension.toLowerCase());
        }
    }

    private void upgradeInfoSchema(UpgradeContext upgradeUnit) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("//*[local-name()='property']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception e) {
            return;
        }
        for (int j = 0; j < nodes.getLength(); j++) {
            Element element = (Element) nodes.item(j);
            if (element.hasAttribute("xmlns:info")) {
                element.setAttribute("xmlns:info", "http://cayenne.apache.org/schema/11/info");
            }
        }
    }

    private void dropROPProperties(UpgradeContext upgradeUnit) {
        Element dataMap = upgradeUnit.getDocument().getDocumentElement();
        NodeList propertyNodes;
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            propertyNodes = (NodeList) xpath.evaluate("/data-map/property", upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception ex) {
            return;
        }

        for (int j = 0; j < propertyNodes.getLength(); j++) {
            Element propertyElement = (Element) propertyNodes.item(j);
            String name = propertyElement.getAttribute("name");

            switch (name) {
                case "clientSupported":
                case "defaultClientPackage":
                case "defaultClientSuperclass":
                    dataMap.removeChild(propertyElement);
                    break;
            }
        }
    }

    private void dropObjEntityClientInfo(UpgradeContext upgradeUnit) {
        NodeList objEntityNodes;
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            objEntityNodes = (NodeList) xpath.evaluate("/data-map/obj-entity", upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception ex) {
            return;
        }

        for (int j = 0; j < objEntityNodes.getLength(); j++) {
            Element objEntityElement = (Element) objEntityNodes.item(j);
            objEntityElement.removeAttribute("serverOnly");
            objEntityElement.removeAttribute("clientClassName");
            objEntityElement.removeAttribute("clientSuperClassName");
        }
    }

    private void upgradeGenericObjEntity(UpgradeContext upgradeUnit) {
        NodeList objEntityNodes;
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            objEntityNodes = (NodeList) xpath.evaluate("/data-map/obj-entity", upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception ex) {
            return;
        }

        for (int j = 0; j < objEntityNodes.getLength(); j++) {
            Element objEntityElement = (Element) objEntityNodes.item(j);
            String className = objEntityElement.getAttribute("className");
            if("org.apache.cayenne.CayenneDataObject".equals(className)) {
                objEntityElement.setAttribute("className", "org.apache.cayenne.GenericPersistentObject");
            }
        }
    }

    private void updateDbImportConfig(UpgradeContext upgradeUnit) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("/data-map/*[local-name()='dbImport']/*[local-name()='usePrimitives']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception e) {
            return;
        }
        for (int j = 0; j < nodes.getLength(); j++) {
            Element element = (Element) nodes.item(j);
            element.getParentNode().removeChild(element);
        }
    }

    private void updateCgenConfig(UpgradeContext upgradeUnit) {
        renameQueryTemplates(upgradeUnit);
        dropCgenClientConfig(upgradeUnit);
        updateTemplates(upgradeUnit);
    }

    private void updateDataNodeConnectionPool(UpgradeContext upgradeUnit) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("/domain/node",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception e) {
            return;
        }

        for (int j = 0; j < nodes.getLength(); j++) {
            Element propertyElement = (Element) nodes.item(j);
            String factory = propertyElement.getAttribute("factory");
            if("org.apache.cayenne.configuration.server.XMLPoolingDataSourceFactory".equals(factory)) {
                propertyElement.setAttribute("factory", "org.apache.cayenne.configuration.runtime.XMLPoolingDataSourceFactory");
            }
        }
    }

    private void renameQueryTemplates(UpgradeContext upgradeUnit) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList queryTemplates;
        NodeList querySuperTemplates;
        try {
            queryTemplates = (NodeList) xpath.evaluate("/data-map/*[local-name()='cgen']/*[local-name()='queryTemplate']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
            querySuperTemplates = (NodeList) xpath.evaluate("/data-map/*[local-name()='cgen']/*[local-name()='querySuperTemplate']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception e) {
            return;
        }

        for (int j = 0; j < queryTemplates.getLength(); j++) {
            Node element = queryTemplates.item(j);
            upgradeUnit.getDocument().renameNode(element, null, "dataMapTemplate");
        }

        for (int j = 0; j < querySuperTemplates.getLength(); j++) {
            Node element = querySuperTemplates.item(j);
            upgradeUnit.getDocument().renameNode(element, null, "dataMapSuperTemplate");
        }
    }

    private void dropCgenClientConfig(UpgradeContext upgradeUnit) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("/data-map/*[local-name()='cgen']/*[local-name()='client']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception e) {
            return;
        }
        for (int j = 0; j < nodes.getLength(); j++) {
            Element element = (Element) nodes.item(j);
            element.getParentNode().removeChild(element);
        }
    }

    /**
     * upgrades templates in dataMap. Reads the file by path and write template if it was found.
     * If not, the warning message will be written.
     * In case of default template the path to default template will be written.
     *
     * @param upgradeUnit - unit to upgrade
     */
    private void updateTemplates(UpgradeContext upgradeUnit) {
        updateTemplate(upgradeUnit, "template");
        updateTemplate(upgradeUnit, "superTemplate");
        updateTemplate(upgradeUnit, "embeddableTemplate");
        updateTemplate(upgradeUnit, "embeddableSuperTemplate");
        updateTemplate(upgradeUnit, "dataMapTemplate");
        updateTemplate(upgradeUnit, "dataMapSuperTemplate");
    }

    private void updateTemplate(UpgradeContext upgradeUnit, String nodeName) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList templates;
        try {
            templates = (NodeList) xpath.evaluate("/data-map/*[local-name()='cgen']/*[local-name()='" + nodeName + "']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception e) {
            return;
        }
        for (int j = 0; j < templates.getLength(); j++) {
            Node node = templates.item(j).getFirstChild();
            if (node != null) {
                String dataMapPath = upgradeUnit.getResource().getURL().getPath();
                node.setNodeValue(readTemplateFile(node.getNodeValue(), dataMapPath));
            }
        }
    }

    private String readTemplateFile(String path, String dataMapPath) {
        if (!isTemplateDefault(path)) {
            try {
                String templateFromClassPath = readTemplateFromClassPath(path);
                if (templateFromClassPath != null) {
                    return templateFromClassPath;
                }
                return readTemplateFromFile(path, dataMapPath);
            } catch (NoSuchFileException e) {
                return "The template " + path + " was not found during the project upgrade. Use the template editor in Cayenne modeler to set the template";
            } catch (IOException e) {
                LOGGER.warn("Can't read the file: {}", path, e);
                return "Can't read the file: " + path + e.getMessage();
            }
        } else {
            return path;
        }
    }

    private String readTemplateFromClassPath(String path) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream != null) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String readTemplateFromFile(String path, String dataMapPath) throws IOException {
        String absolutPath = buildPath(path, dataMapPath);
        return new String(Files.readAllBytes(Paths.get(absolutPath)));
    }

    private String buildPath(String path, String dataMapPath) throws IOException {
        File dataMap = new File(dataMapPath);
        return new File(dataMap.getParent(), path).getCanonicalPath();
    }

    private boolean isTemplateDefault(String template) {
        return defaultTemplatePaths.contains(template);
    }

}
