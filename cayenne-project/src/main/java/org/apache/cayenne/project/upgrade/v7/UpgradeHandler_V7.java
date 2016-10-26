/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.project.upgrade.v7;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.XMLDataChannelDescriptorLoader;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.upgrade.BaseUpgradeHandler;
import org.apache.cayenne.project.upgrade.UpgradeHandler;
import org.apache.cayenne.project.upgrade.UpgradeMetaData;
import org.apache.cayenne.project.upgrade.v6.ProjectUpgrader_V6;
import org.apache.cayenne.resource.Resource;

import java.util.ArrayList;
import java.util.List;

class UpgradeHandler_V7 extends BaseUpgradeHandler {

    static final String PREVIOUS_VERSION = "6";
    static final String TO_VERSION = "7";

    // this will be removed from DataDomain soon. So caching this property name
    // in the upgrade handler
    static final String USING_EXTERNAL_TRANSACTIONS_PROPERTY = "cayenne.DataDomain.usingExternalTransactions";

    @Inject
    protected Injector injector;

    @Inject
    private ProjectSaver projectSaver;

    public UpgradeHandler_V7(Resource source) {
        super(source);
    }

    @Override
    protected Resource doPerformUpgrade(UpgradeMetaData metaData) throws ConfigurationException {
        if (compareVersions(metaData.getProjectVersion(), PREVIOUS_VERSION) == -1) {
            ProjectUpgrader_V6 upgraderV6 = new ProjectUpgrader_V6();
            injector.injectMembers(upgraderV6);
            UpgradeHandler handlerV6 = upgraderV6.getUpgradeHandler(projectSource);
            projectSource = handlerV6.performUpgrade();
        }

        XMLDataChannelDescriptorLoader loader = new XMLDataChannelDescriptorLoader();
        injector.injectMembers(loader);
        ConfigurationTree<DataChannelDescriptor> tree = loader.load(projectSource);
        Project project = new Project(tree);

        attachToNamespace((DataChannelDescriptor) project.getRootNode());
        
        removeExternalTxProperty(project);

        // remove "shadow" attributes per CAY-1795
        removeShadowAttributes(project);

        // load and safe cycle removes objects no longer supported, specifically
        // listeners
        projectSaver.save(project);
        return project.getConfigurationResource();
    }

    private void removeExternalTxProperty(Project project) {
        DataChannelDescriptor rootNode = (DataChannelDescriptor) project.getRootNode();
        rootNode.getProperties().remove(USING_EXTERNAL_TRANSACTIONS_PROPERTY);
    }

    private void removeShadowAttributes(Project project) {
        DataChannelDescriptor rootNode = (DataChannelDescriptor) project.getRootNode();

        for (DataMap dataMap : rootNode.getDataMaps()) {

            // if objEntity has super entity, then checks it
            // for duplicated attributes
            for (ObjEntity objEntity : dataMap.getObjEntities()) {
                ObjEntity superEntity = objEntity.getSuperEntity();
                if (superEntity != null) {
                    removeShadowAttributes(objEntity, superEntity);
                }
            }
        }
    }

    /**
     * Remove attributes from objEntity, if superEntity has attributes with same
     * names.
     */
    private void removeShadowAttributes(ObjEntity objEntity, ObjEntity superEntity) {

        List<String> delList = new ArrayList<String>();

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

    @Override
    protected String getToVersion() {
        return TO_VERSION;
    }

}
