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

package org.apache.cayenne.gen.internal;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Internally used utilities related to cgen.
 *
 * @since 5.0
 */
public class Utils {

    public static Optional<String> getMavenSrcPathForPath(Path path) {
        return getMavenSrcPathForPath(path.toAbsolutePath().toString());
    }

    /**
     * @param path to check
     * @return path in the maven src layout or {@code Optional.empty()} if we are not inside maven project structure
     */
    public static Optional<String> getMavenSrcPathForPath(String path) {
        // check if we are in src/test/resources
        String testDirPath = checkDefaultMavenResourceDir(path, "test");
        if(testDirPath != null) {
            return Optional.of(testDirPath);
        }

        // check if we are in src/main/resources
        String mainDirPath =  checkDefaultMavenResourceDir(path, "main");
        if(mainDirPath != null) {
            return Optional.of(mainDirPath);
        }

        return Optional.empty();
    }

    private static String checkDefaultMavenResourceDir(String path, String dirType) {
        String resourcePath = buildFilePath("src", dirType, "resources");
        int idx = path.indexOf(resourcePath);
        if (idx < 0) {
            return null;
        }
        return path.substring(0, idx) + buildFilePath("src", dirType, "java");
    }

    private static String buildFilePath(String... pathElements) {
        return String.join(File.separator, pathElements);
    }
}
