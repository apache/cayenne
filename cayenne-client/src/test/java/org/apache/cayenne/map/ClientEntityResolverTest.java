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
package org.apache.cayenne.map;

import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class ClientEntityResolverTest extends ClientCase {

    public void testSerializabilityWithHessian() throws Exception {
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(Artist.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection<DataMap> maps = Collections.singleton(dataMap);
        EntityResolver resolver = new EntityResolver(maps);

        // 1. simple case
        Object c1 = HessianUtil.cloneViaClientServerSerialization(resolver, new EntityResolver());

        assertNotNull(c1);
        assertTrue(c1 instanceof EntityResolver);
        EntityResolver cr1 = (EntityResolver) c1;

        assertNotSame(resolver, cr1);
        assertEquals(1, cr1.getObjEntities().size());
        assertNotNull(cr1.getObjEntity(entity.getName()));

        // 2. with descriptors resolved...
        assertNotNull(resolver.getClassDescriptor(entity.getName()));

        EntityResolver cr2 = (EntityResolver) HessianUtil.cloneViaClientServerSerialization(resolver,
                new EntityResolver());
        assertNotNull(cr2);
        assertEquals(1, cr2.getObjEntities().size());
        assertNotNull(cr2.getObjEntity(entity.getName()));
        assertNotNull(cr2.getClassDescriptor(entity.getName()));
    }
}
