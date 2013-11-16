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

import junit.framework.TestCase;

import org.apache.cayenne.remote.hessian.service.HessianUtil;

public class DbEntityTest extends TestCase {

    public void testSerializabilityWithHessian() throws Exception {
        DbEntity entity = new DbEntity("entity");

        DbAttribute pk = new DbAttribute("pk");
        pk.setPrimaryKey(true);
        entity.addAttribute(pk);

        DbAttribute generated = new DbAttribute("generated");
        generated.setGenerated(true);
        entity.addAttribute(generated);

        DbEntity d2 = (DbEntity) HessianUtil.cloneViaClientServerSerialization(entity, new EntityResolver());

        assertNotNull(d2.getPrimaryKeys());
        assertEquals(entity.getPrimaryKeys().size(), d2.getPrimaryKeys().size());

        DbAttribute pk2 = d2.getAttribute(pk.getName());
        assertNotNull(pk2);
        assertTrue(d2.getPrimaryKeys().contains(pk2));

        assertNotNull(d2.getGeneratedAttributes());
        assertEquals(entity.getGeneratedAttributes().size(), d2.getGeneratedAttributes().size());

        DbAttribute generated2 = d2.getAttribute(generated.getName());
        assertNotNull(generated2);
        assertTrue(d2.getGeneratedAttributes().contains(generated2));
    }
}
