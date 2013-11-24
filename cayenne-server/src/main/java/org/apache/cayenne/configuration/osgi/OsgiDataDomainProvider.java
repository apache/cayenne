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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.DataDomainProvider;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 3.2
 */
// TODO: this is really a hack until we can have fully injectable class loading
// at the EntityResolver level per CAY-1887
public class OsgiDataDomainProvider extends DataDomainProvider {

    private OsgiEnvironment osgiEnvironment;

    public OsgiDataDomainProvider(@Inject OsgiEnvironment osgiEnvironment) {
        this.osgiEnvironment = osgiEnvironment;
    }

    @Override
    public DataDomain get() throws ConfigurationException {

        // here goes the class loading hack, temporarily setting application
        // bundle ClassLoader to be a thread ClassLoader for runtime to start.

        Thread thread = Thread.currentThread();
        ClassLoader activeCl = thread.getContextClassLoader();
        try {

            // using fake package name... may not work with all implementations
            // of osgiEnvironment?
            thread.setContextClassLoader(osgiEnvironment.applicationClassLoader("com/"));

            DataDomain domain = super.get();
            EntityResolver entityResolver = domain.getEntityResolver();
            for (ObjEntity e : entityResolver.getObjEntities()) {

                // it is not enough to just call 'getObjectClass()' on
                // ClassDescriptor - there's an optimization that prevents full
                // descriptor resolving... so calling some other method...
                entityResolver.getClassDescriptor(e.getName()).getProperty("__dummy__");
                entityResolver.getCallbackRegistry();
            }

            // this triggers callbacks initialization using thread class loader
            entityResolver.getCallbackRegistry();

            return domain;

        } finally {
            thread.setContextClassLoader(activeCl);
        }
    }

}
