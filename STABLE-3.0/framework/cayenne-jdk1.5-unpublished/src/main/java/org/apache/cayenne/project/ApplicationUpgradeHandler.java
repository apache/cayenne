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

package org.apache.cayenne.project;

import java.util.Collection;

import org.apache.cayenne.conf.Configuration;

/**
 * A Cayenne project upgrade handler that relies on individual loaders to be able to read
 * older versions and convert them to the latest version in memory. This handler simply
 * checks the version to trigger the update in the calling UI, and saves what's been
 * already loaded.
 * 
 * @since 1.1
 */
abstract class ApplicationUpgradeHandler {

    private static final ApplicationUpgradeHandler sharedInstance = new UpgradeHandler_3_0_0_1();

    static ApplicationUpgradeHandler sharedHandler() {
        return sharedInstance;
    }

    abstract String supportedVersion();

    abstract int checkForUpgrades(Configuration project, Collection appendMessages);

    abstract void performUpgrade(ApplicationProject project) throws ProjectException;

    int compareVersion(String version) {
        double supported = decodeVersion(supportedVersion());
        double newVersion = decodeVersion(version);
        return supported < newVersion ? -1 : (supported == newVersion) ? 0 : 1;
    }

    static double decodeVersion(String version) {
        if (version == null || version.trim().length() == 0) {
            return 0;
        }

        // leave the first dot, and treat remaining as a fraction
        // remove all non digit chars
        StringBuilder buffer = new StringBuilder(version.length());
        boolean dotProcessed = false;
        for (int i = 0; i < version.length(); i++) {
            char nextChar = version.charAt(i);
            if (nextChar == '.' && !dotProcessed) {
                dotProcessed = true;
                buffer.append('.');
            }
            else if (Character.isDigit(nextChar)) {
                buffer.append(nextChar);
            }
        }

        return Double.parseDouble(buffer.toString());
    }

    static class UpgradeHandler_3_0_0_1 extends UpgradeHandler_3_0 {

        @Override
        String supportedVersion() {
            return "3.0.0.1";
        }
    }

    static class UpgradeHandler_3_0 extends UpgradeHandler_2_0 {

        @Override
        String supportedVersion() {
            return "3.0";
        }
    }

    static class UpgradeHandler_2_0 extends UpgradeHandler_1_1 {

        @Override
        String supportedVersion() {
            return "2.0";
        }
    }

    static class UpgradeHandler_1_1 extends ApplicationUpgradeHandler {

        @Override
        String supportedVersion() {
            return "1.1";
        }

        @Override
        void performUpgrade(ApplicationProject project) throws ProjectException {
            project.setModified(true);
            project.getConfiguration().setProjectVersion(supportedVersion());
            project.save();
        }

        @Override
        int checkForUpgrades(Configuration project, Collection appendMessages) {
            String loadedVersion = project.getProjectVersion();
            int versionState = compareVersion(loadedVersion);
            if (versionState < 0) {
                appendMessages.add("Newer Project Version Detected");
                return Project.UPGRADE_STATUS_NEW;
            }
            else if (versionState > 0) {
                appendMessages.add("Older Project Version Detected");
                return Project.UPGRADE_STATUS_OLD;
            }
            else {
                return Project.UPGRADE_STATUS_CURRENT;
            }
        }
    }
}
