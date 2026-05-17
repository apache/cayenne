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
package org.apache.cayenne.mcp.tools.openproject;

import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Optional;

/**
 * Resolves the directory that contains the running MCP server jar. Used as the
 * starting point for {@link ModelerDiscovery}.
 *
 * @since 5.0
 */
final class McpJarLocator {

    private McpJarLocator() {
    }

    /**
     * Locates the directory containing the jar (or class output dir) the given class
     * was loaded from. Returns {@code Optional.empty()} in exotic launch configurations
     * where the protection domain has no resolvable location.
     */
    static Optional<Path> locate(Class<?> anchor) {
        try {
            ProtectionDomain pd = anchor.getProtectionDomain();
            if (pd == null) {
                return Optional.empty();
            }
            CodeSource cs = pd.getCodeSource();
            if (cs == null) {
                return Optional.empty();
            }
            URL url = cs.getLocation();
            if (url == null) {
                return Optional.empty();
            }
            Path location = Paths.get(url.toURI());
            // For a jar: location is the jar itself; we want its parent directory.
            // For a class-file directory (IDE / surefire fork): location is the dir;
            // its parent is also a reasonable starting point (target/), but here we
            // want the dir that "would have been the jar's parent" — so use it directly.
            return Optional.of(location.getParent() != null ? location.getParent() : location);
        } catch (URISyntaxException | RuntimeException e) {
            return Optional.empty();
        }
    }
}
