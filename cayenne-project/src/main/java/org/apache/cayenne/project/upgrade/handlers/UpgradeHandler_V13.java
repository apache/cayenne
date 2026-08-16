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

import org.apache.cayenne.project.upgrade.UpgradeContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Upgrade handler for the project version "13" introduced by 5.0.M3 release.
 * Changes: DataNode removal from Cayenne projects. DataNodes are configured in code
 * via {@code CayenneRuntimeBuilder.addDataNode(..)}.
 *
 * @since 5.0
 */
public class UpgradeHandler_V13 implements UpgradeHandler {

    @Override
    public String getVersion() {
        return "13";
    }

    @Override
    public void processProjectDom(UpgradeContext upgradeUnit) {
        updateDomainSchemaAndVersion(upgradeUnit);
        removeDataNodes(upgradeUnit);
        removeDataNodeInspections(upgradeUnit);
        updateDomainExtensionSchema(upgradeUnit, VALIDATION);
    }

    @Override
    public void processDataMapDom(UpgradeContext upgradeUnit) {
        updateDataMapSchemaAndVersion(upgradeUnit);
        updateExtensionSchema(upgradeUnit, CGEN);
        updateExtensionSchema(upgradeUnit, DB_IMPORT);
        updateInfoSchema(upgradeUnit);
    }

    private void removeDataNodes(UpgradeContext upgradeUnit) {
        for (Element node : elements(upgradeUnit, "/domain/*[local-name()='node']")) {
            node.getParentNode().removeChild(node);
            upgradeUnit.addPostUpgradeMessage("DataNode '" + node.getAttribute("name")
                    + "' was removed from the project along with its connection parameters. Starting with"
                    + " project version 13, DataNodes are configured in code via"
                    + " CayenneRuntimeBuilder.addDataNode(..)");
        }
    }

    private void removeDataNodeInspections(UpgradeContext upgradeUnit) {
        String path = "/domain/*[local-name()='validation']/*[local-name()='exclude']";
        for (Element exclude : elements(upgradeUnit, path)) {
            String inspection = exclude.getTextContent().trim();
            if (inspection.startsWith("DATA_NODE_") || inspection.equals("DATA_MAP_NODE_LINKAGE")) {
                exclude.getParentNode().removeChild(exclude);
            }
        }
    }

    private List<Element> elements(UpgradeContext upgradeUnit, String path) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate(path, upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception e) {
            return List.of();
        }

        List<Element> elements = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            elements.add((Element) nodes.item(i));
        }
        return elements;
    }
}
