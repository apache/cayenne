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

package org.apache.cayenne.unit.jira;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.unit.PeopleCase;

/**
 */
public class CAY_207Test extends PeopleCase {

    protected DataMap testMap;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testCAY_207Super1() throws Exception {
        createTestData("testCAY_207");
        DataContext context = createDataContext();

        prepare();

        try {
            // M1
//            Manager o1 = DataObjectUtils.objectForPK(context, Manager.class, 1);
//            assertTrue(o1 instanceof CAY_207Manager1);
//
//            Object p1 = o1.readProperty("clientContactType");
//            assertNotNull(p1);
//
//            assertTrue(
//                    "Invalid property class: " + p1.getClass().getName(),
//                    p1 instanceof CAY_207String1);
        }
        finally {
            cleanup(context);
        }
    }

    public void testCAY_207Super2() throws Exception {
        createTestData("testCAY_207");
        DataContext context = createDataContext();

        prepare();

        try {
//            Manager o2 = DataObjectUtils.objectForPK(context, Manager.class, 2);
//            assertTrue(o2 instanceof CAY_207Manager2);
//
//            Object p2 = o2.readProperty("clientContactType");
//            assertNotNull(p2);
//            assertTrue(
//                    "Invalid property class: " + p2.getClass().getName(),
//                    p2 instanceof CAY_207String2);
        }
        finally {
            cleanup(context);
        }
    }

    public void testCAY_207Subclass1() throws Exception {
        createTestData("testCAY_207");
        DataContext context = createDataContext();

        prepare();

        try {
            // M1
//            Manager o1 = DataObjectUtils.objectForPK(context, CAY_207Manager1.class, 1);
//            assertTrue(o1 instanceof CAY_207Manager1);
//
//            Object p1 = o1.readProperty("clientContactType");
//            assertNotNull(p1);
//
//            assertTrue(
//                    "Invalid property class: " + p1.getClass().getName(),
//                    p1 instanceof CAY_207String1);
        }
        finally {
            cleanup(context);
        }
    }

    public void testCAY_207Subclass2() throws Exception {
        createTestData("testCAY_207");
        DataContext context = createDataContext();

        prepare();

        try {
//            Manager o2 = DataObjectUtils.objectForPK(context, CAY_207Manager2.class, 2);
//            assertTrue(o2 instanceof CAY_207Manager2);
//
//            Object p2 = o2.readProperty("clientContactType");
//            assertNotNull(p2);
//
//            assertTrue(
//                    "Invalid property class: " + p2.getClass().getName(),
//                    p2 instanceof CAY_207String2);
        }
        finally {
            cleanup(context);
        }
    }

    public void testCAY_207Save() throws Exception {
        DataContext context = createDataContext();

        prepare();

        try {
//            CAY_207Manager2 o2 = context.newObject(CAY_207Manager2.class);
//            o2.setPersonType("M2");
//            o2.setName("aaaa");
//            o2.setClientContactType(new CAY_207String1("T1:AAAAA"));
//
//            // should succeed...
//            context.commitChanges();
//
//            int pk = DataObjectUtils.intPKForObject(o2);
//            String query = "SELECT #result('CLIENT_CONTACT_TYPE' 'String' 'CLIENT_CONTACT_TYPE') "
//                    + "FROM PERSON WHERE PERSON_ID = "
//                    + pk;
//            SQLTemplate template = new SQLTemplate(CAY_207Manager2.class, query);
//            template.setFetchingDataRows(true);
//            List rows = context.performQuery(template);
//            assertEquals(1, rows.size());
//
//            Map map = (Map) rows.get(0);
//            assertEquals("T1:AAAAA", map.get("CLIENT_CONTACT_TYPE"));
        }
        finally {
            cleanup(context);
        }
    }

    protected void prepare() {

        prepareDataMap();

        DataDomain domain = getDomain();
        DataNode node = domain.lookupDataNode(domain.getMap("people"));
        domain.removeDataNode(node.getName());

        node.addDataMap(testMap);
        domain.addNode(node);

        ExtendedTypeMap map = node.getAdapter().getExtendedTypes();
        map.registerType(new CAY_207StringType1());
        map.registerType(new CAY_207StringType2());
    }

    protected void cleanup(DataContext context) {
        DataDomain domain = getDomain();
        domain.removeMap(testMap.getName());
        DataNode node = domain.lookupDataNode(domain.getMap("people"));

        ExtendedTypeMap map = node.getAdapter().getExtendedTypes();
        map.unregisterType(CAY_207String1.class.getName());
        map.unregisterType(CAY_207String2.class.getName());
    }

    /**
     * Overrides super implementation to add a few extra entities to this DataContext
     * without affecting others.
     */
    protected void prepareDataMap() {
        if (testMap == null) {
            DataDomain domain = getDomain();
            ObjEntity manager = domain.getEntityResolver().lookupObjEntity(Manager.class);

            ObjEntity m1 = new ObjEntity("Manager1");
            m1.setSuperEntityName(manager.getName());
            m1.setDeclaredQualifier(Expression.fromString("personType = \"M1\""));
            m1.setClassName(CAY_207Manager1.class.getName());
            ObjAttribute ma1 = new ObjAttribute("clientContactType");
            ma1.setDbAttributePath("CLIENT_CONTACT_TYPE");
            ma1.setType(CAY_207String1.class.getName());
            ma1.setEntity(m1);
            m1.addAttribute(ma1);

            ObjEntity m2 = new ObjEntity("Manager2");
            m2.setSuperEntityName(manager.getName());
            m2.setDeclaredQualifier(Expression.fromString("personType = \"M2\""));
            m2.setClassName(CAY_207Manager2.class.getName());
            ObjAttribute ma2 = new ObjAttribute("clientContactType");
            ma2.setDbAttributePath("CLIENT_CONTACT_TYPE");
            ma2.setType(CAY_207String2.class.getName());
            ma2.setEntity(m2);
            m2.addAttribute(ma2);

            testMap = new DataMap("CAY-207");
            testMap.addObjEntity(m1);
            testMap.addObjEntity(m2);
        }
    }
}
