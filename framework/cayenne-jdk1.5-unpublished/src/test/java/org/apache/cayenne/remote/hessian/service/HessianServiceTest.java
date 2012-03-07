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

package org.apache.cayenne.remote.hessian.service;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import junit.framework.TestCase;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.event.MockEventBridgeFactory;

import com.caucho.services.server.ServiceContext;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpSession;

public class HessianServiceTest extends TestCase {

    public void testGetSession() throws Exception {

        Map<String, String> map = new HashMap<String, String>();
        map.put(
                Constants.SERVER_ROP_EVENT_BRIDGE_FACTORY_PROPERTY,
                MockEventBridgeFactory.class.getName());

        ObjectContextFactory factory = new ObjectContextFactory() {

            public ObjectContext createContext(DataChannel parent) {
                return null;
            }

            public ObjectContext createContext() {
                return null;
            }
        };
        HessianService service = new HessianService(factory, map);

        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = new MockHttpSession();
        request.setSession(session);

        // for some eason need to call this to get session activated in the mock request
        request.getSession();

        try {
            ServiceContext.begin(request, null, null);
            assertSame(session, service.getSession(false));
        }
        finally {
            ServiceContext.end();
        }
    }
}
