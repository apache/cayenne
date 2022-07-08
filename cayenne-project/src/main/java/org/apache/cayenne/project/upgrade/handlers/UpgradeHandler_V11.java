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
import org.w3c.dom.NodeList;

/**
 * Upgrade handler for the project version "11" introduced by 4.3.M1 release.
 * Changes highlight:
 *      - ROP removal
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
        Element domain = upgradeUnit.getDocument().getDocumentElement();
        // introduce xml namespace and schema for domain
        domain.setAttribute("xmlns","http://cayenne.apache.org/schema/11/domain");
        domain.setAttribute("xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance");
        domain.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/11/domain " +
                "https://cayenne.apache.org/schema/11/domain.xsd");
        // update version
        domain.setAttribute("project-version", getVersion());
    }

    @Override
    public void processDataMapDom(UpgradeUnit upgradeUnit) {
        Element dataMap = upgradeUnit.getDocument().getDocumentElement();
        // update schema
        dataMap.setAttribute("xmlns","http://cayenne.apache.org/schema/11/modelMap");
        dataMap.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/11/modelMap " +
                "https://cayenne.apache.org/schema/11/modelMap.xsd");
        // update version
        dataMap.setAttribute("project-version", getVersion());

        dropRopProperties(upgradeUnit);
        cleanupObjEntityClientInfo(upgradeUnit);

        upgradeXmlExtensionsSchemas(upgradeUnit);
    }

    private void upgradeXmlExtensionsSchemas(UpgradeUnit upgradeUnit) {
        
    }

    private void dropRopProperties(UpgradeUnit upgradeUnit) {
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

    private void cleanupObjEntityClientInfo(UpgradeUnit upgradeUnit) {
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
}
