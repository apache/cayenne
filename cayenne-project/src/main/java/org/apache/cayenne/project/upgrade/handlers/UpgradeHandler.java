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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.project.upgrade.UpgradeUnit;

/**
 * Interface that upgrade handlers should implement.
 * Implementation also should be injected into DI stack in right order.
 *
 * @since 4.1
 */
public interface UpgradeHandler {

    /**
     * @return target version for this handler
     */
    String getVersion();

    /**
     * Process DOM for the project root file (e.g. cayenne-project.xml)
     */
    void processProjectDom(UpgradeUnit upgradeUnit);

    /**
     * Process DOM for the data map file (e.g. datamap.map.xml)
     */
    void processDataMapDom(UpgradeUnit upgradeUnit);

    /**
     * This method should be avoided as much as possible, as
     * using this method will make upgrade process not future proof and
     * will require refactoring if model should change.
     */
    default void processModel(DataChannelDescriptor dataChannelDescriptor) {
    }

}
