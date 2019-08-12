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
package org.apache.cayenne.remote;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.BaseContext;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
@RunWith(value=Parameterized.class)
public class NestedObjectContextLocalIT extends RemoteCayenneCase {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {LocalConnection.HESSIAN_SERIALIZATION},
                {LocalConnection.JAVA_SERIALIZATION},
                {LocalConnection.NO_SERIALIZATION},
        });
    }

    public NestedObjectContextLocalIT(int serializationPolicy) {
        super.serializationPolicy = serializationPolicy;
    }

    @Inject
    private ClientRuntime runtime;

    @Test
    public void testLocalCacheStaysLocal() {

        ObjectSelect<ClientMtTable1> query = ObjectSelect
                .query(ClientMtTable1.class)
                .cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        BaseContext child1 = (BaseContext) runtime.newContext(clientContext);

        assertNull(child1.getQueryCache().get(
                query.getMetaData(child1.getEntityResolver())));

        assertNull(clientContext.getQueryCache().get(
                query.getMetaData(clientContext.getEntityResolver())));

        List<?> results = child1.performQuery(query);
        assertSame(results, child1.getQueryCache().get(
                query.getMetaData(child1.getEntityResolver())));

        assertNull(clientContext.getQueryCache().get(
                query.getMetaData(clientContext.getEntityResolver())));
    }
}
