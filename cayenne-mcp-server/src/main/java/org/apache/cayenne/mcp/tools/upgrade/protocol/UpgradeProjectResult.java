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

package org.apache.cayenne.mcp.tools.upgrade.protocol;

import org.apache.cayenne.project.upgrade.UpgradeChange;

import java.util.List;

/**
 * Top-level envelope returned by the {@code upgrade_project} MCP tool.
 *
 * @param status        one of "up_to_date", "upgraded" or "error"
 * @param fromVersion   the project version before the upgrade, if known
 * @param toVersion     the current project version
 * @param changes       the changes the upgrade made
 * @since 6.0
 */
public record UpgradeProjectResult(
        String status,
        String fromVersion,
        String toVersion,
        List<UpgradeChange> changes,
        UpgradeError error
) {}
