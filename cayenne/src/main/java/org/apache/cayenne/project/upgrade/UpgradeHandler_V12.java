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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Upgrade handler for the project version "12" introduced by 5.0.M2 release.
 * Changes: graph extension removal from Cayenne projects.
 *
 * @since 5.0
 */
public final class UpgradeHandler_V12 implements UpgradeHandler {

    static final String GRAPH_SUFFIX = ".graph.xml";

    @Override
    public String getVersion() {
        return "12";
    }

    @Override
    public void upgradeProjectDOM(UpgradeContext upgradeUnit) {
        upgradeProjectSchemaAndVersion(upgradeUnit);
        removeGraphIncludes(upgradeUnit);
        upgradeDomainExtensionSchema(upgradeUnit, "validation");
    }

    @Override
    public void upgradeDataMapDOM(UpgradeContext upgradeUnit) {
        upgradeDataMapSchemaAndVersion(upgradeUnit);
        upgradeExtensionSchema(upgradeUnit, "cgen");
        upgradeExtensionSchema(upgradeUnit, "dbImport");
        upgradeInfoSchema(upgradeUnit);
    }

    private void upgradeProjectSchemaAndVersion(UpgradeContext upgradeUnit) {
        Element project = upgradeUnit.getDocument().getDocumentElement();
        project.setAttribute("xmlns", "http://cayenne.apache.org/schema/12/domain");
        project.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        project.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/12/domain "
                + "https://cayenne.apache.org/schema/12/domain.xsd");
        project.setAttribute("project-version", "12");
    }

    private void upgradeDataMapSchemaAndVersion(UpgradeContext upgradeUnit) {
        Element dataMap = upgradeUnit.getDocument().getDocumentElement();
        dataMap.setAttribute("xmlns", "http://cayenne.apache.org/schema/12/modelMap");
        dataMap.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/12/modelMap "
                + "https://cayenne.apache.org/schema/12/modelMap.xsd");
        dataMap.setAttribute("project-version", "12");
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
            element.setAttribute("xmlns", "http://cayenne.apache.org/schema/12/" + extension.toLowerCase());
        }
    }

    private void upgradeDomainExtensionSchema(UpgradeContext upgradeUnit, String extension) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("/domain/*[local-name()='" + extension + "']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception e) {
            return;
        }
        for (int j = 0; j < nodes.getLength(); j++) {
            Element element = (Element) nodes.item(j);
            element.setAttribute("xmlns", "http://cayenne.apache.org/schema/12/" + extension.toLowerCase());
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
                element.setAttribute("xmlns:info", "http://cayenne.apache.org/schema/12/info");
            }
        }
    }

    private void removeGraphIncludes(UpgradeContext upgradeUnit) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("/domain/*[local-name()='include']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception e) {
            return;
        }

        List<Element> toRemove = new ArrayList<>();
        for (int j = 0; j < nodes.getLength(); j++) {
            Element element = (Element) nodes.item(j);
            String href = element.getAttribute("href");
            if (href.endsWith(GRAPH_SUFFIX)) {
                toRemove.add(element);
                upgradeUnit.addObsoleteFile(href);
                upgradeUnit.recordChange(
                        "The 'graph' diagram layout is no longer supported and was removed from the project", false);
            }
        }
        for (Element element : toRemove) {
            element.getParentNode().removeChild(element);
        }
    }

}
