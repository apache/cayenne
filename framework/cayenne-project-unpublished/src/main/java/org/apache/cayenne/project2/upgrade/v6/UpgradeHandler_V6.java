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
package org.apache.cayenne.project2.upgrade.v6;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.project2.upgrade.BaseUpgradeHandler;
import org.apache.cayenne.project2.upgrade.UpgradeMetaData;
import org.apache.cayenne.project2.upgrade.UpgradeType;
import org.apache.cayenne.resource.Resource;

/**
 * @since 3.1
 */
class UpgradeHandler_V6 extends BaseUpgradeHandler {

    static final String TO_VERSION = "6";
    static final String MIN_SUPPORTED_VERSION = "3.0.0.1";

    UpgradeHandler_V6(Resource source) {
        super(source);
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
    protected Resource performNeededUpgrade() throws ConfigurationException {
        throw new UnsupportedOperationException("TODO");
    }

}
