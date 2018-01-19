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

package org.apache.cayenne.velocity;

import org.apache.cayenne.util.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.apache.velocity.Template;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.ResourceManager;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.apache.velocity.util.ExtProperties;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

/**
 * An implementation of the Velocity ResourceManager and ResourceLoader that
 * creates templates from in-memory Strings.
 *
 * @since 1.1
 * @deprecated since 4.1 is unused
 */
@Deprecated
public class SQLTemplateResourceManager
    extends ResourceLoader
    implements ResourceManager {

    protected Map<String, Template> templateCache;

    public void initialize(RuntimeServices rs) {
        super.rsvc = rs;
        this.templateCache = new ConcurrentLinkedHashMap.Builder<String, Template>().maximumWeightedCapacity(100).build();
    }

    public void clearCache() {
        templateCache.clear();
    }

    /**
     * Returns a Velocity Resource which is a Template for the given SQL.
     */
    public Resource getResource(String resourceName, int resourceType, String encoding)
        throws ResourceNotFoundException, ParseErrorException {

        synchronized (templateCache) {
            Template resource = templateCache.get(resourceName);

            if (resource == null) {
                resource = new Template();
                resource.setRuntimeServices(rsvc);
                resource.setResourceLoader(this);
                resource.setName(resourceName);
                resource.setEncoding(encoding);
                resource.process();

                templateCache.put(resourceName, resource);
            }

            return resource;
        }
    }

    public String getLoaderNameForResource(String resourceName) {
        return getClass().getName();
    }

    @Override
    public long getLastModified(Resource resource) {
        return -1;
    }

    @Override
    public Reader getResourceReader(String source, String charset)
        throws ResourceNotFoundException {
        return new InputStreamReader(new ByteArrayInputStream(source.getBytes()));
    }

    @Override
    public void init(ExtProperties configuration) {

    }

    @Override
    public boolean isSourceModified(Resource resource) {
        return false;
    }
}
