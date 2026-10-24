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

package org.apache.cayenne.gen;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class EntityUtilsTest {

    protected EntityUtils entityUtils = null;
    protected DataMap dataMap = null;
    protected ObjEntity objEntity = null;

    @BeforeEach
    public void setUp() throws Exception {
        dataMap = new DataMap();
        objEntity = new ObjEntity();
    }

    @AfterEach
    public void tearDown() throws Exception {
        dataMap = null;
        objEntity = null;
        entityUtils = null;
    }

    @Test
    public void declaresDbAttribute() throws Exception {

        DbEntity dbEntity = new DbEntity("test");
        DbAttribute exists = new DbAttribute("testKey");
        DbAttribute notExists = new DbAttribute("testKey1");
        dbEntity.addAttribute(exists);

        ObjAttribute attribute = new ObjAttribute("exists");
        attribute.setDbAttributePath("testKey");
        objEntity.addAttribute(attribute);

        objEntity.setName("test");
        objEntity.setDbEntity(dbEntity);
        dataMap.addDbEntity(dbEntity);
        dataMap.addObjEntity(objEntity);

        entityUtils = new EntityUtils(dataMap, objEntity, "TestBaseClass", "TestSuperClass", "TestSubClass");

        assertTrue(entityUtils.declaresDbAttribute(exists));
        assertFalse(entityUtils.declaresDbAttribute(notExists));

    }
}
