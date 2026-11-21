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
package org.apache.cayenne.mcp.tools.openproject.protocol;

/**
 * Where the {@code open_project} tool got the CayenneModeler from.
 *
 * @since 6.0
 */
public enum OpenProjectModelerSource {

    /**
     * The Modeler installation the running MCP jar is bundled with.
     */
    bundled,

    /**
     * The runnable Modeler jar of the server version, fetched from a Maven repository (or found in the local cache).
     */
    downloaded
}
