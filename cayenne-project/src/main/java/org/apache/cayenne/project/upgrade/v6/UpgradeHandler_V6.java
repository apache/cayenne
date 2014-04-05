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
package org.apache.cayenne.project.upgrade.v6;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.upgrade.BaseUpgradeHandler;
import org.apache.cayenne.project.upgrade.UpgradeMetaData;
import org.apache.cayenne.project.upgrade.UpgradeType;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.Util;

/**
 * @since 3.1
 */
class UpgradeHandler_V6 extends BaseUpgradeHandler {

    static final String TO_VERSION = "6";
    static final String MIN_SUPPORTED_VERSION = "3.0.0.1";

    /**
     * Notice that the loader is statically typed, intentionally not using DI to ensure
     * predictable behavior on legacy upgrades.
     */
    private XMLDataChannelDescriptorLoader_V3_0_0_1 projectLoader;

    /**
     * Unlike loader, saver is injected, so that we can save dynamically with the latest
     * version. This may change once this upgrade handler becomes an intermediate handler.
     */
    @Inject
    private ProjectSaver projectSaver;

    UpgradeHandler_V6(Resource source) {
        super(source);
        this.projectLoader = new XMLDataChannelDescriptorLoader_V3_0_0_1();
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

    @Override
    protected Resource doPerformUpgrade(UpgradeMetaData metaData) throws ConfigurationException {

        List<DataChannelDescriptor> domains = projectLoader.load(projectSource);
        if (domains.isEmpty()) {
            // create a single domain dummy project if a noop config is being upgraded
            DataChannelDescriptor descriptor = new DataChannelDescriptor();
            descriptor.setName("DEFAULT");
            domains.add(descriptor);
        }

        // collect resources to delete before the upgrade...
        Collection<Resource> resourcesToDelete = new ArrayList<Resource>();
        for (DataChannelDescriptor descriptor : domains) {
            for (DataNodeDescriptor node : descriptor.getNodeDescriptors()) {
                Resource nodeResource = node.getConfigurationSource();
                if (nodeResource != null) {
                    resourcesToDelete.add(nodeResource);
                }
            }
        }

        // save in the new format
        for (DataChannelDescriptor descriptor : domains) {
            Project project = new Project(new ConfigurationTree<DataChannelDescriptor>(
                    descriptor));
            
            attachToNamespace((DataChannelDescriptor) project.getRootNode());

            // side effect of that is deletion of the common "cayenne.xml"
            projectSaver.save(project);
        }

        // delete all .driver.xml files
        for (Resource resource : resourcesToDelete) {
            try {
                File file = Util.toFile(resource.getURL());
                if (file.exists() && file.getName().endsWith(".driver.xml")) {
                    file.delete();
                }
            }
            catch (Exception e) {
                // ignore...
            }
        }

        // returns the first domain configuration out of possibly multiple new
        // configurations...
        return domains.get(0).getConfigurationSource();
    }
}
