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

package org.apache.cayenne.configuration.upgrade;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.List;

/**
 * A step of the project upgrade that brings the XML and the model from the previous project version to the version
 * it reports in {@link #getVersion()}. The set of handlers is closed: every handler is a permitted implementation of
 * this interface.
 *
 * @since 4.1
 */
public sealed interface UpgradeHandler permits
        UpgradeHandler_V7,
        UpgradeHandler_V8,
        UpgradeHandler_V9,
        UpgradeHandler_V10,
        UpgradeHandler_V11,
        UpgradeHandler_V12,
        UpgradeHandler_V13,
        UpgradeHandler_V14 {

    /**
     * Creates an instance of every permitted handler, ordered from the oldest to the current version. Must be kept in
     * sync with the "permits" clause.
     *
     * @since 5.0
     */
    static List<UpgradeHandler> all() {
        return List.of(
                new UpgradeHandler_V7(),
                new UpgradeHandler_V8(),
                new UpgradeHandler_V9(),
                new UpgradeHandler_V10(),
                new UpgradeHandler_V11(),
                new UpgradeHandler_V12(),
                new UpgradeHandler_V13(),
                new UpgradeHandler_V14());
    }

    /**
     * Project version written by this version of Cayenne.
     *
     * @since 5.0
     */
    String CURRENT_VERSION = "14";

    /**
     * The oldest project version that can be upgraded to {@link #CURRENT_VERSION}. Older projects must first be
     * upgraded with an older CayenneModeler.
     *
     * @since 5.0
     */
    String MIN_SUPPORTED_VERSION = "6";

    /**
     * Version reported for projects that have no "project-version" attribute.
     *
     * @since 5.0
     */
    String UNKNOWN_VERSION = "0";


    /**
     * root tag for the cgen extension
     *
     * @since 5.0
     */
    String CGEN = "cgen";

    /**
     * root tag for the dbImport extension
     *
     * @since 5.0
     */
    String DB_IMPORT = "dbImport";

    /**
     * root tag for the validation extension
     *
     * @since 5.0
     */
    String VALIDATION = "validation";

    /**
     * @return target version for this handler
     */
    String getVersion();

    /**
     * Process DOM for the project root file (e.g. cayenne-project.xml)
     */
    void processProjectDom(UpgradeContext upgradeUnit);

    /**
     * Process DOM for the data map file (e.g. datamap.map.xml)
     */
    void processDataMapDom(UpgradeContext upgradeUnit);

    /**
     * This method should be avoided as much as possible, as
     * using this method will make upgrade process not future proof and
     * will require refactoring if model should change.
     */
    default void processModel(DataChannelDescriptor dataChannelDescriptor) {
    }

    /**
     * Upgrade Domain schema and version info
     *
     * @param upgradeUnit for the datamap
     */
    default void updateDomainSchemaAndVersion(UpgradeContext upgradeUnit) {
        Element domain = upgradeUnit.getDocument().getDocumentElement();
        // update schema
        domain.setAttribute("xmlns", "http://cayenne.apache.org/schema/" + getVersion() + "/domain");
        domain.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        domain.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/" + getVersion() + "/domain " +
                "https://cayenne.apache.org/schema/" + getVersion() + "/domain.xsd");
        // update version
        domain.setAttribute("project-version", getVersion());
    }

    /**
     * Upgrade DataMap schema and version info
     *
     * @param upgradeUnit for the datamap
     */
    default void updateDataMapSchemaAndVersion(UpgradeContext upgradeUnit) {
        Element dataMap = upgradeUnit.getDocument().getDocumentElement();
        // update schema
        dataMap.setAttribute("xmlns", "http://cayenne.apache.org/schema/" + getVersion() + "/modelMap");
        dataMap.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/" + getVersion() + "/modelMap " +
                "https://cayenne.apache.org/schema/" + getVersion() + "/modelMap.xsd");
        // update version
        dataMap.setAttribute("project-version", getVersion());
    }

    /**
     * Update schema for the given extension in a datamap file (root element: data-map)
     *
     * @param upgradeUnit a unit to work with
     * @param extension   name of the extension (cgen, dbImport, etc.)
     */
    default void updateExtensionSchema(UpgradeContext upgradeUnit, String extension) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("/data-map/*[local-name()='" + extension + "']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return;
        }
        for (int j = 0; j < nodes.getLength(); j++) {
            Element element = (Element) nodes.item(j);
            element.setAttribute("xmlns", "http://cayenne.apache.org/schema/" + getVersion() + "/" + extension.toLowerCase());
        }
    }

    /**
     * Update schema for the given extension in a project file (root element: domain)
     *
     * @param upgradeUnit a unit to work with
     * @param extension   name of the extension (e.g. validation)
     * @since 5.0-M2
     */
    default void updateDomainExtensionSchema(UpgradeContext upgradeUnit, String extension) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("/domain/*[local-name()='" + extension + "']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return;
        }
        for (int j = 0; j < nodes.getLength(); j++) {
            Element element = (Element) nodes.item(j);
            element.setAttribute("xmlns", "http://cayenne.apache.org/schema/" + getVersion() + "/" + extension.toLowerCase());
        }
    }

    /**
     * Update the version-stamped namespace on {@code info:property} elements
     * (entity/attribute comments) in a datamap file.
     *
     * @param upgradeUnit a unit to work with
     * @since 5.0
     */
    default void updateInfoSchema(UpgradeContext upgradeUnit) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("//*[local-name()='property']",
                    upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return;
        }
        for (int j = 0; j < nodes.getLength(); j++) {
            Element element = (Element) nodes.item(j);
            if (element.hasAttribute("xmlns:info")) {
                element.setAttribute("xmlns:info", "http://cayenne.apache.org/schema/" + getVersion() + "/info");
            }
        }
    }
}
