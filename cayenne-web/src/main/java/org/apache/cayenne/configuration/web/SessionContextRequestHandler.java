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
package org.apache.cayenne.configuration.web;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.cayenne.BaseContext;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;

/**
 * Default implementation of the {@link RequestHandler} that stores per-user
 * {@link ObjectContext} in a web session and binds it to request thread. Note that using
 * this handler would force {@link HttpSession} creation, that may not be desirable in
 * many cases. Also session-bound context may result in a race condition with two user
 * requests updating the same persistent objects in parallel.
 * <p>
 * User applications in most cases should provide a custom RequestHandler that implements
 * a smarter app-specific strategy for providing ObjectContext.
 * <p>
 * For stateless (per request) context creation use {@link StatelessContextRequestHandler}.
 * 
 * @since 3.1
 */
public class SessionContextRequestHandler implements RequestHandler {

    static final String SESSION_CONTEXT_KEY = SessionContextRequestHandler.class
            .getName()
            + ".SESSION_CONTEXT";

    // using injector to lookup services instead of injecting them directly for lazy
    // startup and "late binding"
    @Inject
    private Injector injector;

    public void requestStart(ServletRequest request, ServletResponse response) {

        CayenneRuntime.bindThreadInjector(injector);

        if (request instanceof HttpServletRequest) {

            // this forces session creation if it does not exist yet
            HttpSession session = ((HttpServletRequest) request).getSession();

            ObjectContext context;
            synchronized (session) {
                context = (ObjectContext) session.getAttribute(SESSION_CONTEXT_KEY);

                if (context == null) {
                    context = injector
                            .getInstance(ObjectContextFactory.class)
                            .createContext();
                    session.setAttribute(SESSION_CONTEXT_KEY, context);
                }
            }

            BaseContext.bindThreadObjectContext(context);
        }
    }

    public void requestEnd(ServletRequest request, ServletResponse response) {
        CayenneRuntime.bindThreadInjector(null);
        BaseContext.bindThreadObjectContext(null);
    }

}
