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

import javax.servlet.ServletContext;

import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.configuration.rop.server.ROPHessianServlet;

/**
 * A helper class to retrieve and store {@link CayenneRuntime} in the
 * {@link ServletContext}. All Cayenne web configuration objects, such as
 * {@link CayenneFilter} and {@link ROPHessianServlet}, are using this class to access
 * runtime.
 * 
 * @since 3.1
 */
public class WebUtil {

    static final String CAYENNE_RUNTIME_KEY = WebUtil.class.getName()
            + ".CAYENNE_RUNTIME";

    /**
     * Retrieves CayenneRuntime previously stored in provided context via
     * {@link #setCayenneRuntime(ServletContext, CayenneRuntime)}. May return null if no
     * runtime was stored.
     */
    public static CayenneRuntime getCayenneRuntime(ServletContext context) {
        return (CayenneRuntime) context.getAttribute(CAYENNE_RUNTIME_KEY);
    }

    /**
     * Stores {@link CayenneRuntime} in the servlet context. It can be later retrieve via
     * {@link #getCayenneRuntime(ServletContext)}.
     */
    public static void setCayenneRuntime(ServletContext context, CayenneRuntime runtime) {
        context.setAttribute(CAYENNE_RUNTIME_KEY, runtime);
    }
}
