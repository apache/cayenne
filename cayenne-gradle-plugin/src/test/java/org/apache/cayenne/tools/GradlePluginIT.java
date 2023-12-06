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

package org.apache.cayenne.tools;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.Test;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GradlePluginIT extends BaseTaskIT {

    private void testDbImportWithGradleVersion(String version) throws Exception {
        String dbUrl = "jdbc:derby:" + projectDir.getAbsolutePath() + "/build/" + version.replace('.', '_');
        dbUrl += ";create=true";
        GradleRunner runner = createRunner("dbimport_simple_db", "cdbimport", "--info", "-PdbUrl=" + dbUrl);
        runner.withGradleVersion(version);
        runner.build();
    }

    private void testCgenWithGradleVersion(String version) throws Exception {
        GradleRunner runner = createRunner(
                "cgen_default_config",
                "cgen",
                "-PdataMap=test_datamap.map.xml"
        );
        runner.withGradleVersion(version);
        runner.build();
    }

    @Test
    public void testGradleVersionsCompatibility() throws Exception {

        String[] versions;

        // Old gradle versions will fail on new JDK
        int javaMajorVersion = getJavaMajorVersion(System.getProperty("java.version"));
        if(javaMajorVersion >= 22) {
            versions = new String[]{"8.8"};
        } else if(javaMajorVersion >= 21) {
            versions = new String[]{"8.5"};
        } else if(javaMajorVersion >= 19) {
            versions = new String[]{"7.6"};
        } else if(javaMajorVersion >= 17) {
            versions = new String[]{"7.3"};
        } else if(javaMajorVersion >= 16) {
            versions = new String[]{"7.0"};
        } else if(javaMajorVersion >= 11) {
            versions = new String[]{"4.8"};
        } else if (javaMajorVersion < 9) {
            versions = new String[]{"4.3", "4.0", "3.5"};
        } else {
            versions = new String[]{"4.3.1", "4.3"};
        }

        List<String> failedVersions = new ArrayList<>();
        for(String version : versions) {
            try {
                testDbImportWithGradleVersion(version);
                testCgenWithGradleVersion(version);
            } catch(Throwable th) {
                failedVersions.add(version);
            }
        }

        StringBuilder versionString = new StringBuilder("Failed versions:");
        for(String version : failedVersions) {
            versionString.append(" ").append(version);
        }
        assertTrue(versionString.toString(), failedVersions.isEmpty());
    }

    @Test
    public void testVersion() {
        assertEquals(7, getJavaMajorVersion("1.7.0_25-b15"));
        assertEquals(7, getJavaMajorVersion("1.7.2+123"));
        assertEquals(8, getJavaMajorVersion("1.8.145"));
        assertEquals(9, getJavaMajorVersion("9-ea+19"));
        assertEquals(9, getJavaMajorVersion("9+100"));
        assertEquals(9, getJavaMajorVersion("9"));
        assertEquals(9, getJavaMajorVersion("9.0.1"));
        assertEquals(10, getJavaMajorVersion("10-ea+38"));
    }

    // will fail on Java 1.1 or earlier :)
    private static int getJavaMajorVersion(String versionString) {
        int index = 0, prevIndex = 0, version = 0;
        if((index = versionString.indexOf("-")) >= 0) {
            versionString = versionString.substring(0, index);
        }
        if((index = versionString.indexOf("+")) >= 0) {
            versionString = versionString.substring(0, index);
        }

        while(version < 2) {
            index = versionString.indexOf(".", prevIndex);
            if(index == -1) {
                index = versionString.length();
            }
            version = Integer.parseInt(versionString.substring(prevIndex, index));
            prevIndex = index + 1;
        }
        return version;
    }
}
