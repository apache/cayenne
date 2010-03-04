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
package org.apache.cayenne.resource;

import java.io.Serializable;
import java.net.URL;

/**
 * An abstraction of a resource whose data can be accessed via a URL.
 * 
 * @since 3.1
 */
public interface Resource extends Serializable {

    /**
     * Returns a resource URL to read (and possibly write) the resource data.
     */
    URL getURL();

    /**
     * Returns a resource resolved relatively to the current resource. E.g. DataMap files
     * can be resolved relatively to cayenne.xml.
     */
    Resource getRelativeResource(String relativePath);
}
