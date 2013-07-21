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

import org.apache.cayenne.BaseContext;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;

/**
 * <p>Stateless implementation of {@link RequestHandler} that creates a new
 * {@link ObjectContext} for each request and binds it to the request thread.
 * <p>
 * This is an alternative to the session-based request handler 
 * {@link SessionContextRequestHandler} which is the default.
 * <p>
 * The request handler can be used by injecting it with a custom @{link Module}, like so:
 * 
<pre><code>
import org.apache.cayenne.configuration.web.RequestHandler;
import org.apache.cayenne.configuration.web.StatelessContextRequestHandler;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
public class AppModule implements Module {
    public void configure(Binder binder) {
        binder.bind(RequestHandler.class).to(StatelessContextRequestHandler.class);
    }
}
</code></pre>
 * 
 * @since 3.2
 */
public class StatelessContextRequestHandler implements RequestHandler {

    // using injector to lookup services instead of injecting them directly for lazy
    // startup and "late binding"
    @Inject
    private Injector injector;

    public void requestStart(ServletRequest request, ServletResponse response) {
        CayenneRuntime.bindThreadInjector(injector);
        ObjectContext context = injector.getInstance(ObjectContextFactory.class).createContext();
        BaseContext.bindThreadObjectContext(context);
    }

    public void requestEnd(ServletRequest request, ServletResponse response) {
        CayenneRuntime.bindThreadInjector(null);
        BaseContext.bindThreadObjectContext(null);
    }

}
