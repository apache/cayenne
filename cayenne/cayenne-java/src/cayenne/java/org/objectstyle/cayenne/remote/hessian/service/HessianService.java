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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.objectstyle.cayenne.remote.hessian.EnumSerializerProxy;
import org.objectstyle.cayenne.remote.hessian.HessianConfig;
import org.objectstyle.cayenne.remote.service.HttpRemoteService;

import com.caucho.hessian.io.SerializerFactory;
import com.caucho.services.server.Service;
import com.caucho.services.server.ServiceContext;

/**
 * An implementation of RemoteService using binary Hessian protocol. For more info on
 * Hessian see http://www.caucho.com/resin-3.0/protocols/hessian.xtp.
 * 
 * @see org.objectstyle.cayenne.remote.hessian.service.HessianServlet
 * @see org.objectstyle.cayenne.remote.RemoteService
 * @since 1.2
 * @author Andrus Adamchik
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