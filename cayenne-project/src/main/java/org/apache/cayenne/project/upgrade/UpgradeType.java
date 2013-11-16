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
 * An enum indicating what type of upgrade, if any, is needed and possible to bring the
 * project configuration format in sync with the current Cayenne runtime.
 * 
 * @since 3.1
 */
public enum UpgradeType {

    /**
     * The project configuration is current and can be used with the enclosing Cayenne
     * runtime.
     */
    UPGRADE_NOT_NEEDED,

    /**
     * The project configuration format is from an older version of Cayenne, and it can be
     * fully upgraded to the current Cayenne version.
     */
    UPGRADE_NEEDED,

    /**
     * The project configuration format is from an older version of Cayenne, and it can be
     * upgraded to the current Cayenne version, however some information may get lost
     * during the upgrade. A user may still chose to upgrade, however a recommended
     * upgrade path is to do an intermediary upgrade using previous versions of Cayenne.
     */
    INTERMEDIATE_UPGRADE_NEEDED,

    /**
     * The project configuration format is from a newer version of Cayenne, and the
     * current runtime can't work with it.
     */
    DOWNGRADE_NEEDED
}
