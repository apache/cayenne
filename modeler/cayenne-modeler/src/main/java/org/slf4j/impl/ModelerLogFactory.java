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
package org.slf4j.impl;

import org.slf4j.ILoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating ModelerLogger instances.
 */
public class ModelerLogFactory implements ILoggerFactory {

    private static final String ignoreVelocity = "org.apache.velocity";

    private final Map<String, ModelerLogger> localCache;

    public ModelerLogFactory() {
        localCache = new HashMap<>();
    }

    @Override
    public ModelerLogger getLogger(String name) {
        ModelerLogger local = localCache.get(name);
        if (local == null) {
            local = name.contains(ignoreVelocity) ? new NoopModelerLogger(name) : new ModelerLogger(name);
            localCache.put(name, local);
        }
        return local;
    }
}
