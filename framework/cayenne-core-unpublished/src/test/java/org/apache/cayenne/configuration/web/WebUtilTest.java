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
package org.apache.cayenne.configuration.web;

import org.apache.cayenne.configuration.CayenneRuntime;

import com.mockrunner.mock.web.MockServletContext;

import junit.framework.TestCase;

public class WebUtilTest extends TestCase {

    public void testGetCayenneRuntime() {
        MockServletContext context = new MockServletContext();

        assertNull(WebUtil.getCayenneRuntime(context));

        CayenneRuntime runtime = new CayenneRuntime() {
        };

        WebUtil.setCayenneRuntime(context, runtime);
        assertSame(runtime, WebUtil.getCayenneRuntime(context));

        CayenneRuntime runtime1 = new CayenneRuntime() {
        };

        WebUtil.setCayenneRuntime(context, runtime1);
        assertSame(runtime1, WebUtil.getCayenneRuntime(context));

        WebUtil.setCayenneRuntime(context, null);
        assertNull(WebUtil.getCayenneRuntime(context));
    }
}
