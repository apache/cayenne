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

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import junit.framework.TestCase;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;

import com.mockrunner.mock.web.MockHttpSession;

/**
 * @author Andrei Adamchik
 */
public class WebApplicationListenerTst extends TestCase {

    public void testSessionCreated() throws Exception {
        HttpSession session = new MockHttpSession();
        assertNull(session.getAttribute(ServletUtil.DATA_CONTEXT_KEY));
        WebApplicationListener listener = createTestListener();

        // testing this..
        listener.sessionCreated(new HttpSessionEvent(session));

        // session must have a DataContext now...

        Object context = session.getAttribute(ServletUtil.DATA_CONTEXT_KEY);
        assertTrue(
                "DataContext was expected to be created, instead iot was " + context,
                context instanceof DataContext);
    }

    protected WebApplicationListener createTestListener() throws Exception {
        // configure mockup objects for the web listener environment...

        final Configuration config = new MockConfiguration();
        config.addDomain(new DataDomain("mockup"));
        return new WebApplicationListener() {

            protected Configuration getConfiguration() {
                return config;
            }
        };
    }
}
