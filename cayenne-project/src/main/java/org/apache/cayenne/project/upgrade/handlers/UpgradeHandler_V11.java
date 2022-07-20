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

package org.apache.cayenne.project.upgrade.handlers;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.cayenne.project.upgrade.UpgradeUnit;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Upgrade handler for the project version "11" introduced by 4.3.M1 release.
 * Changes highlight:
 *      - schemas version update
 *      - ROP removal
 *      - cgen schema changes
 *
 * @since 4.3
 */
public class UpgradeHandler_V11 implements UpgradeHandler {

    @Override
    public String getVersion() {
        return "11";
    }

    @Override
    public void processProjectDom(UpgradeUnit upgradeUnit) {
        updateDomainSchemaAndVersion(upgradeUnit);
    }

    @Override
    public void processDataMapDom(UpgradeUnit upgradeUnit) {
        updateDataMapSchemaAndVersion(upgradeUnit);
        updateExtensionSchema(upgradeUnit, "cgen");
        updateExtensionSchema(upgradeUnit, "dbimport");
        updateExtensionSchema(upgradeUnit, "graph");
        upgradeComments(upgradeUnit);

        dropROPProperties(upgradeUnit);
        dropObjEntityClientInfo(upgradeUnit);
        updateCgenConfig(upgradeUnit);
    }

    private void upgradeComments(UpgradeUnit upgradeUnit) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList infoNodes;
        try {
            infoNodes = (NodeList) xpath.evaluate("//*[local-name()='property']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (int j = 0; j < infoNodes.getLength(); j++) {
            Element infoElement = (Element) infoNodes.item(j);
            if(infoElement.hasAttribute("xmlns:info")) {
                infoElement.setAttribute("xmlns:info", "http://cayenne.apache.org/schema/11/info");
            }
        }
    }

    private void dropROPProperties(UpgradeUnit upgradeUnit) {
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

    private void dropObjEntityClientInfo(UpgradeUnit upgradeUnit) {
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

    private void updateCgenConfig(UpgradeUnit upgradeUnit) {
        renameQueryTemplates(upgradeUnit);
        dropCgenClientConfig(upgradeUnit);
    }

    private void renameQueryTemplates(UpgradeUnit upgradeUnit) {
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

    private void dropCgenClientConfig(UpgradeUnit upgradeUnit) {
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
}
