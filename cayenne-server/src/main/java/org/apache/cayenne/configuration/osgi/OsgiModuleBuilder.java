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
package org.apache.cayenne.configuration.osgi;

import java.sql.Driver;

import org.apache.cayenne.di.Module;

/**
 * A builder of a DI module that helps to bootstrap Cayenne in OSGi environment.
 * 
 * @since 3.2
 */
public class OsgiModuleBuilder {

    private OsgiModule module;

    public static OsgiModuleBuilder forProject(Class<?> typeFromProjectBundle) {

        if (typeFromProjectBundle == null) {
            throw new NullPointerException("Null 'typeFromProjectBundle'");
        }

        return new OsgiModuleBuilder(typeFromProjectBundle);
    }

    private OsgiModuleBuilder(Class<?> typeFromProjectBundle) {
        this.module = new OsgiModule();
        module.setTypeFromProjectBundle(typeFromProjectBundle);
    }

    /**
     * Registers a JDBC driver class used by Cayenne. This is an optional piece
     * of information used by Cayenne to find JDBC driver in case of Cayenne
     * managed DataSource. E.g. don't use this when you are using a JNDI
     * DataSource.
     */
    public OsgiModuleBuilder withDriver(Class<? extends Driver> driverType) {
        return withType(driverType);
    }

    /**
     * Registers an arbitrary Java class. If Cayenne tries to load it via
     * reflection later on, it will be using ClassLoader attached to the class
     * passed to this method.
     */
    public OsgiModuleBuilder withType(Class<?> type) {
        module.putClassLoader(type.getName(), type.getClassLoader());
        return this;
    }

    /**
     * Registers an arbitrary Java class. If Cayenne tries to load it via
     * reflection later on, it will be using ClassLoader attached to the class
     * passed to this method.
     */
    public OsgiModuleBuilder withTypes(Class<?>... types) {
        if (types != null) {
            for (Class<?> type : types) {
                module.putClassLoader(type.getName(), type.getClassLoader());
            }
        }
        return this;
    }

    public Module module() {
        return module;
    }
}
