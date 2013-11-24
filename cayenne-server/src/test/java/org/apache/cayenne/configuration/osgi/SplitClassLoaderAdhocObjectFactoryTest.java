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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import junit.framework.TestCase;

public class SplitClassLoaderAdhocObjectFactoryTest extends TestCase {

    public void testGetClassLoader() {

        final ClassLoader appCl = mock(ClassLoader.class);
        final ClassLoader diCl = mock(ClassLoader.class);
        final ClassLoader serverCl = mock(ClassLoader.class);

        OsgiEnvironment osgiEnvironment = mock(OsgiEnvironment.class);
        when(osgiEnvironment.applicationClassLoader(anyString())).thenReturn(appCl);
        when(osgiEnvironment.cayenneDiClassLoader()).thenReturn(diCl);
        when(osgiEnvironment.cayenneServerClassLoader()).thenReturn(serverCl);
        
        SplitClassLoaderAdhocObjectFactory factory = new SplitClassLoaderAdhocObjectFactory(osgiEnvironment);

        assertSame(appCl, factory.getClassLoader(null));
        assertSame(appCl, factory.getClassLoader(""));
        assertSame(appCl, factory.getClassLoader("org/example/test"));
        assertSame(appCl, factory.getClassLoader("/org/example/test"));
        assertSame(serverCl, factory.getClassLoader("/org/apache/cayenne/access/DataContext.class"));
        assertSame(diCl, factory.getClassLoader("/org/apache/cayenne/di/Injector.class"));
        assertSame(diCl, factory.getClassLoader("org/apache/cayenne/di/Injector.class"));

    }

}
