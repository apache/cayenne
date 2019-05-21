/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.rop;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ROPRequestContext {

    private static final ThreadLocal<ROPRequestContext> localContext = new ThreadLocal<>();

    private String serviceId;
    private String objectId;
    private ServletRequest request;
    private ServletResponse response;
    private int count;

    private ROPRequestContext() {
    }

    public static void start(String serviceId, String objectId, ServletRequest request, ServletResponse response) {
        ROPRequestContext context = localContext.get();

        if (context == null) {
            context = new ROPRequestContext();
            localContext.set(context);
        }

        context.serviceId = serviceId;
        context.objectId = objectId;
        context.request = request;
        context.response = response;
        context.count++;
    }

    public static ROPRequestContext getROPRequestContext() {
        return localContext.get();
    }

    public static String getContextServiceId() {
        ROPRequestContext context = localContext.get();

        if (context != null) {
            return context.serviceId;
        } else {
            return null;
        }
    }

    public static String getContextObjectId() {
        ROPRequestContext context = localContext.get();

        if (context != null) {
            return context.objectId;
        } else {
            return null;
        }
    }

    public static ServletRequest getContextRequest() {
        ROPRequestContext context = localContext.get();

        if (context != null) {
            return context.request;
        } else {
            return null;
        }
    }

    public static ServletResponse getContextResponse() {
        ROPRequestContext context = localContext.get();

        if (context != null) {
            return context.response;
        } else {
            return null;
        }
    }

    public static void end() {
        ROPRequestContext context = localContext.get();

        if (context != null && --context.count == 0) {
            context.request = null;
            context.response = null;

            localContext.set(null);
        }
    }
}
