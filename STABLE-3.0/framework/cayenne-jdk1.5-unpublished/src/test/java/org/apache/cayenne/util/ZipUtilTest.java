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


package org.apache.cayenne.util;

import java.io.File;
import java.net.URL;

import org.apache.cayenne.unit.BasicCase;

/**
 */
public class ZipUtilTest extends BasicCase {

    public void testUnzip() throws Exception {

        URL jarResource = Thread.currentThread().getContextClassLoader().getResource(
                "jar-test.jar");
        File jarCopy = new File(getTestDir(), "jar-test.jar");
        Util.copy(jarResource, jarCopy);

        File unjarDir = getTestDir();
        File unjarRootDir = new File(unjarDir, "jar-test");
        File manifest = new File(unjarRootDir.getParentFile(), "META-INF"
                + File.separator
                + "MANIFEST.MF");
        assertFalse(unjarRootDir.exists());
        assertFalse(manifest.exists());

        try {
            // try unzipping the JAR
            ZipUtil.unzip(jarCopy, unjarDir);

            assertTrue(unjarRootDir.isDirectory());
            assertTrue(new File(unjarRootDir, "jar-test1.txt").length() > 0);
            assertTrue(new File(unjarRootDir, "jar-test2.txt").length() > 0);
            assertTrue(manifest.isFile());
        }
        finally {
            Util.delete(unjarRootDir.getPath(), true);
            Util.delete(new File(unjarDir, "META-INF").getPath(), true);
        }
    }

    public void testZip() throws Exception {
        URL jarResource = Thread.currentThread().getContextClassLoader().getResource(
                "jar-test.jar");
        File jarCopy = new File(getTestDir(), "jar-test.jar");
        Util.copy(jarResource, jarCopy);

        File unjarDir = getTestDir();
        File unjarRootDir = new File(unjarDir, "jar-test");
        File newJarFile = new File(unjarDir, "new-jar.jar");

        try {
            // unzip existing jar and recreate
            assertFalse(unjarRootDir.exists());
            ZipUtil.unzip(jarCopy, unjarDir);

            ZipUtil.zip(newJarFile, unjarDir, new File[] {
                    unjarRootDir, new File(unjarDir, "META-INF")
            }, '/');

            assertTrue(newJarFile.isFile());

            // can't compare length, since different algorithms may have been used
            // assertEquals(jar.length(), newJarFile.length());

            // try unzipping it again
            Util.delete(unjarRootDir.getPath(), true);
            Util.delete(new File(unjarDir, "META-INF").getPath(), true);
            ZipUtil.unzip(newJarFile, unjarDir);

        }
        finally {
            Util.delete(unjarRootDir.getPath(), true);
            Util.delete(new File(unjarDir, "META-INF").getPath(), true);
            newJarFile.delete();
        }
    }
}
