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
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.project.upgrade.UpgradeUnit;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.List;

/**
 * Interface that upgrade handlers should implement.
 * Implementation also should be injected into DI stack in right order.
 *
 * @since 4.1
 */
public interface UpgradeHandler {

    /**
     * root tag for the cgen extension
     * @since 5.0-M1
     */
    String CGEN = "cgen";

    /**
     * root tag for the dbImport extension
     * @since 5.0-M1
     */
    String DB_IMPORT = "dbImport";

    /**
     * root tag for the graph extension
     * @since 5.0-M1
     */
    String GRAPH = "graph";

    /**
     * @return target version for this handler
     */
    String getVersion();

    /**
     * Process DOM for the project root file (e.g. cayenne-project.xml)
     */
    void processProjectDom(UpgradeUnit upgradeUnit);

    /**
     * Process DOM for the data map file (e.g. datamap.map.xml)
     */
    void processDataMapDom(UpgradeUnit upgradeUnit);

    /**
     * This method should be avoided as much as possible, as
     * using this method will make upgrade process not future proof and
     * will require refactoring if model should change.
     */
    default void processModel(DataChannelDescriptor dataChannelDescriptor) {
    }

    /**
     * Upgrade Domain schema and version info
     * @param upgradeUnit for the datamap
     */
    default void updateDomainSchemaAndVersion(UpgradeUnit upgradeUnit) {
        Element domain = upgradeUnit.getDocument().getDocumentElement();
        // update schema
        domain.setAttribute("xmlns","http://cayenne.apache.org/schema/"+getVersion()+"/domain");
        domain.setAttribute("xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance");
        domain.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/"+getVersion()+"/domain " +
                "https://cayenne.apache.org/schema/"+getVersion()+"/domain.xsd");
        // update version
        domain.setAttribute("project-version", getVersion());
    }

    /**
     * Upgrade DataMap schema and version info
     * @param upgradeUnit for the datamap
     */
    default void updateDataMapSchemaAndVersion(UpgradeUnit upgradeUnit) {
        Element dataMap = upgradeUnit.getDocument().getDocumentElement();
        // update schema
        dataMap.setAttribute("xmlns","http://cayenne.apache.org/schema/"+getVersion()+"/modelMap");
        dataMap.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/"+getVersion()+"/modelMap " +
                "https://cayenne.apache.org/schema/"+getVersion()+"/modelMap.xsd");
        // update version
        dataMap.setAttribute("project-version", getVersion());
    }

    /**
     * Update schema for the given extension
     * @param upgradeUnit a unit to work with
     * @param extension name of the extension (cgen, dbimport, graph )
     */
    default void updateExtensionSchema(UpgradeUnit upgradeUnit, String extension) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("/data-map/*[local-name()='"+extension+"']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return;
        }
        for (int j = 0; j < nodes.getLength(); j++) {
            Element element = (Element) nodes.item(j);
            element.setAttribute("xmlns", "http://cayenne.apache.org/schema/"+getVersion()+"/"+extension.toLowerCase());
        }
    }

    default void processAllDataMapDomes(List<UpgradeUnit> dataMapUnits) {
        //noop
    }
}
