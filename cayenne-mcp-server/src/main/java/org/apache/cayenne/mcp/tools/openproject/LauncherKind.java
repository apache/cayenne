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

/**
 * How a discovered Modeler is to be launched. Distinct from
 * {@link org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectDistribution},
 * which is reported in the response and tracks <em>how it was found</em> (the
 * source-tree probe maps to a launcher kind for argv construction while reporting
 * {@code source_tree} as the matched distribution).
 *
 * @since 5.0
 */
enum LauncherKind {
    MAC_APP,
    WINDOWS_EXE,
    GENERIC_JAR
}
