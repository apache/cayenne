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

package org.apache.cayenne.util;

import junit.framework.TestCase;

import com.mockrunner.mock.web.MockServletContext;

/**
 * @author Andrus Adamchik
 */
public class WebApplicationResourceLocatorTest extends TestCase {

    public void testServletContext() {
        MockServletContext context = new MockServletContext();

        WebApplicationResourceLocator locator = new WebApplicationResourceLocator();
        locator.setServletContext(context);
        assertSame(context, locator.getServletContext());
    }

    public void testAddContextPath() {
        WebApplicationResourceLocator locator = new WebApplicationResourceLocator();
        locator.addFilesystemPath("/WEB-INF/abc");
        assertTrue(locator.additionalContextPaths.contains("/WEB-INF/abc"));
    }
}
