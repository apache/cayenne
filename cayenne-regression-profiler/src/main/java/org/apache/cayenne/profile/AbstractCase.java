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
package org.apache.cayenne.profile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.objectstyle.cayenne.access.DataContext;

/**
 * An noop profiling case.
 * 
 * @author Andrus Adamchik
 */
public class AbstractCase extends TestCase {

    public void doGet(
            DataContext context,
            HttpServletRequest request,
            HttpServletResponse response) {

        doRequest(context, request, response);
    }

    public void doPost(
            DataContext context,
            HttpServletRequest request,
            HttpServletResponse response) {
        doRequest(context, request, response);
    }

    protected void doRequest(
            DataContext context,
            HttpServletRequest request,
            HttpServletResponse response) {

    }
}
