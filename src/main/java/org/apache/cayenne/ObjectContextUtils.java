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
package org.apache.cayenne;

import org.apache.cayenne.conf.Configuration;

/**
 * Provides static methods for {@link ObjectContext} creation. Context settings are
 * extracted behind the scenes from the shared {@link Configuration} instance. Some can be
 * overridden with custom properties on the fly.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
// note - this is sort of like javax.persistence.Persistence class with Cayenne flavor,
// with support for explicit thread binding of context, nested contexts, etc.
public class ObjectContextUtils {

    private static final ThreadLocal threadContext = new ThreadLocal();

    public static ObjectContext createContext() {
        return null;
    }

    public static ObjectContext getThreadContext() {
        return null;
    }

    /**
     * Binds an ObjectContext to the current thread. ObjectContext can later be retrieved
     * within the same thread by calling {@link ObjectContextUtils#getThreadContext()}.
     * Using null parameter will unbind currently bound context and should be used to
     * clean up the thread state.
     */
    public static void setThreadContext(ObjectContext context) {
        threadContext.set(context);
    }
}
