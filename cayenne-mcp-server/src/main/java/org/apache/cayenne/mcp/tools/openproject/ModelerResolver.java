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

import org.apache.cayenne.mcp.tools.openproject.ModelerDiscovery.Found;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectDistribution;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectErrorCode;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectModelerSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Picks the CayenneModeler to open a project in: the installation the MCP jar is bundled with, if any, or else the
 * runnable Modeler jar of the server version, from a Maven repository. Either way, the Modeler is of the same version
 * as the server, so a project is never opened (and possibly saved) by a Modeler of another version.
 *
 * @since 6.0
 */
class ModelerResolver {

    // a release or a snapshot version, unlike e.g. "dev" of a server started from the IDE
    private static final Pattern PUBLISHED_VERSION = Pattern.compile("\\d+\\.\\d+.*");

    sealed interface Resolution permits Resolved, Unresolved {}

    record Resolved(Found found, OpenProjectModelerSource source) implements Resolution {}

    record Unresolved(OpenProjectErrorCode code, String message) implements Resolution {}

    private final OsKind osKind;
    private final Path mcpDir;
    private final String serverVersion;
    private final ModelerDownloader downloader;

    /**
     * @param mcpDir the directory of the running MCP jar, or {@code null} if it can't be resolved
     */
    ModelerResolver(OsKind osKind, Path mcpDir, String serverVersion, ModelerDownloader downloader) {
        this.osKind = osKind;
        this.mcpDir = mcpDir;
        this.serverVersion = serverVersion;
        this.downloader = downloader;
    }

    Resolution resolve() {

        if (mcpDir != null && ModelerDiscovery.discover(mcpDir, osKind) instanceof Found found) {
            return new Resolved(found, OpenProjectModelerSource.bundled);
        }

        if (!PUBLISHED_VERSION.matcher(serverVersion).matches()) {
            return new Unresolved(OpenProjectErrorCode.modeler_not_found, """
                    The MCP server is not bundled with CayenneModeler, and its version ("%s") is not a published \
                    one, so the matching Modeler can't be downloaded."""
                    .formatted(serverVersion));
        }

        try {
            Path jar = downloader.jar(serverVersion);
            return new Resolved(
                    new Found(OpenProjectDistribution.generic, LauncherKind.GENERIC_JAR, jar),
                    OpenProjectModelerSource.downloaded);
        } catch (IOException e) {
            return new Unresolved(OpenProjectErrorCode.modeler_download_failed,
                    "Can't download CayenneModeler %s: %s".formatted(serverVersion, e.getMessage()));
        }
    }
}
