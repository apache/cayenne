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
package org.apache.cayenne.exp;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cayenne.reflect.TstJavaBean;
import org.apache.cayenne.reflect.UnresolvablePathException;
import org.apache.cayenne.util.Util;

public class PropertyTest extends TestCase {

    public void testIn() {
        Property<String> p = new Property<String>("x.y");

        Expression e1 = p.in("a");
        assertEquals("x.y in (\"a\")", e1.toString());

        Expression e2 = p.in("a", "b");
        assertEquals("x.y in (\"a\", \"b\")", e2.toString());

        Expression e3 = p.in(Arrays.asList("a", "b"));
        assertEquals("x.y in (\"a\", \"b\")", e3.toString());
    }
    
    public void testGetFrom() {
    	TstJavaBean bean = new TstJavaBean();
    	bean.setIntField(7);
    	final Property<Integer> INT_FIELD = new Property<Integer>("intField");
    	assertEquals(Integer.valueOf(7), INT_FIELD.getFrom(bean));
    }
    
    public void testGetFromNestedProperty() {
    	TstJavaBean bean = new TstJavaBean();
    	TstJavaBean nestedBean = new TstJavaBean();
    	nestedBean.setIntField(7);
    	bean.setObjectField(nestedBean);
    	final Property<Integer> OBJECT_FIELD_INT_FIELD = new Property<Integer>("objectField.intField");
    	assertEquals(Integer.valueOf(7), OBJECT_FIELD_INT_FIELD.getFrom(bean));
    }
    
    public void testGetFromNestedNull() {
    	TstJavaBean bean = new TstJavaBean();
    	bean.setObjectField(null);
    	final Property<Integer> OBJECT_FIELD_INT_FIELD = new Property<Integer>("objectField.intField");
    	try {
    		OBJECT_FIELD_INT_FIELD.getFrom(bean);
    		fail();
    	} catch (Exception e) {
    		Throwable rootException = Util.unwindException(e);
    		if (!(rootException instanceof UnresolvablePathException)) {
    			fail();
    		}
    	}
    }
    
    public void testGetFromAll() {
    	TstJavaBean bean = new TstJavaBean();
    	bean.setIntField(7);
    	
    	TstJavaBean bean2 = new TstJavaBean();
    	bean2.setIntField(8);
    	
    	List<TstJavaBean> beans = Arrays.asList(bean, bean2);

    	final Property<Integer> INT_FIELD = new Property<Integer>("intField");
    	assertEquals(Arrays.asList(7, 8), INT_FIELD.getFromAll(beans));
    }
    
    public void testSetIn() {
    	TstJavaBean bean = new TstJavaBean();
    	final Property<Integer> INT_FIELD = new Property<Integer>("intField");
    	INT_FIELD.setIn(bean, 7);
    	assertEquals(7, bean.getIntField());
    }
    
    public void testSetInNestedProperty() {
    	TstJavaBean bean = new TstJavaBean();
    	bean.setObjectField(new TstJavaBean());
    	
    	final Property<Integer> OBJECT_FIELD_INT_FIELD = new Property<Integer>("objectField.intField");

    	OBJECT_FIELD_INT_FIELD.setIn(bean, 7);
    	assertEquals(7, ((TstJavaBean)bean.getObjectField()).getIntField());
    }
    
    public void testSetInNestedNull() {
    	TstJavaBean bean = new TstJavaBean();
    	bean.setObjectField(null);
    	final Property<Integer> OBJECT_FIELD_INT_FIELD = new Property<Integer>("objectField.intField");
    	try {
    		OBJECT_FIELD_INT_FIELD.setIn(bean, 7);
    		fail();
    	} catch (Exception e) {
    		Throwable rootException = Util.unwindException(e);
    		if (!(rootException instanceof UnresolvablePathException)) {
    			fail();
    		}
    	}
    }
    
    public void testSetInAll() {
    	TstJavaBean bean = new TstJavaBean();
    	TstJavaBean bean2 = new TstJavaBean();
    	List<TstJavaBean> beans = Arrays.asList(bean, bean2);

    	final Property<Integer> INT_FIELD = new Property<Integer>("intField");
    	INT_FIELD.setInAll(beans, 7);
    	assertEquals(7, bean.getIntField());
    	assertEquals(7, bean2.getIntField());
    }
    
    public void testEquals() {
    	final Property<Integer> INT_FIELD = new Property<Integer>("intField");
    	final Property<Integer> INT_FIELD2 = new Property<Integer>("intField");

    	assertTrue(INT_FIELD != INT_FIELD2);
    	assertTrue(INT_FIELD.equals(INT_FIELD2));
    }
    
    public void testHashCode() {
    	final Property<Integer> INT_FIELD  = new Property<Integer>("intField");
    	final Property<Integer> INT_FIELD2 = new Property<Integer>("intField");
    	final Property<Long> LONG_FIELD = new Property<Long>("longField");

    	assertTrue(INT_FIELD.hashCode() == INT_FIELD2.hashCode());
    	assertTrue(INT_FIELD.hashCode() != LONG_FIELD.hashCode());
    }
    
}
