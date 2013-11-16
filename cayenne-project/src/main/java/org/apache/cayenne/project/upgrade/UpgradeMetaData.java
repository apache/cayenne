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
package org.apache.cayenne.project.upgrade;

/**
 * An object providing upgrade information about a specific project in the context of
 * enclosing Cayenne runtime.
 * 
 * @since 3.1
 */
public class UpgradeMetaData {

    protected UpgradeType upgradeType;
    protected String projectVersion;
    protected String supportedVersion;
    protected String intermediateUpgradeVersion;

    public UpgradeType getUpgradeType() {
        return upgradeType;
    }

    public void setUpgradeType(UpgradeType upgradeType) {
        this.upgradeType = upgradeType;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    public String getSupportedVersion() {
        return supportedVersion;
    }

    public void setSupportedVersion(String supportedVersion) {
        this.supportedVersion = supportedVersion;
    }

    public String getIntermediateUpgradeVersion() {
        return intermediateUpgradeVersion;
    }

    public void setIntermediateUpgradeVersion(String intermediateUpgradeVersion) {
        this.intermediateUpgradeVersion = intermediateUpgradeVersion;
    }

}
