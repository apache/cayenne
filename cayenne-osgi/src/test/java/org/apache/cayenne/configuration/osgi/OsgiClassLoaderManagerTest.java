/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.configuration.osgi;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

public class OsgiClassLoaderManagerTest {

    @Test
    public void testGetClassLoader() {

        final ClassLoader appCl = mock(ClassLoader.class);
        final ClassLoader diCl = mock(ClassLoader.class);
        final ClassLoader serverCl = mock(ClassLoader.class);

        OsgiClassLoaderManager manager = new OsgiClassLoaderManager(appCl, Collections.<String, ClassLoader> emptyMap()) {
            @Override
            protected ClassLoader cayenneDiClassLoader() {
                return diCl;
            }

            @Override
            protected ClassLoader cayenneRuntimeClassLoader() {
                return serverCl;
            }
        };

        assertSame(appCl, manager.getClassLoader(null));
        assertSame(appCl, manager.getClassLoader(""));
        assertSame(appCl, manager.getClassLoader("org/example/test"));
        assertSame(appCl, manager.getClassLoader("/org/example/test"));
        assertSame(serverCl, manager.getClassLoader("/org/apache/cayenne/access/DataContext.class"));
        assertSame(diCl, manager.getClassLoader("/org/apache/cayenne/di/Injector.class"));
        assertSame(diCl, manager.getClassLoader("org/apache/cayenne/di/Injector.class"));

    }

}
