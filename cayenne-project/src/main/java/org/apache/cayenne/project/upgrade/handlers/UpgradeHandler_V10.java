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

/**
 * Upgrade handler for the project version "10" introduced by 4.1.M1 release.
 * Changes highlight:
 *      - strict schema for domain (e.g. main project document)
 *      - new schema for data map allowing usage of additional elements (e.g. XML extensions)
 *
 * @since 4.1
 */
public class UpgradeHandler_V10 implements UpgradeHandler {

    @Override
    public ProjectVersion getVersion() {
        return ProjectVersion.V10;
    }

    @Override
    public void processProjectDom(UpgradeUnit upgradeUnit) {
        // introduce xml namespace and schema for domain
        updateDomainSchemaAndVersion(upgradeUnit);
    }

    @Override
    public void processDataMapDom(UpgradeUnit upgradeUnit) {
        updateDataMapSchemaAndVersion(upgradeUnit);
    }
}
