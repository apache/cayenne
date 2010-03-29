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
package org.apache.cayenne.conf;

import java.net.URL;
import java.util.Collection;

/**
 * An object that defines a strategy for locating stream resources.
 * 
 * @since 3.0
 */
public interface ResourceFinder {

    /**
     * Returns a collection of resource URLs with a given name found in the environment
     * using some lookup strategy.
     */
    Collection<URL> getResources(String name);

    /**
     * Returns a single resource matching a given name. If more than one resource matches
     * the name, it is implementation specific which one will be returned.
     */
    URL getResource(String name);
}
