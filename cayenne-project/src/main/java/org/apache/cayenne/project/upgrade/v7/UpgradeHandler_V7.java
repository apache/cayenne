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

import java.util.ArrayList;
import java.util.List;

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
import org.apache.cayenne.project.upgrade.UpgradeType;
import org.apache.cayenne.project.upgrade.v6.ProjectUpgrader_V6;
import org.apache.cayenne.resource.Resource;

class UpgradeHandler_V7 extends BaseUpgradeHandler {

    static final String TO_VERSION = "7";
    static final String MIN_SUPPORTED_VERSION = "3.0.0.1";
    
    @Inject
    protected Injector injector;
    
    @Inject
    private ProjectSaver projectSaver;
    
    public UpgradeHandler_V7(Resource source) {
        super(source);
    }

    @Override
    protected Resource doPerformUpgrade(UpgradeMetaData metaData) throws ConfigurationException {
        if (compareVersions(metaData.getProjectVersion(), MIN_SUPPORTED_VERSION) == 0) {
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
        
        // remove "shadow" attributes per CAY-1795
        checkObjEntities(project);
        
        // load and safe cycle removes objects no longer supported, specifically listeners
        projectSaver.save(project); 
        return project.getConfigurationResource();
    }
    
    private void checkObjEntities(Project project) {
        DataChannelDescriptor rootNode = (DataChannelDescriptor) project.getRootNode();

        List<DataMap> dataList = new ArrayList<DataMap>(rootNode.getDataMaps());
        List<ObjEntity> objEntityList = null;

        // take ObjEntities from DataMap
        if (!dataList.isEmpty()) {
            for (DataMap dataMap : dataList) {
                objEntityList = new ArrayList<ObjEntity>(dataMap.getObjEntities());
                ObjEntity superEntity = null;

                // if objEntity has super entity, then checks it
                // for duplicated attributes
                for (ObjEntity objEntity : objEntityList) {
                    superEntity = objEntity.getSuperEntity();
                    if (superEntity != null) {
                        removeDuplicatedAttributes(objEntity, superEntity);
                    }
                }
            }
        }
    }

    /**
     * Remove attributes from objEntity, if superEntity has attributes with same
     * names.
     */
    private void removeDuplicatedAttributes(ObjEntity objEntity, ObjEntity superEntity) {
        List<ObjAttribute> entityAttr = new ArrayList<ObjAttribute>(objEntity.getDeclaredAttributes());
        List<ObjAttribute> superEntityAttr = new ArrayList<ObjAttribute>(superEntity.getAttributes());
        List<String> delList = new ArrayList<String>();
        // entityAttr - attributes of objEntity, without inherited
        // superEntityAttr - all attributes in the superEntity inheritance
        // hierarchy
        // delList - attributes, that will be removed from objEntity

        // if subAttr and superAttr have same names, adds subAttr to delList
        for (ObjAttribute subAttr : entityAttr) {
            for (ObjAttribute superAttr : superEntityAttr) {
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
    protected UpgradeMetaData loadMetaData() {
        String version = loadProjectVersion();

        UpgradeMetaData metadata = new UpgradeMetaData();
        metadata.setSupportedVersion(TO_VERSION);
        metadata.setProjectVersion(version);

        int c1 = compareVersions(version, MIN_SUPPORTED_VERSION);
        int c2 = compareVersions(TO_VERSION, version);
        
        if (c1 < 0) {
            metadata.setIntermediateUpgradeVersion(MIN_SUPPORTED_VERSION);
            metadata.setUpgradeType(UpgradeType.INTERMEDIATE_UPGRADE_NEEDED);
        }
        else if (c2 < 0) {
            metadata.setUpgradeType(UpgradeType.DOWNGRADE_NEEDED);
        }
        else if (c2 == 0) {
            metadata.setUpgradeType(UpgradeType.UPGRADE_NOT_NEEDED);
        }
        else {
            metadata.setUpgradeType(UpgradeType.UPGRADE_NEEDED);
        }

        return metadata;
    }

}
