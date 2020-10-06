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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class DataMapUtilsTest {

    protected DataMapUtils dataMapUtils = null;
    protected ObjEntity objEntity = null;

    @Before
    public void setUp() {
        dataMapUtils = new DataMapUtils();
        objEntity = new ObjEntity();
    }

    @After
    public void tearDown() {
        dataMapUtils = null;
        objEntity = null;
    }

    @Test
    public void testGetParameterNamesWithFilledQueriesMap() {

        String param = "param";
        String qualifierString = "name = $" + param;

        SelectQueryDescriptor selectQueryDescriptor = new SelectQueryDescriptor();

        Set<String> result = new LinkedHashSet<>();
        assertEquals(result, dataMapUtils.getParameterNames(selectQueryDescriptor));

        Expression exp = ExpressionFactory.exp(qualifierString);
        selectQueryDescriptor.setQualifier(exp);
        selectQueryDescriptor.setName("name");

        Map<String, String> map = new HashMap<>();
        map.put(param, "java.lang.String");

        dataMapUtils.queriesMap.put("name", map);
        Collection collection = dataMapUtils.getParameterNames(selectQueryDescriptor);

        result.add(param);

        assertEquals(collection, result);
    }

    @Test
    public void testGetParameterNamesWithEmptyQueriesMap() {

        DbEntity dbEntity = new DbEntity("test");
        ObjAttribute attribute = new ObjAttribute("name");
        attribute.setDbAttributePath("testKey");
        attribute.setType("java.lang.String");
        objEntity.addAttribute(attribute);
        objEntity.setName("test");
        objEntity.setDbEntity(dbEntity);

        String param = "param";
        String qualifierString = "name = $" + param;

        SelectQueryDescriptor selectQueryDescriptor = new SelectQueryDescriptor();
        Expression exp = ExpressionFactory.exp(qualifierString);
        selectQueryDescriptor.setQualifier(exp);
        selectQueryDescriptor.setName("name");
        selectQueryDescriptor.setRoot(objEntity);

        Collection collection = dataMapUtils.getParameterNames(selectQueryDescriptor);

        Map<String, Map<String, String>> queriesMap = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        map.put(param, "java.lang.String");
        queriesMap.put("name", map);

        assertEquals(dataMapUtils.queriesMap, queriesMap);

        Set<String> result = new LinkedHashSet<>();
        result.add(param);

        assertEquals(collection, result);
    }
}
