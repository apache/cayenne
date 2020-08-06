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

import org.apache.cayenne.map.CallbackDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class EntityUtilsTest {
    
    protected EntityUtils entityUtils = null;
    protected DataMap dataMap = null;
    protected ObjEntity objEntity = null;
    
    @Before
    public void setUp() throws Exception {
        dataMap = new DataMap();
        objEntity = new ObjEntity();
    }
    
    @After
    public void tearDown() throws Exception {
        dataMap = null;
        objEntity = null;
        entityUtils = null;
    }

    @Test
    public void testAllCallbackNamesUnique() throws Exception {
        
        CallbackDescriptor[] callbacks = objEntity.getCallbackMap().getCallbacks();
        for (int i = 0; i < callbacks.length; i++) {
            callbacks[i].addCallbackMethod("callback1");
            callbacks[i].addCallbackMethod("callback2");
            callbacks[i].addCallbackMethod("callback3");
        }
        entityUtils = new EntityUtils(dataMap, objEntity, "TestBaseClass", "TestSuperClass", "TestSubClass");
        
        boolean hasNoDuplicates = true;
        Set<String> callbackNames = new LinkedHashSet<String>();
        for (String cbName : entityUtils.getCallbackNames()) {
            if (!callbackNames.add(cbName)) {
                hasNoDuplicates = false;
            }
        }
        
        assertTrue("Contains duplicate callback names.", hasNoDuplicates);
    }

    @Test
    public void testDeclaresDbAttribute() throws Exception {

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
