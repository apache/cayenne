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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.resource.Resource;

/**
 * A stateful helper object for analyzing the projects and performing upgrades.
 * 
 * @since 3.1
 */
public interface UpgradeHandler {

    /**
     * Returns the original configuration source for the project before the upgrade.
     */
    Resource getProjectSource();

    /**
     * Returns a metadata object containing information about the upgrade to be performed.
     * Users should call this method before invoking {@link #performUpgrade()}, to make
     * sure upgrade is needed and possible. Tools (like CayenneModeler) may use this
     * object to build user-friendly messages asking for user input on the upgrade.
     */
    UpgradeMetaData getUpgradeMetaData();

    /**
     * Performs an in-place project configuration upgrade, throwing a
     * {@link ConfigurationException} if the upgrade fails. Before doing the upgrade,
     * check the handler {@link UpgradeMetaData}. Upgrades will succeed only for projects
     * that have {@link UpgradeType#UPGRADE_NEEDED} or
     * {@link UpgradeType#UPGRADE_NOT_NEEDED} statuses. In the later case of course,
     * upgrade will simply be skipped.
     * 
     * @return a configuration Resource for the upgraded project. Depending on the upgrade
     *         type, it may be the same resource as the original configuration, or a
     *         totally different resource.
     */
    Resource performUpgrade() throws ConfigurationException;
}
