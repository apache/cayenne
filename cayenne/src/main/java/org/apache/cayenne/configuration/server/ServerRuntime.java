/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.configuration.server;

import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.runtime.CayenneRuntimeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Object representing Cayenne stack. Serves as an entry point to Cayenne for user applications and a factory of ObjectContexts.
 * Implementation is a thin wrapper of the dependency injection container.
 * <p>The "Server" prefix in the name is in contrast to ROP "client" (that is started via ClientRuntime). So
 * ServerRuntime is the default Cayenne stack that you should be using in all apps with the exception of client-side ROP.</p>
 *
 * @since 3.1
 * @deprecated since 5.0, use {@link CayenneRuntime} class instead
 */
@Deprecated(since = "5.0", forRemoval = true)
public class ServerRuntime extends CayenneRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerRuntime.class);

    /**
     * Creates a builder of CayenneRuntime.
     *
     * @return a builder of CayenneRuntime.
     * @since 4.0
     * @deprecated since 5.0, use {@link CayenneRuntime#builder()} instead
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public static CayenneRuntimeBuilder builder() {
        LOGGER.warn("ServerRuntime is deprecated, use CayenneRuntime instead");
        return CayenneRuntime.builder();
    }

    /**
     * Creates a builder of CayenneRuntime.
     *
     * @param name optional symbolic name of the created runtime.
     * @return a named builder of CayenneRuntime.
     * @deprecated since 5.0, use {@link CayenneRuntime#builder(String)} instead
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public static CayenneRuntimeBuilder builder(String name) {
        LOGGER.warn("ServerRuntime is deprecated, use CayenneRuntime instead");
        return CayenneRuntime.builder(name);
    }

    private ServerRuntime() {
        super(Collections.emptyList());
    }
}
