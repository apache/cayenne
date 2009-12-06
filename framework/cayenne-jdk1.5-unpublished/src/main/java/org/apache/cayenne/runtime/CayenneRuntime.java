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
package org.apache.cayenne.runtime;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;

/**
 * A superclass of possible Cayenne runtime objects. A CayenneRuntime is the main access
 * point to a given Cayenne stack. It provides a default Cayenne configuration as well as
 * a way to customize this configuration via a built in dependency injection container.
 * 
 * @since 3.1
 */
public abstract class CayenneRuntime {

    protected String name;
    protected Injector injector;
    protected Module[] modules;

    /**
     * Initializes Cayenne runtime with an array of DI modules.
     */
    public CayenneRuntime(String name, Module... modules) {

        if (name == null) {
            throw new NullPointerException("Null runtime name");
        }

        if (modules == null) {
            modules = new Module[0];
        }

        this.name = name;
        this.modules = modules;
        this.injector = DIBootstrap.createInjector(modules);
    }

    /**
     * Returns runtime name. By default a configuration file name contains a runtime name
     * in it, to allow multiple runtimes in a single JVM. E.g. a typical config file name
     * has the form of "cayenne-<name>.xml".
     */
    public String getName() {
        return name;
    }

    /**
     * Returns an array of modules used to initialize this runtime.
     */
    public Module[] getModules() {
        return modules;
    }

    /**
     * Returns the runtime {@link DataChannel}.
     */
    public DataChannel getDataChannel() {
        return injector.getInstance(DataChannel.class);
    }

    /**
     * Creates and returns an ObjectContext based on the runtime DataChannel.
     */
    public ObjectContext newContext() {
        return injector.getInstance(ObjectContext.class);
    }
}
