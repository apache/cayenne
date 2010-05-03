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
package org.apache.cayenne.configuration;

import java.util.Collection;

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

    protected Injector injector;
    protected Module[] modules;

    /**
     * Creates a CayenneRuntime with configuration based on the supplied array of DI
     * modules.
     */
    public CayenneRuntime(Module... modules) {

        if (modules == null) {
            modules = new Module[0];
        }

        this.modules = modules;
        this.injector = DIBootstrap.createInjector(modules);
    }

    /**
     * Creates a CayenneRuntime with configuration based on the supplied collection of DI
     * modules.
     */
    public CayenneRuntime(Collection<Module> modules) {

        if (modules == null) {
            this.modules = new Module[0];
        }
        else {
            this.modules = modules.toArray(new Module[modules.size()]);
        }

        this.injector = DIBootstrap.createInjector(this.modules);
    }

    /**
     * Returns an array of modules used to initialize this runtime.
     */
    public Module[] getModules() {
        return modules;
    }

    /**
     * Returns DI injector used by this runtime.
     */
    public Injector getInjector() {
        return injector;
    }

    /**
     * Returns the runtime {@link DataChannel}.
     */
    public DataChannel getDataChannel() {
        return injector.getInstance(DataChannel.class);
    }

    /**
     * Returns an ObjectContext based on the runtime DataChannel. Default configuration
     * will return a new instance of the ObjectContext on every call, as the corresponding
     * factory is bound using NO_SCOPE scope. Custom modules may change this behavior.
     */
    public ObjectContext getContext() {
        return injector.getInstance(ObjectContext.class);
    }
}
