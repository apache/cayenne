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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.xml.ProjectVersion;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.project.upgrade.UpgradeUnit;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @since 4.1
 */
public class UpgradeHandler_V8 implements UpgradeHandler {

    @Override
    public ProjectVersion getVersion() {
        return ProjectVersion.V8;
    }

    @Override
    public void processProjectDom(UpgradeUnit upgradeUnit) {
        Element domain = upgradeUnit.getDocument().getDocumentElement();
        domain.setAttribute("project-version", getVersion().getAsString());
    }

    @Override
    public void processDataMapDom(UpgradeUnit upgradeUnit) {
        updateDataMapSchemaAndVersion(upgradeUnit);

        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList queryNodes;
        try {
            queryNodes = (NodeList) xpath.evaluate("/data-map/query", upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception ex) {
            return;
        }

        for (int j = 0; j < queryNodes.getLength(); j++) {
            Element queryElement = (Element) queryNodes.item(j);
            String factory = queryElement.getAttribute("factory");
            if(factory == null || factory.isEmpty()) {
                continue;
            }

            String queryType;
            switch (factory) {
                case "org.apache.cayenne.map.SelectQueryBuilder":
                    queryType = QueryDescriptor.SELECT_QUERY;
                    break;
                case "org.apache.cayenne.map.SQLTemplateBuilder":
                    queryType = QueryDescriptor.SQL_TEMPLATE;
                    break;
                case "org.apache.cayenne.map.EjbqlBuilder":
                    queryType = QueryDescriptor.EJBQL_QUERY;
                    break;
                case "org.apache.cayenne.map.ProcedureQueryBuilder":
                    queryType = QueryDescriptor.PROCEDURE_QUERY;
                    break;
                default:
                    throw new ConfigurationException("Unknown query factory: " + factory);
            }

            queryElement.setAttribute("type", queryType);
            queryElement.removeAttribute("factory");
        }
    }
}
