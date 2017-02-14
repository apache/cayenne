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
package org.apache.cayenne.rop;

import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.configuration.rop.server.ROPServerModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.web.WebConfiguration;
import org.apache.cayenne.configuration.web.WebUtil;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.remote.ClientMessage;
import org.apache.cayenne.remote.RemoteService;
import org.apache.cayenne.remote.RemoteSession;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class ROPServlet extends HttpServlet {

    protected ServletContext servletContext;
	protected RemoteService remoteService;
    protected ROPSerializationService serializationService;

    @Override
    public void init(ServletConfig configuration) throws ServletException {

        checkAlreadyConfigured(configuration.getServletContext());

        this.servletContext = configuration.getServletContext();

        WebConfiguration configAdapter = new WebConfiguration(configuration);

        String configurationLocation = configAdapter.getConfigurationLocation();
        Map<String, String> eventBridgeParameters = configAdapter.getOtherParameters();

        Collection<Module> modules = configAdapter.createModules(new ROPServerModule(
                eventBridgeParameters));

        ServerRuntime runtime = ServerRuntime.builder()
                .addConfig(configurationLocation)
                .addModules(modules)
                .build();

        this.remoteService = runtime.getInjector().getInstance(RemoteService.class);
        this.serializationService = runtime.getInjector().getInstance(ROPSerializationService.class);

        WebUtil.setCayenneRuntime(servletContext, runtime);
        super.init(configuration);
    }

    protected void checkAlreadyConfigured(ServletContext context) throws ServletException {
        // sanity check
        if (WebUtil.getCayenneRuntime(context) != null) {
            throw new ServletException(
                    "CayenneRuntime is already configured in the servlet environment");
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        CayenneRuntime runtime = WebUtil.getCayenneRuntime(servletContext);
        if (runtime != null) {
            runtime.shutdown();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
            String serviceId = req.getPathInfo();
            String objectId = req.getParameter("id");

            if (objectId == null) {
                objectId = req.getParameter("ejbid");
            }

            ROPRequestContext.start(serviceId, objectId, req, resp);

            String operation = req.getParameter(ROPConstants.OPERATION_PARAMETER);

            if (operation != null) {
                switch (operation) {
                    case ROPConstants.ESTABLISH_SESSION_OPERATION:
                        RemoteSession session = remoteService.establishSession();
                        serializationService.serialize(session, resp.getOutputStream());
                        break;
                    case ROPConstants.ESTABLISH_SHARED_SESSION_OPERATION:
                        String sessionName = req.getParameter(ROPConstants.SESSION_NAME_PARAMETER);
                        RemoteSession sharedSession = remoteService.establishSharedSession(sessionName);

                        serializationService.serialize(sharedSession, resp.getOutputStream());
                        break;
                    default:
                        throw new ServletException("Unknown operation: " + operation);
                }
            } else {
                Object response = remoteService.processMessage(
                        serializationService.deserialize(req.getInputStream(), ClientMessage.class));

                serializationService.serialize(response, resp.getOutputStream());
            }
        } catch (RuntimeException | ServletException e) {
            throw e;
        } catch (Throwable e) {
            throw new ServletException(e);
        } finally {
            ROPRequestContext.end();
        }
    }
}
