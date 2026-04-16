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

import org.apache.cayenne.configuration.xml.ProjectVersion;
import org.apache.cayenne.project.upgrade.UpgradeUnit;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;

/**
 * @since 4.1
 */
public class UpgradeHandler_V9 implements UpgradeHandler {

    private static final Logger logger = LoggerFactory.getLogger(UpgradeHandler_V9.class);

    @Override
    public ProjectVersion getVersion() {
        return ProjectVersion.V9;
    }

    @Override
    public void processProjectDom(UpgradeUnit upgradeUnit) {
        Element domain = upgradeUnit.getDocument().getDocumentElement();
        domain.setAttribute("project-version", getVersion().getAsString());
    }

    @Override
    public void processDataMapDom(UpgradeUnit upgradeUnit) {
        Document document = upgradeUnit.getDocument();
        Element dataMap = document.getDocumentElement();
        updateDataMapSchemaAndVersion(upgradeUnit);

        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            Node reNode = (Node) xpath.evaluate("/data-map/reverse-engineering-config", document, XPathConstants.NODE);

            if (reNode != null) {
                String reFileName = ((Element) reNode).getAttribute("name") + ".xml";
                String directoryPath = Util.toFile(upgradeUnit.getResource().getURL()).getParent();

                File file = new File(directoryPath + "/" + reFileName);
                if (file.exists()) {
                    if(!file.delete()) {
                        logger.warn("Can't delete file " + file);
                    }
                }
                dataMap.removeChild(reNode);
            }
        } catch (Exception ex) {
            logger.warn("Can't process dataMap DOM: ", ex);
        }
    }
}
