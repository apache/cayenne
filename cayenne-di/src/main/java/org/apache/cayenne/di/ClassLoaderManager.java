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

/**
 * Maps ClassLoaders to resources. This is a useful abstraction when switching
 * between environments. E.g. between JEE with thread/hierarchical classloaders
 * and OSGi with per-bundle classloaders.
 * 
 * @since 3.2
 */
public interface ClassLoaderManager {

    /**
     * Returns a ClassLoader appropriate for loading a given resource. Resource
     * path should be compatible with Class.getResource(..) and such, i.e. the
     * path component separator should be slash, not dot.
     */
    ClassLoader getClassLoader(String resourceName);
}
