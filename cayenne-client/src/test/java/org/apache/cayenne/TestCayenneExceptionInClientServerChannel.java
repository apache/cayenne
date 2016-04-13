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

package org.apache.cayenne;

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;;

/**
 * @since 4.0
 */
@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class TestCayenneExceptionInClientServerChannel extends ClientCase {

    @Inject
    private ServerRuntime serverRuntime;

    @Test (expected=CayenneRuntimeException.class)
    public void testFormatterExceptionIsNotThrown() throws Exception {
        MockDataChannel parent = new MockDataChannel(new EntityResolver()) {

            @Override
            public QueryResponse onQuery(ObjectContext context, Query query) {
                return super.onQuery(context, query);
            }
        };

        DataContext context = (DataContext) serverRuntime.newContext(parent);

        Expression qualifier = ExpressionFactory.likeIgnoreCaseExp(
                MtTable1.GLOBAL_ATTRIBUTE1_PROPERTY,
                "gi%");

        ObjectSelect objectSelect = ObjectSelect.query(MtTable1.class).
                limit(2).
                where(qualifier).
                cacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        new ClientServerChannel(context).onQuery(null, objectSelect);
    }
}
