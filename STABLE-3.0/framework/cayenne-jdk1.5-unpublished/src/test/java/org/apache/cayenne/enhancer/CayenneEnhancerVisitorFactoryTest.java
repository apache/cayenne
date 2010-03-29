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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.MockObjectContext;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

public class CayenneEnhancerVisitorFactoryTest extends TestCase {

    public static final String C1 = "org.apache.cayenne.enhancer.MockPojo1";

    protected ClassLoader loader;

    @Override
    protected void setUp() throws Exception {
        Collection<String> managedClasses = new ArrayList<String>();
        managedClasses.add(C1);

        Map<String, Collection<String>> enhancedPropertyMap = new HashMap<String, Collection<String>>();

        Collection<String> c1 = new HashSet<String>();
        c1.add("attribute1");
        enhancedPropertyMap.put(C1, c1);

        ObjAttribute a1 = new ObjAttribute("attribute1");
        ObjAttribute a2 = new ObjAttribute("attribute2");
        ObjAttribute a3 = new ObjAttribute("attribute3");
        ObjEntity e = new ObjEntity("E1");
        e.addAttribute(a1);
        e.addAttribute(a2);
        e.addAttribute(a3);
        e.setClassName(C1);
        DataMap map = new DataMap("x");
        map.addObjEntity(e);

        EnhancerVisitorFactory factory = new CayenneEnhancerVisitorFactory(
                new EntityResolver(Collections.singleton(map)));
        loader = new EnhancingClassLoader(new Enhancer(factory));
    }

    public void testPersistentInterfaceInjected() throws Exception {

        Class e1Class = Class.forName(C1, true, loader);
        assertNotNull(e1Class);
        assertEquals(C1, e1Class.getName());
        assertTrue(Persistent.class.isAssignableFrom(e1Class));
    }

    public void testPersistenceFieldsInjected() throws Exception {

        Class e1Class = Class.forName(C1, true, loader);
        assertNotNull(e1Class);

        Field objectContext = e1Class.getDeclaredField("$cay_objectContext");
        assertTrue(ObjectContext.class.isAssignableFrom(objectContext.getType()));
        assertTrue(!Modifier.isStatic(objectContext.getModifiers()));
        assertTrue(Modifier.isProtected(objectContext.getModifiers()));
        assertTrue(!Modifier.isFinal(objectContext.getModifiers()));
        assertTrue(Modifier.isTransient(objectContext.getModifiers()));

        Field persistenceState = e1Class.getDeclaredField("$cay_persistenceState");
        assertTrue(Integer.TYPE.isAssignableFrom(persistenceState.getType()));
        assertTrue(!Modifier.isStatic(persistenceState.getModifiers()));
        assertTrue(Modifier.isProtected(persistenceState.getModifiers()));
        assertTrue(!Modifier.isFinal(persistenceState.getModifiers()));
        assertTrue(!Modifier.isTransient(persistenceState.getModifiers()));

        Field objectId = e1Class.getDeclaredField("$cay_objectId");
        assertTrue(ObjectId.class.isAssignableFrom(objectId.getType()));
        assertTrue(!Modifier.isStatic(objectId.getModifiers()));
        assertTrue(Modifier.isProtected(objectId.getModifiers()));
        assertTrue(!Modifier.isFinal(objectId.getModifiers()));
        assertTrue(!Modifier.isTransient(objectId.getModifiers()));
    }

    public void testObjectContentInjectedProperty() throws Exception {
        Class e1Class = Class.forName(C1, true, loader);
        assertNotNull(e1Class);

        Method getObjectContext = e1Class.getDeclaredMethod(
                "getObjectContext",
                (Class[]) null);
        Method setObjectContext = e1Class.getDeclaredMethod(
                "setObjectContext",
                ObjectContext.class);

        Object o = e1Class.newInstance();
        assertNull(getObjectContext.invoke(o, (Object[]) null));
        ObjectContext oc = new MockObjectContext();
        setObjectContext.invoke(o, oc);
        assertSame(oc, getObjectContext.invoke(o, (Object[]) null));
    }

    public void testPersistenceStateInjectedProperty() throws Exception {
        Class e1Class = Class.forName(C1, true, loader);
        assertNotNull(e1Class);
        assertEquals(C1, e1Class.getName());

        Method getPersistenceState = e1Class.getDeclaredMethod(
                "getPersistenceState",
                (Class[]) null);
        Method setPersistenceState = e1Class.getDeclaredMethod(
                "setPersistenceState",
                Integer.TYPE);

        Object o = e1Class.newInstance();
        assertEquals(0, getPersistenceState.invoke(o, (Object[]) null));

        setPersistenceState.invoke(o, new Integer(PersistenceState.DELETED));

        Object state = getPersistenceState.invoke(o, (Object[]) null);
        assertEquals(PersistenceState.DELETED, state);
    }

    public void testStringGetterIntercepted() throws Exception {

        Class e1Class = Class.forName(C1, true, loader);
        assertNotNull(e1Class);
        assertEquals(C1, e1Class.getName());

        Object o = e1Class.newInstance();

        // attempt calling on detached object - must not fail
        Method getAttribute1 = e1Class.getDeclaredMethod("getAttribute1", (Class[]) null);
        assertEquals(null, getAttribute1.invoke(o, (Object[]) null));

        // now call on attached object

        final Object[] prepared = new Object[3];
        ObjectContext context = new MockObjectContext() {

            @Override
            public void prepareForAccess(Persistent object, String property, boolean lazyFaulting) {
                prepared[0] = object;
                prepared[1] = property;
                prepared[2] = (lazyFaulting) ? Boolean.TRUE : Boolean.FALSE;
            }
        };

        Method setObjectContext = e1Class.getDeclaredMethod(
                "setObjectContext",
                ObjectContext.class);

        setObjectContext.invoke(o, context);

        assertEquals(null, getAttribute1.invoke(o, (Object[]) null));
        assertSame(o, prepared[0]);
        assertEquals("attribute1", prepared[1]);
        assertSame(Boolean.FALSE, prepared[2]);
    }

    public void testStringSetterIntercepted() throws Exception {

        Class e1Class = Class.forName(C1, true, loader);
        assertNotNull(e1Class);
        assertEquals(C1, e1Class.getName());

        Object o = e1Class.newInstance();

        // attempt calling on detached object - must not fail
        Method getAttribute1 = e1Class.getDeclaredMethod("getAttribute1", (Class[]) null);
        Method setAttribute1 = e1Class.getDeclaredMethod("setAttribute1", String.class);

        assertEquals(null, getAttribute1.invoke(o, (Object[]) null));
        setAttribute1.invoke(o, "x");
        assertEquals("x", getAttribute1.invoke(o, (Object[]) null));

        // now call on attached object
        final Object[] change = new Object[4];
        ObjectContext context = new MockObjectContext() {

            @Override
            public void propertyChanged(
                    Persistent object,
                    String property,
                    Object oldValue,
                    Object newValue) {
                change[0] = object;
                change[1] = property;
                change[2] = oldValue;
                change[3] = newValue;
            }
        };

        Method setObjectContext = e1Class.getDeclaredMethod(
                "setObjectContext",
                ObjectContext.class);

        setObjectContext.invoke(o, context);

        setAttribute1.invoke(o, "y");
        assertEquals("y", getAttribute1.invoke(o, (Object[]) null));
        assertSame(o, change[0]);
        assertEquals("attribute1", change[1]);
        assertEquals("x", change[2]);
        assertEquals("y", change[3]);
    }

    public void testIntGetterIntercepted() throws Exception {

        Class e1Class = Class.forName(C1, true, loader);
        Object o = e1Class.newInstance();

        // attempt calling on detached object - must not fail
        Method getAttribute2 = e1Class.getDeclaredMethod("getAttribute2", (Class[]) null);
        assertEquals(new Integer(0), getAttribute2.invoke(o, (Object[]) null));

        // now call on attached object

        final Object[] prepared = new Object[3];
        ObjectContext context = new MockObjectContext() {

            @Override
            public void prepareForAccess(Persistent object, String property, boolean lazyFaulting) {
                prepared[0] = object;
                prepared[1] = property;
                prepared[2] = (lazyFaulting) ? Boolean.TRUE : Boolean.FALSE;
            }
        };

        Method setObjectContext = e1Class.getDeclaredMethod(
                "setObjectContext",
                ObjectContext.class);

        setObjectContext.invoke(o, context);

        assertEquals(new Integer(0), getAttribute2.invoke(o, (Object[]) null));
        assertSame(o, prepared[0]);
        assertEquals("attribute2", prepared[1]);
        assertSame(Boolean.FALSE, prepared[2]);
    }

    public void testIntSetterIntercepted() throws Exception {

        Class e1Class = Class.forName(C1, true, loader);

        Object o = e1Class.newInstance();

        // attempt calling on detached object - must not fail
        Method getAttribute2 = e1Class.getDeclaredMethod("getAttribute2", (Class[]) null);
        Method setAttribute2 = e1Class.getDeclaredMethod("setAttribute2", Integer.TYPE);

        assertEquals(new Integer(0), getAttribute2.invoke(o, (Object[]) null));
        setAttribute2.invoke(o, new Integer(3));
        assertEquals(new Integer(3), getAttribute2.invoke(o, (Object[]) null));

        // now call on attached object
        final Object[] change = new Object[4];
        ObjectContext context = new MockObjectContext() {

            @Override
            public void propertyChanged(
                    Persistent object,
                    String property,
                    Object oldValue,
                    Object newValue) {
                change[0] = object;
                change[1] = property;
                change[2] = oldValue;
                change[3] = newValue;
            }
        };

        Method setObjectContext = e1Class.getDeclaredMethod(
                "setObjectContext",
                ObjectContext.class);

        setObjectContext.invoke(o, context);

        setAttribute2.invoke(o, new Integer(4));
        assertEquals(new Integer(4), getAttribute2.invoke(o, (Object[]) null));
        assertSame(o, change[0]);
        assertEquals("attribute2", change[1]);
        assertEquals(new Integer(3), change[2]);
        assertEquals(new Integer(4), change[3]);
    }
    
    public void testDoubleGetterIntercepted() throws Exception {

        Class e1Class = Class.forName(C1, true, loader);
        Object o = e1Class.newInstance();

        // attempt calling on detached object - must not fail
        Method getAttribute3 = e1Class.getDeclaredMethod("getAttribute3", (Class[]) null);
        assertEquals(new Double(0d), getAttribute3.invoke(o, (Object[]) null));

        // now call on attached object

        final Object[] prepared = new Object[3];
        ObjectContext context = new MockObjectContext() {

            @Override
            public void prepareForAccess(Persistent object, String property, boolean lazyFaulting) {
                prepared[0] = object;
                prepared[1] = property;
                prepared[2] = (lazyFaulting) ? Boolean.TRUE : Boolean.FALSE;
            }
        };

        Method setObjectContext = e1Class.getDeclaredMethod(
                "setObjectContext",
                ObjectContext.class);

        setObjectContext.invoke(o, context);

        assertEquals(new Double(0d), getAttribute3.invoke(o, (Object[]) null));
        assertSame(o, prepared[0]);
        assertEquals("attribute3", prepared[1]);
        assertSame(Boolean.FALSE, prepared[2]);
    }

    public void testDoubleSetterIntercepted() throws Exception {

        Class e1Class = Class.forName(C1, true, loader);

        Object o = e1Class.newInstance();

        // attempt calling on detached object - must not fail
        Method getAttribute3 = e1Class.getDeclaredMethod("getAttribute3", (Class[]) null);
        Method setAttribute3 = e1Class.getDeclaredMethod("setAttribute3", Double.TYPE);

        assertEquals(new Double(0d), getAttribute3.invoke(o, (Object[]) null));
        setAttribute3.invoke(o, new Double(3.1d));
        assertEquals(new Double(3.1d), getAttribute3.invoke(o, (Object[]) null));

        // now call on attached object
        final Object[] change = new Object[4];
        ObjectContext context = new MockObjectContext() {

            @Override
            public void propertyChanged(
                    Persistent object,
                    String property,
                    Object oldValue,
                    Object newValue) {
                change[0] = object;
                change[1] = property;
                change[2] = oldValue;
                change[3] = newValue;
            }
        };

        Method setObjectContext = e1Class.getDeclaredMethod(
                "setObjectContext",
                ObjectContext.class);

        setObjectContext.invoke(o, context);

        setAttribute3.invoke(o, new Double(5.3));
        assertEquals(new Double(5.3), getAttribute3.invoke(o, (Object[]) null));
        assertSame(o, change[0]);
        assertEquals("attribute3", change[1]);
        assertEquals(new Double(3.1), change[2]);
        assertEquals(new Double(5.3), change[3]);
    }
}
