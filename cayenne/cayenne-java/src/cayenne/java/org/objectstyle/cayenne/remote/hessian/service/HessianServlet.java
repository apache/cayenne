/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.remote.hessian.service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.objectstyle.cayenne.remote.RemoteService;

/**
 * An extension of the <code>com.caucho.hessian.server.HessianServlet</code> that
 * installs default Cayenne handlers, simplifying <code>web.xml</code> configuration.
 * Here is a sample configuration:
 * 
 * <pre>
 *        &lt;servlet&gt;
 *          &lt;servlet-name&gt;cayenne&lt;/servlet-name&gt;
 *          &lt;servlet-class&gt;org.objectstyle.cayenne.remote.hessian.service.HessianServlet&lt;/servlet-class&gt;
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
 * @author Andrus Adamchik
 */
public class HessianServlet extends _HessianServlet {

    // config parameters compatible with Hessian parameter names
    static final String API_CLASS_PARAMETER = "api-class";
    static final String SERVICE_CLASS_PARAMETER = "service-class";

    /**
     * Installs {@link HessianService} to respond to {@link RemoteService} requests.
     */
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

        // proceed to super
        super.init(config);
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
