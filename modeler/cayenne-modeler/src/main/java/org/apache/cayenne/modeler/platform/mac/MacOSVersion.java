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

package org.apache.cayenne.modeler.platform.mac;

import org.apache.cayenne.modeler.service.os.OperatingSystem;

public record MacOSVersion(int major, int minor) {

    public static final MacOSVersion UNKNOWN = new MacOSVersion(-1, -1);
    public static final MacOSVersion CATALINA = new MacOSVersion(10, 15);
    public static final MacOSVersion BIG_SUR = new MacOSVersion(10, 16);

    public static MacOSVersion fromSystemProperties() {

        // sanity check in case this code executed not on macOS
        if (OperatingSystem.os != OperatingSystem.MAC_OS) {
            return UNKNOWN;
        }

        String osVersion = System.getProperty("os.version");
        String[] osVersionComponents = osVersion.split("\\.");
        if (osVersionComponents.length != 2) {
            return UNKNOWN;
        }

        try {
            int major = Integer.parseInt(osVersionComponents[0]);
            int minor = Integer.parseInt(osVersionComponents[1]);
            return new MacOSVersion(major, minor);
        } catch (Exception ex) {
            return UNKNOWN;
        }
    }

    public boolean gt(MacOSVersion version) {
        return major() > version.major() || (major() == version.major() && minor() > version.minor());
    }
}
