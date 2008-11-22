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

package org.apache.cayenne.conf;

import org.apache.cayenne.util.ResourceLocator;
import org.apache.cayenne.util.WebApplicationResourceLocator;
import org.apache.cayenne.util.WebApplicationResourceLocatorAccess;

import com.mockrunner.mock.web.MockServletContext;

import junit.framework.TestCase;

/**
 */
public class ServletUtilTest extends TestCase {

    public void testCreateLocator() {
        MockServletContext context = new MockServletContext();
        context.setInitParameter(ServletUtil.CONFIGURATION_PATH_KEY, "/WEB-INF/xyz");

        ResourceLocator locator = ServletUtil.createLocator(context);
        assertNotNull("Locator not initialized", locator);
        assertTrue(
                "Unexpected Locator type: " + locator.getClass().getName(),
                locator instanceof WebApplicationResourceLocator);
        WebApplicationResourceLocatorAccess accessor = new WebApplicationResourceLocatorAccess(
                (WebApplicationResourceLocator) locator);

        assertTrue(accessor.getAdditionalContextPaths().contains("/WEB-INF/xyz"));
    }
}
