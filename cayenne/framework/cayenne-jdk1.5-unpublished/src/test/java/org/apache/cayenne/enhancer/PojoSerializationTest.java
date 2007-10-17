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
package org.apache.cayenne.enhancer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.PropertyUtils;

public class PojoSerializationTest extends TestCase {

    protected ClassLoader loader;

    @Override
    protected void setUp() throws Exception {
        Collection<String> managedClasses = new ArrayList<String>();
        managedClasses.add(MockSerializablePojo1.class.getName());
        managedClasses.add(MockSerializablePojo2.class.getName());

        Map<String, Collection<String>> enhancedPropertyMap = new HashMap<String, Collection<String>>();

        Collection<String> c1 = new HashSet<String>();
        c1.add("attribute1");
        enhancedPropertyMap.put(MockSerializablePojo1.class.getName(), c1);

        ObjAttribute a1 = new ObjAttribute("attribute1");
        ObjEntity e = new ObjEntity("E1");
        e.addAttribute(a1);
        e.setClassName(MockSerializablePojo1.class.getName());

        ObjAttribute a2 = new ObjAttribute("attribute1");
        ObjEntity e2 = new ObjEntity("E2");
        e2.addAttribute(a2);
        e2.setClassName(MockSerializablePojo2.class.getName());

        DataMap map = new DataMap("x");
        map.addObjEntity(e);
        map.addObjEntity(e2);

        EnhancerVisitorFactory factory = new CayenneEnhancerVisitorFactory(
                new EntityResolver(Collections.singleton(map)));
        loader = new EnhancingClassLoader(new Enhancer(factory));
    }

    public void testEnhancedToRegular() throws Exception {

        Class enhanced = Class.forName(
                MockSerializablePojo1.class.getName(),
                true,
                loader);
        assertTrue(Persistent.class.isAssignableFrom(enhanced));
        try {
            enhanced.getDeclaredField("$cay_persistenceState");
        }
        catch (NoSuchFieldException e) {
            fail("Enhancer fields are not present");
        }

        Class unenhanced = Class.forName(CayenneEnhancerVisitorFactoryTest.C1);
        assertFalse(Persistent.class.isAssignableFrom(unenhanced));
        try {
            unenhanced.getDeclaredField("$cay_persistenceState");
            fail("Enhancer fields are present");
        }
        catch (NoSuchFieldException e) {
            // expected
        }

        Object eo = enhanced.newInstance();
        PropertyUtils.setProperty(eo, "attribute1", "XXX");
        PropertyUtils.setProperty(eo, "persistenceState", new Integer(
                PersistenceState.MODIFIED));
        assertTrue(eo instanceof Persistent);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(out);
        oout.writeObject(eo);
        oout.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream oin = new ObjectInputStream(in);

        Object ueo = oin.readObject();
        assertNotNull(ueo);
        assertFalse(ueo instanceof Persistent);
        assertEquals("XXX", PropertyUtils.getProperty(ueo, "attribute1"));

    }

    public void testEnhancedToRegularNoSerialVersionId() throws Exception {

        Class enhanced = Class.forName(
                MockSerializablePojo2.class.getName(),
                true,
                loader);
        assertTrue(Persistent.class.isAssignableFrom(enhanced));
        try {
            enhanced.getDeclaredField("$cay_persistenceState");
        }
        catch (NoSuchFieldException e) {
            fail("Enhancer fields are not present");
        }

        Class unenhanced = Class.forName(CayenneEnhancerVisitorFactoryTest.C1);
        assertFalse(Persistent.class.isAssignableFrom(unenhanced));
        try {
            unenhanced.getDeclaredField("$cay_persistenceState");
            fail("Enhancer fields are present");
        }
        catch (NoSuchFieldException e) {
            // expected
        }

        Object eo = enhanced.newInstance();
        PropertyUtils.setProperty(eo, "attribute1", "XXX");
        PropertyUtils.setProperty(eo, "persistenceState", new Integer(
                PersistenceState.MODIFIED));
        assertTrue(eo instanceof Persistent);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(out);
        oout.writeObject(eo);
        oout.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream oin = new ObjectInputStream(in);

        Object ueo = oin.readObject();
        assertNotNull(ueo);
        assertFalse(ueo instanceof Persistent);
        assertEquals("XXX", PropertyUtils.getProperty(ueo, "attribute1"));

    }
}
