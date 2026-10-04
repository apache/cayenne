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

package org.apache.cayenne.project.upgrade;

import java.util.List;

/**
 * A step of the project upgrade that brings the XML and the model from the previous project version to the version
 * it reports in {@link #getVersion()}. The set of handlers is closed: every handler is a permitted implementation of
 * this interface.
 *
 * @since 4.1
 */
public sealed interface UpgradeHandler permits
        UpgradeHandler_V10,
        UpgradeHandler_V11,
        UpgradeHandler_V12,
        UpgradeHandler_V13,
        UpgradeHandler_V14 {

    /**
     * Creates an instance of every permitted handler, ordered from the oldest to the current version. Must be kept in
     * sync with the "permits" clause.
     *
     * @since 5.0
     */
    static List<UpgradeHandler> all() {
        return List.of(
                new UpgradeHandler_V10(),
                new UpgradeHandler_V11(),
                new UpgradeHandler_V12(),
                new UpgradeHandler_V13(),
                new UpgradeHandler_V14());
    }

    /**
     * Project version written by this version of Cayenne.
     *
     * @since 5.0
     */
    String CURRENT_VERSION = "14";

    /**
     * The oldest project version that can be upgraded to {@link #CURRENT_VERSION}. Older projects must first be
     * upgraded with an older CayenneModeler.
     *
     * @since 5.0
     */
    String MIN_SUPPORTED_VERSION = "9";

    /**
     * Version reported for projects that whose version can not be determined.
     *
     * @since 5.0
     */
    String UNKNOWN_VERSION = "0";

    String getVersion();

    /**
     * @since 5.0
     */
    void upgradeProjectDOM(UpgradeContext context);

    /**
     * @since 5.0
     */
    void upgradeDataMapDOM(UpgradeContext context);
}
