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
package org.apache.cayenne.configuration;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import junit.framework.TestCase;

import org.apache.cayenne.unit.JNDISetup;

import com.mockrunner.mock.jdbc.MockDataSource;

public class JNDIDataSourceFactoryTest extends TestCase {

    public void testGetDataSource_NameBound() throws Exception {

        DataNodeDescriptor descriptor = new DataNodeDescriptor();
        descriptor.setLocation("jdbc/TestDS");

        JNDISetup.doSetup();

        MockDataSource dataSource = new MockDataSource();
        InitialContext context = new InitialContext();
        context.bind(descriptor.getLocation(), dataSource);

        try {

            JNDIDataSourceFactory factory = new JNDIDataSourceFactory();
            assertSame(dataSource, factory.getDataSource(descriptor));
        }
        finally {
            // since the context is shared, must clear it after the test
            context.unbind(descriptor.getLocation());
        }
    }

    public void testGetDataSource_NameBoundWithPrefix() throws Exception {

        DataNodeDescriptor descriptor = new DataNodeDescriptor();
        descriptor.setLocation("jdbc/TestDS");

        JNDISetup.doSetup();

        MockDataSource dataSource = new MockDataSource();
        InitialContext context = new InitialContext();
        context.bind("java:comp/env/" + descriptor.getLocation(), dataSource);

        try {

            JNDIDataSourceFactory factory = new JNDIDataSourceFactory();
            assertSame(dataSource, factory.getDataSource(descriptor));
        }
        finally {
            // since the context is shared, must clear it after the test
            context.unbind("java:comp/env/" + descriptor.getLocation());
        }
    }

    public void testGetDataSource_NameNotBound() throws Exception {

        DataNodeDescriptor descriptor = new DataNodeDescriptor();
        descriptor.setLocation("jdbc/TestDS");

        JNDISetup.doSetup();

        JNDIDataSourceFactory factory = new JNDIDataSourceFactory();

        try {
            factory.getDataSource(descriptor);
            fail("Didn't throw on unbound name");
        }
        catch (NameNotFoundException e) {
            // expected
        }
    }
}
