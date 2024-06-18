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
package org.apache.cayenne.configuration.runtime;

import com.mockrunner.mock.jdbc.MockDataSource;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.unit.JNDISetup;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

@Deprecated
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class JNDIDataSourceFactoryIT extends RuntimeCase {
    
    @Inject
    private Injector injector;

    @Test
    public void testGetDataSource_NameBound() throws Exception {

        DataNodeDescriptor descriptor = new DataNodeDescriptor();
        descriptor.setParameters("jdbc/TestDS");

        JNDISetup.doSetup();

        MockDataSource dataSource = new MockDataSource();
        InitialContext context = new InitialContext();
        context.bind(descriptor.getParameters(), dataSource);

        try {

            JNDIDataSourceFactory factory = new JNDIDataSourceFactory();
            injector.injectMembers(factory);
            assertSame(dataSource, factory.getDataSource(descriptor));
        }
        finally {
            // since the context is shared, must clear it after the test
            context.unbind(descriptor.getParameters());
        }
    }

    @Test
    public void testGetDataSource_NameBoundWithPrefix() throws Exception {

        DataNodeDescriptor descriptor = new DataNodeDescriptor();
        descriptor.setParameters("jdbc/TestDS");

        JNDISetup.doSetup();

        MockDataSource dataSource = new MockDataSource();
        InitialContext context = new InitialContext();
        context.bind("java:comp/env/" + descriptor.getParameters(), dataSource);

        try {

            JNDIDataSourceFactory factory = new JNDIDataSourceFactory();
            injector.injectMembers(factory);
            assertSame(dataSource, factory.getDataSource(descriptor));
        }
        finally {
            // since the context is shared, must clear it after the test
            context.unbind("java:comp/env/" + descriptor.getParameters());
        }
    }

    @Test
    public void testGetDataSource_NameNotBound() throws Exception {

        DataNodeDescriptor descriptor = new DataNodeDescriptor();
        descriptor.setParameters("jdbc/TestDS");

        JNDISetup.doSetup();

        JNDIDataSourceFactory factory = new JNDIDataSourceFactory();
        injector.injectMembers(factory);

        try {
            factory.getDataSource(descriptor);
            fail("Didn't throw on unbound name");
        }
        catch (NameNotFoundException e) {
            // expected
        }
    }
}
