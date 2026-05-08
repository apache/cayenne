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

import org.apache.cayenne.project.upgrade.UpgradeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Upgrade handler for the project version "12" introduced by 5.0.M2 release.
 * Changes: graph extension removal from Cayenne projects.
 *
 * @since 5.0
 */
public class UpgradeHandler_V12 implements UpgradeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeHandler_V12.class);

    static final String GRAPH_SUFFIX = ".graph.xml";

    @Override
    public String getVersion() {
        return "12";
    }

    @Override
    public void processProjectDom(UpgradeUnit upgradeUnit) {
        updateDomainSchemaAndVersion(upgradeUnit);
        removeGraphIncludes(upgradeUnit);
        updateDomainExtensionSchema(upgradeUnit, VALIDATION);
    }

    @Override
    public void processDataMapDom(UpgradeUnit upgradeUnit) {
        updateDataMapSchemaAndVersion(upgradeUnit);
        updateExtensionSchema(upgradeUnit, CGEN);
        updateExtensionSchema(upgradeUnit, DB_IMPORT);
    }

    private void removeGraphIncludes(UpgradeUnit upgradeUnit) {
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
            if (href != null && href.endsWith(GRAPH_SUFFIX)) {
                deleteGraphFile(upgradeUnit, href);
                toRemove.add(element);
            }
        }
        for (Element element : toRemove) {
            element.getParentNode().removeChild(element);
        }
    }

    private void deleteGraphFile(UpgradeUnit upgradeUnit, String href) {
        try {
            File projectFile = new File(upgradeUnit.getResource().getURL().toURI());
            File graphFile = new File(projectFile.getParentFile(), href);
            if (!Files.deleteIfExists(graphFile.toPath())) {
                LOGGER.warn("Graph file not found, skipping deletion: {}", graphFile);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to delete graph file '{}': {}", href, e.getMessage());
        }
    }
}
