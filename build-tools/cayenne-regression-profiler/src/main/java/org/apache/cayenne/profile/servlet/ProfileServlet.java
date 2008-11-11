/*
 *  Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.cayenne.profile.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.profile.AbstractCase;

/**
 * A main servlet of the profiler web app. Accepts URLs like
 * "/servlet-path/nosession/CaseClass" and "/servlet-path/session/CaseClass".
 * 
 */
public class ProfileServlet extends HttpServlet {

    public static final String CASE_PACKAGE = "org.apache.cayenne.profile.cases.";

    public void init() throws ServletException {
        super.init();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        DataContext context = contextForRequest(request);
        caseForRequest(request).doGet(context, request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        DataContext context = contextForRequest(request);
        caseForRequest(request).doPost(context, request, response);
    }

    protected DataContext contextForRequest(HttpServletRequest request) {

        if(request.getPathInfo().startsWith("/session/")) {
            return DataContext.getThreadDataContext();
        }
        else {
            return DataContext.createDataContext();
        }
    }

    protected AbstractCase caseForRequest(HttpServletRequest request)
            throws ServletException {

        // everything after the first path component is the case class...
        String path = request.getPathInfo();
        int slash = path.indexOf('/', 1);

        if (slash < 0 || slash + 1 == path.length()) {
            throw new ServletException("Invalid case path: " + path);
        }

        String caseName = CASE_PACKAGE + path.substring(slash + 1);
        try {
            return (AbstractCase) Class.forName(
                    caseName,
                    true,
                    Thread.currentThread().getContextClassLoader()).newInstance();
        }
        catch (Exception e) {
            throw new ServletException("Error instantiating case '" + caseName + "'", e);
        }
    }
}
