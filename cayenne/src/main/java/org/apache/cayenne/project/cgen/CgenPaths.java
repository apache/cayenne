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

package org.apache.cayenne.project.cgen;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.Util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Path helpers for {@link CgenDescriptor#getDestDir()}, which is stored relative to the DataMap directory where
 * possible.
 *
 * @since 6.0
 */
public final class CgenPaths {

    private CgenPaths() {
    }

    /**
     * Returns the directory of the DataMap file, which need not exist. Empty if the DataMap location is not a "file:"
     * URL.
     */
    public static Optional<Path> dataMapDir(DataMap dataMap) {
        return resourceDir(dataMap.getConfigurationSource());
    }

    /**
     * Returns the directory of a resource file, which need not exist. Empty if the resource is null or its location is
     * not a "file:" URL.
     */
    // Util.toFile(..) handles the unencoded spaces a DataMap URL may have. It rejects the "file://server/share/.."
    // URLs of Windows UNC paths, which fall back to Path.of(..).
    public static Optional<Path> resourceDir(Resource resource) {
        URL url = resource != null ? resource.getURL() : null;
        if (url == null || !"file".equalsIgnoreCase(url.getProtocol())) {
            return Optional.empty();
        }

        Path path;
        try {
            path = Util.toFile(url).toPath();
        } catch (IllegalArgumentException e) {
            try {
                path = Path.of(new URI(url.toExternalForm().replace(" ", "%20")));
            } catch (URISyntaxException | IllegalArgumentException invalid) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(path.getParent());
    }

    /**
     * Sets a new location of a DataMap, rebasing the output directories of its class generation settings with
     * {@link #rebase(String, Path, Path)}. Not needed when saving, as the project saver rebases them itself.
     */
    public static void moveDataMap(DataMap dataMap, Resource newLocation) {
        Optional<Path> newRoot = resourceDir(newLocation);
        if (newRoot.isPresent()) {
            Path oldRoot = dataMapDir(dataMap).orElse(null);
            for (CgenDescriptor descriptor : dataMap.getCgenDescriptors()) {
                descriptor.setDestDir(rebase(descriptor.getDestDir(), oldRoot, newRoot.get()));
            }
        }

        dataMap.setConfigurationSource(newLocation);
    }

    /**
     * Maps a path within "src/(main|test)/resources" to the matching "src/(main|test)/java" directory. Empty if the
     * path is not in a Maven resource directory.
     */
    public static Optional<Path> mavenSourceDir(Path path) {
        String pathString = path.toString();
        String testDir = mavenSourceDir(pathString, "test");
        if (testDir != null) {
            return Optional.of(Path.of(testDir));
        }
        return Optional.ofNullable(mavenSourceDir(pathString, "main")).map(Path::of);
    }

    private static String mavenSourceDir(String path, String dirType) {
        String resourceDir = String.join(File.separator, "src", dirType, "resources");
        int index = path.indexOf(resourceDir);
        if (index < 0) {
            return null;
        }
        return path.substring(0, index) + String.join(File.separator, "src", dirType, "java");
    }

    /**
     * Encodes an output directory relative to the root where possible, with "/" separators, and "." for the root.
     *
     * @param root an absolute directory, or null not to relativize
     */
    static String encode(Path dir, Path root) {
        Path encoded = dir;

        // directories on different drives or shares of Windows can't be relativized
        if (root != null && root.isAbsolute() && dir.isAbsolute() && Objects.equals(root.getRoot(), dir.getRoot())) {
            encoded = root.relativize(dir);
        }

        String path = encoded.toString();
        return path.isEmpty() ? "." : Util.substBackslashes(path);
    }

    /**
     * Moves an output directory from one root to another, so that it keeps pointing at the same directory, and
     * encodes it as {@link #encode(Path, Path)} does. With no old root, a relative directory stays relative to the new
     * root, and an unset one is the Maven source directory of the new root, if it has one.
     *
     * @param oldRoot the old root, or null for a DataMap that was never saved or was loaded from a non-file URL
     */
    public static String rebase(String destDir, Path oldRoot, Path newRoot) {
        Path dir;
        try {
            dir = Path.of(destDir != null ? destDir : "");
        } catch (InvalidPathException e) {
            // a path of another OS, kept as is
            return destDir;
        }

        Path rebased;
        if (dir.isAbsolute()) {
            rebased = dir.normalize();
        } else if (oldRoot != null) {
            rebased = oldRoot.resolve(dir).normalize();
        } else if (dir.toString().isEmpty()) {
            rebased = mavenSourceDir(newRoot).orElse(dir);
        } else {
            rebased = dir.normalize();
        }

        return encode(rebased, newRoot);
    }
}
