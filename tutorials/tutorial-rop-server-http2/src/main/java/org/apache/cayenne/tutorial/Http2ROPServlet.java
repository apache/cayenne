/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.tutorial;

import org.apache.cayenne.configuration.rop.client.ProtostuffModule;
import org.apache.cayenne.configuration.rop.server.ROPServerModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.web.WebConfiguration;
import org.apache.cayenne.configuration.web.WebUtil;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.java8.CayenneJava8Module;
import org.apache.cayenne.remote.RemoteService;
import org.apache.cayenne.rop.ROPSerializationService;
import org.apache.cayenne.rop.ROPServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.util.Collection;
import java.util.Map;

public class Http2ROPServlet extends ROPServlet {

    @Override
    public void init(ServletConfig configuration) throws ServletException {

        checkAlreadyConfigured(configuration.getServletContext());

        this.servletContext = configuration.getServletContext();

        WebConfiguration configAdapter = new WebConfiguration(configuration);

        String configurationLocation = configAdapter.getConfigurationLocation();
        Map<String, String> eventBridgeParameters = configAdapter.getOtherParameters();

        Collection<Module> modules = configAdapter.createModules(
                new ROPServerModule(eventBridgeParameters),
                new ProtostuffModule(),
                new CayenneJava8Module());

        ServerRuntime runtime = new ServerRuntime(configurationLocation, modules
                .toArray(new Module[modules.size()]));

        this.remoteService = runtime.getInjector().getInstance(RemoteService.class);
        this.serializationService = runtime.getInjector().getInstance(ROPSerializationService.class);

        WebUtil.setCayenneRuntime(servletContext, runtime);
    }

}
