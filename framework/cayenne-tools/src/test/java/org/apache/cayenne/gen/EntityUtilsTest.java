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
package org.apache.cayenne.gen;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cayenne.map.CallbackDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;

import junit.framework.TestCase;


public class EntityUtilsTest extends TestCase {
    
    protected EntityUtils entityUtils = null;
    protected DataMap dataMap = null;
    protected ObjEntity objEntity = null;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dataMap = new DataMap();
        objEntity = new ObjEntity();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        dataMap = null;
        objEntity = null;
        entityUtils = null;
    }
    
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
}
