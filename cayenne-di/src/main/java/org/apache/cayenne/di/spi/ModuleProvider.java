/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.di.spi;

import org.apache.cayenne.di.Module;

import java.util.Collection;
import java.util.List;

/**
 * Provider of modules used by module autoloading mechanism to identify and load modules. Multiple providers can be
 * created by inheriting from this interface and using it with {@link ModuleLoader}
 *
 * @since 4.0
 */
public interface ModuleProvider {

    Module module();

    Class<? extends Module> moduleType();

    /**
     * Returns a collection of module types this module overrides. Module autoloading mechanism will ensure module
     * load order that respects overriding preferences. The default implementation returns an empty collection.
     *
     * @return a collection of module types this module overrides.
     */
    default Collection<Class<? extends Module>> overrides() {
        return List.of();
    }
}
