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
package org.apache.cayenne.configuration.rop.server;

import java.util.Collection;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.web.RequestHandler;
import org.apache.cayenne.configuration.web.WebConfiguration;
import org.apache.cayenne.configuration.web.WebUtil;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.remote.RemoteService;
import org.apache.cayenne.remote.hessian.HessianConfig;
import org.apache.cayenne.remote.hessian.service.HessianService;

import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianServlet;

/**
 * A servlet that bootstraps a Hessian-based ROP server. Servlet initialization
 * parameters:
 * <ul>
 * <li>configuration-location (optional) - a name of Cayenne configuration XML file that
 * will be used to load Cayenne stack. If missing, the servlet name will be used to derive
 * the location. ".xml" extension will be appended to the servlet name to get the
 * location, so a servlet named "cayenne-foo" will result in location "cayenne-foo.xml".
 * <li>extra-modules (optional) - a comma or space-separated list of class names, with
 * each class implementing {@link Module} interface. These are the custom modules loaded
 * after the two standard ones that allow users to override any Cayenne runtime aspects,
 * e.g. {@link RequestHandler}. Each custom module must have a no-arg constructor.
 * </ul>
 * All other parameters passed to the servlet are considered to be related to the
 * {@link EventBridge} initialization.
 * 
 * @since 3.1
 */
public class ROPHessianServlet extends HessianServlet {

    protected ServletContext servletContext;

    /**
     * Installs {@link HessianService} to respond to {@link RemoteService} requests.
     */
    @Override
    public void init(ServletConfig configuration) throws ServletException {
        
        checkAlreadyConfigured(configuration.getServletContext());

        this.servletContext = configuration.getServletContext();

        WebConfiguration configAdapter = new WebConfiguration(configuration);

        String configurationLocation = configAdapter.getConfigurationLocation();
        Map<String, String> eventBridgeParameters = configAdapter.getOtherParameters();

        Collection<Module> modules = configAdapter.createModules(new ROPServerModule(
                eventBridgeParameters));

        ServerRuntime runtime = new ServerRuntime(configurationLocation, modules
                .toArray(new Module[modules.size()]));

        DataChannel channel = runtime.getChannel();

        RemoteService service = runtime.getInjector().getInstance(RemoteService.class);

        SerializerFactory serializerFactory = HessianConfig.createFactory(
                HessianService.SERVER_SERIALIZER_FACTORIES,
                channel.getEntityResolver());

        setAPIClass(RemoteService.class);
        setSerializerFactory(serializerFactory);
        setService(service);

        // Even though runtime instance is not accessed by Hessian service directly (it
        // uses DataChannel injection instead), expose it in a manner consistent with
        // CayenneFilter. Servlets other than ROP may decide to use it...

        // TODO: andrus 04/14/2010: if CayenneFilter and ROPHessianServlet are used
        // together in the same webapp, maybe a good idea to ensure they are using the
        // same stack...Merging CayenneRuntime's modules might be tough though.

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
}
