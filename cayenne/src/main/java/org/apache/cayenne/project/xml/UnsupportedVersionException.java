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

package org.apache.cayenne.project.xml;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.project.upgrade.UpgradeHandler;

/**
 * Thrown by the XML handlers when the project version of a document is not the current one. The XML loaders catch
 * it and attempt an in-memory upgrade of the document.
 *
 * @since 5.0
 */
public class UnsupportedVersionException extends CayenneRuntimeException {

    private final String version;

    public UnsupportedVersionException(String version) {
        super("Unsupported project version: %s, expected: %s", version, UpgradeHandler.CURRENT_VERSION);
        this.version = version;
    }

    /**
     * @return the project version of the document, or {@link UpgradeHandler#UNKNOWN_VERSION} if it has none
     */
    public String getVersion() {
        return version;
    }
}
