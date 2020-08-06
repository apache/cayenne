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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1Subclass1;
import org.apache.cayenne.testdo.mt.MtTable1Subclass1;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
@RunWith(value=Parameterized.class)
public class ValueInjectorIT extends RemoteCayenneCase {

    @Inject
    protected DataContext serverContext;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {LocalConnection.HESSIAN_SERIALIZATION},
                {LocalConnection.JAVA_SERIALIZATION},
                {LocalConnection.NO_SERIALIZATION},
        });
    }

    public ValueInjectorIT(int serializationPolicy) {
        super.serializationPolicy = serializationPolicy;
    }

    @Test
    public void testServer() {
        ObjEntity entity = serverContext.getEntityResolver().getObjEntity(MtTable1Subclass1.class);
        Expression qualifier = entity.getDeclaredQualifier();

        try {
            MtTable1Subclass1 ee = serverContext.newObject(MtTable1Subclass1.class);
            assertEquals(ee.getGlobalAttribute1(), "sub1");

            // check AND
            entity.setDeclaredQualifier(qualifier.andExp(ExpressionFactory.exp("serverAttribute1 = 'sa'")));
            ee = serverContext.newObject(MtTable1Subclass1.class);
            assertEquals(ee.getGlobalAttribute1(), "sub1");
            assertEquals(ee.getServerAttribute1(), "sa");
        } finally {
            entity.setDeclaredQualifier(qualifier);
        }
    }

    @Test
    public void testClient() {
        ObjectContext context = createROPContext();
        ObjEntity entity = context.getEntityResolver().getObjEntity(ClientMtTable1Subclass1.class);
        Expression qualifier = entity.getDeclaredQualifier();

        try {
            ClientMtTable1Subclass1 ee = context.newObject(ClientMtTable1Subclass1.class);
            assertEquals(ee.getGlobalAttribute1(), "sub1");

            // check AND
            entity.setDeclaredQualifier(qualifier.andExp(ExpressionFactory.exp("serverAttribute1 = 'sa'")));
            ee = context.newObject(ClientMtTable1Subclass1.class);
            assertEquals(ee.getGlobalAttribute1(), "sub1");
            assertEquals(ee.getServerAttribute1(), "sa");
        } finally {
            entity.setDeclaredQualifier(qualifier);
        }
    }
}
