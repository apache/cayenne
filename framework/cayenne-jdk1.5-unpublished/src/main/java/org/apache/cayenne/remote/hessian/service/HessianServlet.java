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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.cayenne.remote.RemoteService;

/**
 * An extension of the <code>com.caucho.hessian.server.HessianServlet</code> that
 * installs default Cayenne handlers, simplifying <code>web.xml</code> configuration.
 * Here is a sample configuration:
 * 
 * <pre>
 *        &lt;servlet&gt;
 *          &lt;servlet-name&gt;cayenne&lt;/servlet-name&gt;
 *          &lt;servlet-class&gt;org.apache.cayenne.remote.hessian.service.HessianServlet&lt;/servlet-class&gt;
 *        &lt;/servlet&gt;
 *                        
 *        &lt;servlet-mapping&gt;
 *          &lt;servlet-name&gt;cayenne&lt;/servlet-name&gt;
 *          &lt;url-pattern&gt;/cayenne&lt;/url-pattern&gt;
 *        &lt;/servlet-mapping&gt;
 * </pre>
 * 
 * Custom service class and interface can be specified in a manner compatible with Hessian
 * recommendations, namely via <em>service-class</em> and <em>api-class</em> servlet
 * parameters.
 * 
 * @since 1.2
 */
public class HessianServlet extends com.caucho.hessian.server.HessianServlet {

    // config parameters compatible with Hessian parameter names
    static final String API_CLASS_PARAMETER = "api-class";
    static final String SERVICE_CLASS_PARAMETER = "service-class";
    
    private HessianService service;

    /**
     * Installs {@link HessianService} to respond to {@link RemoteService} requests.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {

        Class apiClass = createAPIClass(config);
        if (apiClass == null) {
            throw new ServletException("Can't configure service API class");
        }

        setAPIClass(apiClass);

        HessianService service = createService(config);
        if (service == null) {
            throw new ServletException("Error configuring service ");
        }

        service.init(config);
        setSerializerFactory(service.createSerializerFactory());
        setService(service);
        
      
        
        // store service in ServletContext to be able to shut it down on destroy
        this.service = service;
        
        // proceed to super
        super.init(config);
    }
    
    @Override
    public void destroy() {
        if (service != null) {
            service.destroy();
            service = null;
        }

        super.destroy();
    }

    protected HessianService createService(ServletConfig config) throws ServletException {

        String className = config.getInitParameter(SERVICE_CLASS_PARAMETER);
        if (className == null) {
            return new HessianService();
        }

        try {
            Class serviceClass = Class.forName(className, true, Thread
                    .currentThread()
                    .getContextClassLoader());

            if (!HessianService.class.isAssignableFrom(serviceClass)) {
                throw new ServletException(
                        "Service class must be a subclass of HessianService: "
                                + className);
            }

            return (HessianService) serviceClass.newInstance();
        }
        catch (ServletException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServletException(
                    "Error instantiating service class " + className,
                    e);
        }
    }

    protected Class createAPIClass(ServletConfig config) throws ServletException {
        String interfaceName = config.getInitParameter(API_CLASS_PARAMETER);
        if (interfaceName == null) {
            return RemoteService.class;
        }
        try {
            Class serviceInterface = Class.forName(interfaceName, true, Thread
                    .currentThread()
                    .getContextClassLoader());

            if (!RemoteService.class.isAssignableFrom(serviceInterface)) {
                throw new ServletException(
                        "Service interface must be a subinterface of RemoteService: "
                                + interfaceName);
            }

            return serviceInterface;
        }
        catch (ServletException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServletException("Error instantiating service interface "
                    + interfaceName, e);
        }
    }
}
