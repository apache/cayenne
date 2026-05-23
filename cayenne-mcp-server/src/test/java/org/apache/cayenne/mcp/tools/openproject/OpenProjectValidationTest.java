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

import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectErrorCode;
import org.apache.cayenne.mcp.tools.openproject.protocol.OpenProjectResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OpenProjectValidationTest {

    private final OpenProjectTool tool = new OpenProjectTool(new PrefsLocator());

    @Test
    public void projectNotFound() {
        OpenProjectResult result = tool.run("/no/such/file/cayenne-project.xml");

        assertEquals("validation_failed", result.status());
        assertEquals(OpenProjectErrorCode.project_not_found, result.error().code());
        assertNull(result.resolved());
        assertNull(result.handshake());

        assertFalse(result.validation().projectFound());
        assertNull(result.validation().mcpJarLocated());
        assertNull(result.validation().modelerFound());
    }
}
