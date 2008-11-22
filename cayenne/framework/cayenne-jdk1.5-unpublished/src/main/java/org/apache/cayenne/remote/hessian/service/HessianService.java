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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.cayenne.remote.hessian.EnumSerializerProxy;
import org.apache.cayenne.remote.hessian.HessianConfig;
import org.apache.cayenne.remote.service.HttpRemoteService;

import com.caucho.hessian.io.SerializerFactory;
import com.caucho.services.server.Service;
import com.caucho.services.server.ServiceContext;

/**
 * An implementation of RemoteService using binary Hessian protocol. For more info on
 * Hessian see http://www.caucho.com/resin-3.0/protocols/hessian.xtp.
 * 
 * @see org.apache.cayenne.remote.hessian.service.HessianServlet
 * @see org.apache.cayenne.remote.RemoteService
 * @since 1.2
 */
public class HessianService extends HttpRemoteService implements Service {

    public static final String[] SERVER_SERIALIZER_FACTORIES = new String[] {
            EnumSerializerProxy.class.getName(), ServerSerializerFactory.class.getName()
    };

    /**
     * Extracts parameters from ServletConfig and initializes the service.
     */
    public void init(ServletConfig config) throws ServletException {
        Map properties = new HashMap();

        Enumeration en = config.getInitParameterNames();
        while (en.hasMoreElements()) {
            String name = (String) en.nextElement();
            properties.put(name, config.getInitParameter(name));
        }

        initService(properties);
    }

    /**
     * Creates a Hessian SerializerFactory with Cayenne extensions.
     */
    SerializerFactory createSerializerFactory() {
        return HessianConfig.createFactory(SERVER_SERIALIZER_FACTORIES, getRootChannel()
                .getEntityResolver());
    }

    @Override
    protected HttpSession getSession(boolean create) {
        HttpServletRequest request = (HttpServletRequest) ServiceContext
                .getContextRequest();
        if (request == null) {
            throw new IllegalStateException(
                    "Attempt to access HttpSession outside the request scope.");
        }

        return request.getSession(create);
    }

    public void destroy() {
        destroyService();
    }
}
