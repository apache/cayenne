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
package org.apache.cayenne.dba;

import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.log.Slf4jJdbcEventLogger;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class PerAdapterProviderTest {

    private OracleAdapter oracleAdapter;
    private DerbyAdapter derbyAdapter;
    private AutoAdapter autoDerbyAdapter;

    @Before
    public void before() {

        ResourceLocator locator = new ClassLoaderResourceLocator(new DefaultClassLoaderManager());
        RuntimeProperties runtimeProperties = mock(RuntimeProperties.class);
        ValueObjectTypeRegistry valueObjectTypeRegistry = mock(ValueObjectTypeRegistry.class);

        this.oracleAdapter = new OracleAdapter(runtimeProperties,
                Collections.<ExtendedType>emptyList(),
                Collections.<ExtendedType>emptyList(),
                Collections.<ExtendedTypeFactory>emptyList(),
                locator, valueObjectTypeRegistry);

        this.derbyAdapter = new DerbyAdapter(runtimeProperties,
                Collections.<ExtendedType>emptyList(),
                Collections.<ExtendedType>emptyList(),
                Collections.<ExtendedTypeFactory>emptyList(),
                locator, valueObjectTypeRegistry);

        this.autoDerbyAdapter = new AutoAdapter(new Provider<DbAdapter>() {
            @Override
            public DbAdapter get() throws DIRuntimeException {
                return derbyAdapter;
            }
        }, new Slf4jJdbcEventLogger(runtimeProperties));
    }

    @Test
    public void testGet() {

        Map<String, String> map = Collections.singletonMap(DerbyAdapter.class.getName(), "x");
        PerAdapterProvider<String> provider = new PerAdapterProvider<>(map, "default");

        assertEquals("default", provider.get(oracleAdapter));
        assertEquals("x", provider.get(derbyAdapter));
        assertEquals("x", provider.get(autoDerbyAdapter));
    }
}
