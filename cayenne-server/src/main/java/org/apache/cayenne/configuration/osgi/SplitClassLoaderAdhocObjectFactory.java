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

import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;

/**
 * An OSGi-friendly {@link AdhocObjectFactory} that tries to detect the right
 * ClassLoader for any given resource based on the package name.
 * 
 * @since 3.2
 */
class SplitClassLoaderAdhocObjectFactory extends DefaultAdhocObjectFactory {

    private static final String CAYENNE_PACKAGE = "org/apache/cayenne";
    private static final String CAYENNE_DI_PACKAGE_SUFFIX = "/di";

    private OsgiEnvironment osgiEnvironment;

    SplitClassLoaderAdhocObjectFactory(@Inject OsgiEnvironment osgiEnvironment) {
        this.osgiEnvironment = osgiEnvironment;
    }

    @Override
    public ClassLoader getClassLoader(String resourceName) {

        if (resourceName == null || resourceName.length() < 2) {
            return resourceClassLoader(resourceName);
        }

        String normalizedName = resourceName.charAt(0) == '/' ? resourceName.substring(1) : resourceName;
        if (normalizedName.startsWith(CAYENNE_PACKAGE)) {

            return (normalizedName.substring(CAYENNE_PACKAGE.length()).startsWith(CAYENNE_DI_PACKAGE_SUFFIX)) ? cayenneDiClassLoader()
                    : cayenneServerClassLoader();
        }

        return resourceClassLoader(resourceName);
    }

    protected ClassLoader resourceClassLoader(String resourceName) {
        return osgiEnvironment.resourceClassLoader(resourceName);
    }

    protected ClassLoader cayenneDiClassLoader() {
        return osgiEnvironment.cayenneDiClassLoader();
    }

    protected ClassLoader cayenneServerClassLoader() {
        return osgiEnvironment.cayenneServerClassLoader();
    }

}
