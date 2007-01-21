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

package org.apache.cayenne.access.jdbc;

import org.apache.velocity.Template;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.ResourceManager;
import org.apache.cayenne.unit.BasicCase;

/**
 * @author Andrus Adamchik
 */
public class SQLTemplateResourceManagerTest extends BasicCase {
    public void testFetResource() throws Exception {
        SQLTemplateResourceManager rm = new SQLTemplateResourceManager();
        rm.initialize(new MockupRuntimeServices());

        Resource resource =
            rm.getResource(
                "abc",
                ResourceManager.RESOURCE_TEMPLATE,
                RuntimeConstants.ENCODING_DEFAULT);

        assertTrue(resource instanceof Template);

        // must be cached...
        assertSame(
            resource,
            rm.getResource(
                "abc",
                ResourceManager.RESOURCE_TEMPLATE,
                RuntimeConstants.ENCODING_DEFAULT));

        // new resource must be different
        assertNotSame(
            resource,
            rm.getResource(
                "xyz",
                ResourceManager.RESOURCE_TEMPLATE,
                RuntimeConstants.ENCODING_DEFAULT));

        // after clearing cache, resource must be refreshed
        rm.clearCache();
        assertNotSame(
            resource,
            rm.getResource(
                "abc",
                ResourceManager.RESOURCE_TEMPLATE,
                RuntimeConstants.ENCODING_DEFAULT));
    }
}
