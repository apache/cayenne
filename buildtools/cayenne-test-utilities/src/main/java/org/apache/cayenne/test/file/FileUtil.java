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
package org.apache.cayenne.test.file;

import java.io.File;

public final class FileUtil {

    static final String TEST_DIR_KEY = "cayenne.test.dir";
    static final String DEFAULT_TEST_DIR = "target/testrun";

    private static final File baseTestDirectory;

    static {
        String testDirName = System.getProperty(TEST_DIR_KEY);

        if (testDirName == null) {
            testDirName = DEFAULT_TEST_DIR;
        }

        baseTestDirectory = new File(testDirName);

        // delete old tests
        if (baseTestDirectory.exists()) {
            if (!FileUtil.delete(testDirName, true)) {
                throw new RuntimeException("Error deleting test directory: "
                        + testDirName);
            }
        }

        if (!baseTestDirectory.mkdirs()) {
            throw new RuntimeException("Error creating test directory: " + testDirName);
        }
    }

    /**
     * Returns a test directory that is used as a scratch area.
     */
    public static File baseTestDirectory() {
        return baseTestDirectory;
    }

    public static boolean delete(String filePath, boolean recursive) {
        File file = new File(filePath);
        if (!file.exists()) {
            return true;
        }

        if (!recursive || !file.isDirectory())
            return file.delete();

        String[] contents = file.list();

        // list can be null if directory doesn't have an 'x' permission bit set for the
        // user
        if (contents != null) {
            for (String item : contents) {
                if (!delete(filePath + File.separator + item, true)) {
                    return false;
                }
            }
        }

        return file.delete();
    }
}
