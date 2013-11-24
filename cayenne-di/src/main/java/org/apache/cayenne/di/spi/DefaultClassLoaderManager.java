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
package org.apache.cayenne.di.spi;

import org.apache.cayenne.di.ClassLoaderManager;

/**
 * A {@link ClassLoaderManager} that
 * 
 * @since 3.2
 */
public class DefaultClassLoaderManager implements ClassLoaderManager {

    @Override
    public ClassLoader getClassLoader(String resourceName) {
        // here we are ignoring 'className' when looking for ClassLoader...
        // other implementations (such as OSGi) may actually use it

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = DefaultClassLoaderManager.class.getClassLoader();
        }

        // this is too paranoid I guess... "this" class will always have a
        // ClassLoader
        if (classLoader == null) {
            throw new IllegalStateException("Can't find a ClassLoader");
        }

        return classLoader;
    }

}
