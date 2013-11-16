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
package org.apache.cayenne.di;

import java.util.Collection;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.di.spi.DefaultInjector;

/**
 * A class that bootstraps the Cayenne DI container.
 * 
 * @since 3.1
 */
public class DIBootstrap {

    /**
     * Creates and returns an injector instance working with the set of provided modules.
     */
    public static Injector createInjector(Module... modules)
            throws ConfigurationException {
        return new DefaultInjector(modules);
    }

    /**
     * Creates and returns an injector instance working with the set of provided modules.
     */
    public static Injector createInjector(Collection<Module> modules) {
        Module[] moduleArray = modules.toArray(new Module[modules.size()]);
        return createInjector(moduleArray);
    }
}
