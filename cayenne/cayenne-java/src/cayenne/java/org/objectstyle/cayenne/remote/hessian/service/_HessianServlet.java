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
/*
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package org.objectstyle.cayenne.remote.hessian.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianSkeleton;
import com.caucho.services.server.GenericService;
import com.caucho.services.server.Service;
import com.caucho.services.server.ServiceContext;

/**
 * This class is the exact replica of com.caucho.hessian.server.HessianServlet, only with
 * bug fixes annotated with comments.
 * 
 * @since 1.2
 */
class _HessianServlet extends GenericServlet {

    private Class _homeAPI;
    private Object _homeImpl;

    private Class _objectAPI;
    private Object _objectImpl;

    private HessianSkeleton _homeSkeleton;
    private HessianSkeleton _objectSkeleton;

    private SerializerFactory _serializerFactory;

    public String getServletInfo() {
        return "Hessian Servlet";
    }

    /**
     * Sets the home api.
     */
    public void setHomeAPI(Class api) {
        _homeAPI = api;
    }

    /**
     * Sets the home implementation
     */
    public void setHome(Object home) {
        _homeImpl = home;
    }

    /**
     * Sets the object api.
     */
    public void setObjectAPI(Class api) {
        _objectAPI = api;
    }

    /**
     * Sets the object implementation
     */
    public void setObject(Object object) {
        _objectImpl = object;
    }

    /**
     * Sets the service class.
     */
    public void setService(Object service) {
        setHome(service);
    }

    /**
     * Sets the api-class.
     */
    public void setAPIClass(Class api) {
        setHomeAPI(api);
    }

    /**
     * Gets the api-class.
     */
    public Class getAPIClass() {
        return _homeAPI;
    }

    /**
     * Sets the serializer factory.
     */
    public void setSerializerFactory(SerializerFactory factory) {
        _serializerFactory = factory;
    }

    /**
     * Gets the serializer factory.
     */
    public SerializerFactory getSerializerFactory() {
        if (_serializerFactory == null)
            _serializerFactory = new SerializerFactory();

        return _serializerFactory;
    }

    /**
     * Sets the serializer send collection java type.
     */
    public void setSendCollectionType(boolean sendType) {
        getSerializerFactory().setSendCollectionType(sendType);
    }

    /**
     * Initialize the service, including the service object.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            if (_homeImpl != null) {
            }
            else if (getInitParameter("home-class") != null) {
                String className = getInitParameter("home-class");

                Class homeClass = loadClass(className);

                _homeImpl = homeClass.newInstance();

                init(_homeImpl);
            }
            else if (getInitParameter("service-class") != null) {
                String className = getInitParameter("service-class");

                Class homeClass = loadClass(className);

                _homeImpl = homeClass.newInstance();

                init(_homeImpl);
            }
            else {
                if (getClass().equals(HessianServlet.class))
                    throw new ServletException("server must extend HessianServlet");

                _homeImpl = this;
            }

            if (_homeAPI != null) {
            }
            else if (getInitParameter("home-api") != null) {
                String className = getInitParameter("home-api");

                _homeAPI = loadClass(className);
            }
            else if (getInitParameter("api-class") != null) {
                String className = getInitParameter("api-class");

                _homeAPI = loadClass(className);
            }
            else if (_homeImpl != null) {
                _homeAPI = findRemoteAPI(_homeImpl.getClass());

                if (_homeAPI == null)
                    _homeAPI = _homeImpl.getClass();
            }

            if (_objectImpl != null) {
            }
            else if (getInitParameter("object-class") != null) {
                String className = getInitParameter("object-class");

                Class objectClass = loadClass(className);

                _objectImpl = objectClass.newInstance();

                init(_objectImpl);
            }

            if (_objectAPI != null) {
            }
            else if (getInitParameter("object-api") != null) {
                String className = getInitParameter("object-api");

                _objectAPI = loadClass(className);
            }
            else if (_objectImpl != null)
                _objectAPI = _objectImpl.getClass();

            _homeSkeleton = new HessianSkeleton(_homeImpl, _homeAPI);
            if (_objectAPI != null)
                _homeSkeleton.setObjectClass(_objectAPI);

            if (_objectImpl != null) {
                _objectSkeleton = new HessianSkeleton(_objectImpl, _objectAPI);
                _objectSkeleton.setHomeClass(_homeAPI);
            }
            else
                _objectSkeleton = _homeSkeleton;
        }
        catch (ServletException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private Class findRemoteAPI(Class implClass) {
        if (implClass == null || implClass.equals(GenericService.class))
            return null;

        Class[] interfaces = implClass.getInterfaces();

        if (interfaces.length == 1)
            return interfaces[0];

        return findRemoteAPI(implClass.getSuperclass());
    }

    private Class loadClass(String className) throws ClassNotFoundException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        if (loader != null)
            return Class.forName(className, false, loader);
        else
            return Class.forName(className);
    }

    private void init(Object service) throws ServletException {
        if (service instanceof Service)
            ((Service) service).init(getServletConfig());
        else if (service instanceof Servlet)
            ((Servlet) service).init(getServletConfig());
    }

    /**
     * Execute a request. The path-info of the request selects the bean. Once the bean's
     * selected, it will be applied.
     */
    public void service(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (!req.getMethod().equals("POST")) {
            res.setStatus(500);
            PrintWriter out = res.getWriter();

            res.setContentType("text/html");
            out.println("<h1>Hessian Requires POST</h1>");

            return;
        }

        String serviceId = req.getPathInfo();
        String objectId = req.getParameter("id");
        if (objectId == null)
            objectId = req.getParameter("ejbid");

        ServiceContext.begin(req, serviceId, objectId);

        try {
            InputStream is = request.getInputStream();
            OutputStream os = response.getOutputStream();

            // ***** Hessian 3.0.13 bug: the following line was missing.
            HessianInput in = new HessianInput();
            in.setSerializerFactory(getSerializerFactory());
            in.init(is);

            HessianOutput out = new HessianOutput();
            out.setSerializerFactory(getSerializerFactory());
            out.init(os);

            if (objectId != null)
                _objectSkeleton.invoke(in, out);
            else
                _homeSkeleton.invoke(in, out);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (ServletException e) {
            throw e;
        }
        catch (Throwable e) {
            throw new ServletException(e);
        }
        finally {
            ServiceContext.end();
        }
    }
}
