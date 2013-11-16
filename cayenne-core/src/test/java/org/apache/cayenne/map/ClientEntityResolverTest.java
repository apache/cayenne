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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.MULTI_TIER_PROJECT)
public class ClientEntityResolverTest extends ServerCase {

    @Inject
    private EntityResolver serverResolver;

    public void testGetClientEntityResolver() {

        EntityResolver clientResolver = serverResolver.getClientEntityResolver();
        assertNotNull(clientResolver);

        // make sure that client entities got translated properly...

        try {
            assertNotNull(clientResolver.getObjEntity("MtTable1"));
        } catch (CayenneRuntimeException e) {
            fail("'MtTable1' entity is not mapped. All entities: " + clientResolver.getObjEntities());
        }

        assertNotNull(clientResolver.getObjEntity(ClientMtTable1.class));
        assertNull(clientResolver.getObjEntity(MtTable1.class));
    }

    public void testConstructor() {
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName("java.lang.String");
        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection<DataMap> maps = Collections.singleton(dataMap);
        EntityResolver resolver = new EntityResolver(maps);

        assertSame(entity, resolver.getObjEntity(entity.getName()));
        assertNotNull(resolver.getObjEntity(entity.getName()));
    }

    public void testInheritance() {
        ObjEntity superEntity = new ObjEntity("super_entity");
        superEntity.setClassName("java.lang.Object");

        ObjEntity subEntity = new ObjEntity("sub_entity");
        subEntity.setClassName("java.lang.String");

        subEntity.setSuperEntityName(superEntity.getName());

        try {
            subEntity.getSuperEntity();
            fail("hmm... superentity can't possibly be resolved at this point.");
        } catch (CayenneRuntimeException e) {
            // expected
        }

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(superEntity);
        dataMap.addObjEntity(subEntity);
        Collection<DataMap> maps = Collections.singleton(dataMap);
        new EntityResolver(maps);

        // after registration with resolver super entity should resolve just
        // fine
        assertSame(superEntity, subEntity.getSuperEntity());
    }
}
