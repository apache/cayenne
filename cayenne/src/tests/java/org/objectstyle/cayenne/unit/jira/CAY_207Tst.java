/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.unit.jira;

import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.testdo.inherit.Manager;
import org.objectstyle.cayenne.unit.PeopleTestCase;

/**
 * @author Andrei Adamchik
 */
public class CAY_207Tst extends PeopleTestCase {

    protected DataMap testMap;

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
            DataObject o1 = DataObjectUtils.objectForPK(context, Manager.class, 1);
            assertTrue(o1 instanceof CAY_207Manager1);

            Object p1 = o1.readProperty("clientContactType");
            assertNotNull(p1);

            // ***** The next assertion fails - uncomment to test
            //assertTrue(
            //        "Invalid property class: " + p1.getClass().getName(),
            //        p1 instanceof CAY_207String1);

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
            DataObject o2 = DataObjectUtils.objectForPK(context, Manager.class, 2);
            assertTrue(o2 instanceof CAY_207Manager2);

            Object p2 = o2.readProperty("clientContactType");
            assertNotNull(p2);

            // ***** The next assertion fails - uncomment to test
            // assertTrue(
            //        "Invalid property class: " + p2.getClass().getName(),
            //        p2 instanceof CAY_207String2);

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
            DataObject o1 = DataObjectUtils
                    .objectForPK(context, CAY_207Manager1.class, 1);
            assertTrue(o1 instanceof CAY_207Manager1);

            Object p1 = o1.readProperty("clientContactType");
            assertNotNull(p1);

            assertTrue(
                    "Invalid property class: " + p1.getClass().getName(),
                    p1 instanceof CAY_207String1);
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
            DataObject o2 = DataObjectUtils
                    .objectForPK(context, CAY_207Manager2.class, 2);
            assertTrue(o2 instanceof CAY_207Manager2);

            Object p2 = o2.readProperty("clientContactType");
            assertNotNull(p2);

            assertTrue(
                    "Invalid property class: " + p2.getClass().getName(),
                    p2 instanceof CAY_207String2);
        }
        finally {
            cleanup(context);
        }
    }

    public void testCAY_207Save() throws Exception {
        DataContext context = createDataContext();

        prepare();

        try {
            CAY_207Manager2 o2 = (CAY_207Manager2) context
                    .createAndRegisterNewObject(CAY_207Manager2.class);
            o2.setPersonType("M2");
            o2.setName("aaaa");
            o2.setClientContactType(new CAY_207String1("AAAAA"));

            // should succeed...
            context.commitChanges();

            int pk = DataObjectUtils.intPKForObject(o2);
            String query = "SELECT #result('CLIENT_CONTACT_TYPE' 'String' 'CLIENT_CONTACT_TYPE') "
                    + "FROM PERSON WHERE PERSON_ID = "
                    + pk;
            SQLTemplate template = new SQLTemplate(CAY_207Manager2.class, query, true);
            template.setFetchingDataRows(true);
            List rows = context.performQuery(template);
            assertEquals(1, rows.size());

            Map map = (Map) rows.get(0);
            assertEquals("AAAAA", map.get("CLIENT_CONTACT_TYPE"));
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
            ma1.setParent(m1);
            m1.addAttribute(ma1);

            ObjEntity m2 = new ObjEntity("Manager2");
            m2.setSuperEntityName(manager.getName());
            m2.setDeclaredQualifier(Expression.fromString("personType = \"M2\""));
            m2.setClassName(CAY_207Manager2.class.getName());
            ObjAttribute ma2 = new ObjAttribute("clientContactType");
            ma2.setDbAttributePath("CLIENT_CONTACT_TYPE");
            ma2.setType(CAY_207String2.class.getName());
            ma2.setParent(m2);
            m2.addAttribute(ma2);

            testMap = new DataMap("CAY-207");
            testMap.addObjEntity(m1);
            testMap.addObjEntity(m2);
        }
    }
}