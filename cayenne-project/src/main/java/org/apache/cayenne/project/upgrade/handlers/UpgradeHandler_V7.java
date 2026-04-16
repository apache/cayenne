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

import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.xml.ProjectVersion;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.project.upgrade.UpgradeUnit;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @since 4.1
 */
public class UpgradeHandler_V7 implements UpgradeHandler {

    @Override
    public ProjectVersion getVersion() {
        return ProjectVersion.V7;
    }

    @Override
    public void processProjectDom(UpgradeUnit upgradeUnit) {
        Element domain = upgradeUnit.getDocument().getDocumentElement();
        domain.setAttribute("project-version", getVersion().getAsString());

        XPath xpath = XPathFactory.newInstance().newXPath();
        Node node;
        try {
            node = (Node) xpath.evaluate("/domain/property[@name='cayenne.DataDomain.usingExternalTransactions']",
                    upgradeUnit.getDocument(), XPathConstants.NODE);
        }catch (Exception ex) {
            return;
        }

        if(node != null) {
            domain.removeChild(node);
        }
    }

    @Override
    public void processDataMapDom(UpgradeUnit upgradeUnit) {
        updateDataMapSchemaAndVersion(upgradeUnit);
    }

    @Override
    public void processModel(DataChannelDescriptor dataChannelDescriptor) {
        for (DataMap dataMap : dataChannelDescriptor.getDataMaps()) {
            // if objEntity has super entity, then checks it for duplicated attributes
            for (ObjEntity objEntity : dataMap.getObjEntities()) {
                ObjEntity superEntity = objEntity.getSuperEntity();
                if (superEntity != null) {
                    removeShadowAttributes(objEntity, superEntity);
                }
            }
        }
    }

    /**
     * Remove attributes from objEntity, if superEntity has attributes with same names.
     */
    private void removeShadowAttributes(ObjEntity objEntity, ObjEntity superEntity) {

        List<String> delList = new ArrayList<>();

        // if subAttr and superAttr have same names, adds subAttr to delList
        for (ObjAttribute subAttr : objEntity.getDeclaredAttributes()) {
            for (ObjAttribute superAttr : superEntity.getAttributes()) {
                if (subAttr.getName().equals(superAttr.getName())) {
                    delList.add(subAttr.getName());
                }
            }
        }

        if (!delList.isEmpty()) {
            for (String i : delList) {
                objEntity.removeAttribute(i);
            }
        }
    }
}
